package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Checking the formats of every album of an artist from the artist's releases, a page at a time. */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelBulkFormatsTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var api: MusicBrainzApi

    private val now = 1_800_000_000_000L

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(MusicBrainzApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun viewModel(
        throttle: MusicBrainzThrottle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 0),
        pageSize: Int = 2,
        maxPages: Int = 60
    ) = DiscographyViewModel(
        api, database.albumDao(), database.artistDao(),
        pageSize = pageSize, throttle = throttle, clock = { now }, formatCheckMaxPages = maxPages
    )

    private suspend fun album(
        id: String,
        formats: String? = null,
        checkedAt: Long? = null,
        category: String = "STUDIO_ALBUM",
        artistId: String = "artist"
    ) = database.albumDao().insert(
        AlbumEntity(id, artistId, "Album $id", category, null, owned = false, wanted = false, formats = formats, formatsCheckedAt = checkedAt)
    )

    private fun release(group: String, vararg formats: String?) =
        """{ "id": "r-$group-${formats.contentHashCode()}", "release-group": { "id": "$group" },
              "media": [ ${formats.joinToString(",") { if (it == null) "{}" else """{ "format": "${it.replace("\"", "\\\"")}" }""" }} ] }"""

    private fun page(total: Int, vararg releases: String) =
        """{ "release-count": $total, "releases": [ ${releases.joinToString(",")} ] }"""

    private fun enqueue(body: String) = server.enqueue(MockResponse().setBody(body))

    private fun stored(id: String) = runBlocking { database.albumDao().getById(id)!! }

    private fun paths(count: Int) = (1..count).map { server.takeRequest(2, TimeUnit.SECONDS)?.path }

    @Test
    fun `reads every page of the artist's releases, to the total`() = runBlocking {
        album("a")
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        enqueue(page(5, release("a", "CD")))

        viewModel().checkAllFormats("artist")

        assertEquals(
            listOf(
                "/release/?artist=artist&inc=release-groups+media&limit=2&offset=0&fmt=json",
                "/release/?artist=artist&inc=release-groups+media&limit=2&offset=2&fmt=json",
                "/release/?artist=artist&inc=release-groups+media&limit=2&offset=4&fmt=json"
            ),
            paths(3)
        )
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `saves the formats of each album from the releases that belong to it`() = runBlocking {
        album("a"); album("b")
        enqueue(page(3, release("a", "12\" Vinyl"), release("b", "CD")))
        enqueue(page(3, release("a", "Cassette")))

        val result = viewModel().checkAllFormats("artist")

        assertEquals(FormatCheckResult.Done(2), result)
        assertEquals(listOf("12\" Vinyl", "Cassette"), ReleaseFormats.decode(stored("a").formats!!).sorted())
        assertEquals(listOf("CD"), ReleaseFormats.decode(stored("b").formats!!))
        assertEquals(now, stored("a").formatsCheckedAt)
        assertEquals(now, stored("b").formatsCheckedAt)
    }

    @Test
    fun `an album whose releases come on different pages gets the formats of all of them`() = runBlocking {
        album("a")
        enqueue(page(4, release("a", "CD"), release("x", "CD")))
        enqueue(page(4, release("a", "12\" Vinyl"), release("a", "CD")))

        viewModel().checkAllFormats("artist")

        assertEquals(listOf("12\" Vinyl", "CD").sorted(), ReleaseFormats.decode(stored("a").formats!!).sorted())
    }

    @Test
    fun `each format is stored once, however many releases have it`() = runBlocking {
        album("a")
        enqueue(page(3, release("a", "CD"), release("a", "CD"), release("a", "CD")))
        enqueue(page(3, release("a", "CD")))

        viewModel().checkAllFormats("artist")

        assertEquals(listOf("CD"), ReleaseFormats.decode(stored("a").formats!!))
    }

    @Test
    fun `an album seen only on releases with no media is saved as checked with no formats`() = runBlocking {
        album("a")
        enqueue(page(1, """{ "id": "r", "release-group": { "id": "a" } }"""))

        viewModel().checkAllFormats("artist")

        assertEquals("", stored("a").formats)
        assertEquals(now, stored("a").formatsCheckedAt)
    }

    @Test
    fun `an album that none of the releases belong to is saved as checked with nothing found, so the check is not asked for again`() = runBlocking {
        album("a"); album("unseen")
        enqueue(page(1, release("a", "CD")))

        viewModel().checkAllFormats("artist")

        assertEquals("", stored("unseen").formats)
        assertEquals("", stored("unseen").statuses)
        assertEquals(now, stored("unseen").formatsCheckedAt)
        val saved = viewModel().loadSaved("artist").groupedAlbums.values.flatten().single { it.id == "unseen" }
        assertEquals(emptyList<String>(), saved.formats)
        assertEquals(emptyList<String>(), saved.statuses)
        assertEquals(false, saved.formatsStale)
    }

    @Test
    fun `an album the releases do not cover keeps the formats and statuses an earlier lookup found, and only gets a new date`() = runBlocking {
        album("a")
        database.albumDao().insert(
            AlbumEntity(
                "old", "artist", "Old", "STUDIO_ALBUM", null, owned = false, wanted = false,
                formats = "CD", formatsCheckedAt = 1L, statuses = "Official"
            )
        )
        enqueue(page(1, release("a", "CD")))

        viewModel().checkAllFormats("artist")

        assertEquals("CD", stored("old").formats)
        assertEquals("Official", stored("old").statuses)
        assertEquals(now, stored("old").formatsCheckedAt)
    }

    @Test
    fun `an album the releases do not cover keeps what a lookup found for formats, and gets no statuses made up`() = runBlocking {
        album("a")
        album("looked-up", formats = "CD", checkedAt = now)
        enqueue(page(1, release("a", "CD")))

        viewModel().checkAllFormats("artist")

        assertEquals("CD", stored("looked-up").formats)
        assertEquals("", stored("looked-up").statuses)
    }

    @Test
    fun `releases of groups that are not saved albums of the artist make no rows`() = runBlocking {
        album("a")
        enqueue(page(2, release("a", "CD"), release("someone-else", "CD")))

        viewModel().checkAllFormats("artist")

        assertNull(database.albumDao().getById("someone-else"))
    }

    @Test
    fun `an album of another artist is not changed even when it appears in the releases`() = runBlocking {
        album("mine"); album("theirs", artistId = "other-artist")
        enqueue(page(2, release("mine", "CD"), release("theirs", "CD")))

        viewModel().checkAllFormats("artist")

        assertNull(stored("theirs").formats)
    }

    @Test
    fun `replaces formats that were stored earlier, including old ones`() = runBlocking {
        album("a", formats = "CD", checkedAt = now - 40L * 24 * 60 * 60 * 1000)
        enqueue(page(1, release("a", "12\" Vinyl")))

        viewModel().checkAllFormats("artist")

        assertEquals(listOf("12\" Vinyl"), ReleaseFormats.decode(stored("a").formats!!))
        assertEquals(now, stored("a").formatsCheckedAt)
    }

    @Test
    fun `reports each page as it arrives, with the number of pages`() = runBlocking {
        album("a")
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        enqueue(page(5, release("a", "CD")))
        val seen = mutableListOf<Pair<Int, Int>>()

        viewModel().checkAllFormats("artist") { done, total -> seen.add(done to total) }

        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), seen)
    }

    @Test
    fun `an artist with no releases is done with nothing to save`() = runBlocking {
        album("a")
        enqueue(page(0))

        assertEquals(FormatCheckResult.Done(0), viewModel().checkAllFormats("artist"))
        assertNull(stored("a").formats)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `an artist with too many releases is not checked this way, and nothing is saved`() = runBlocking {
        album("a")
        // Two pages of 2 are the most allowed, and this artist has 5 releases.
        enqueue(page(5, release("a", "CD"), release("a", "CD")))

        assertEquals(FormatCheckResult.TooBig, viewModel(maxPages = 2).checkAllFormats("artist"))

        assertEquals(1, server.requestCount)
        assertNull(stored("a").formats)
    }

    @Test
    fun `an artist found too big is not asked about again in the same run`() = runBlocking {
        album("a")
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        val viewModel = viewModel(maxPages = 2)

        viewModel.checkAllFormats("artist")
        assertEquals(FormatCheckResult.TooBig, viewModel.checkAllFormats("artist"))

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a failure on the first page gives Failed and saves nothing`() = runBlocking {
        album("a")
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(FormatCheckResult.Failed, viewModel().checkAllFormats("artist"))
        assertNull(stored("a").formats)
    }

    @Test
    fun `a failure on a later page gives Failed and saves nothing`() = runBlocking {
        album("a")
        enqueue(page(4, release("a", "CD"), release("a", "CD")))
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(FormatCheckResult.Failed, viewModel().checkAllFormats("artist"))
        assertNull(stored("a").formats)
    }

    @Test
    fun `trying again after a failure carries on from the page that failed`() = runBlocking {
        album("a")
        enqueue(page(6, release("a", "CD"), release("a", "CD")))
        server.enqueue(MockResponse().setResponseCode(500))
        val viewModel = viewModel()
        viewModel.checkAllFormats("artist")
        server.takeRequest(2, TimeUnit.SECONDS); server.takeRequest(2, TimeUnit.SECONDS)

        enqueue(page(6, release("a", "12\" Vinyl"), release("a", "CD")))
        enqueue(page(6, release("a", "Cassette")))
        val result = viewModel.checkAllFormats("artist")

        assertEquals(FormatCheckResult.Done(1), result)
        // It asked for offsets 2 and 4 only. The first page was kept.
        assertEquals(
            listOf(
                "/release/?artist=artist&inc=release-groups+media&limit=2&offset=2&fmt=json",
                "/release/?artist=artist&inc=release-groups+media&limit=2&offset=4&fmt=json"
            ),
            paths(2)
        )
        assertEquals(listOf("12\" Vinyl", "CD", "Cassette").sorted(), ReleaseFormats.decode(stored("a").formats!!).sorted())
    }

    @Test
    fun `a run that has finished starts again from the first page next time`() = runBlocking {
        album("a")
        enqueue(page(1, release("a", "CD")))
        enqueue(page(1, release("a", "Cassette")))
        val viewModel = viewModel()

        viewModel.checkAllFormats("artist")
        viewModel.checkAllFormats("artist")

        assertEquals(2, server.requestCount)
        assertEquals(listOf("Cassette"), ReleaseFormats.decode(stored("a").formats!!))
    }

    @Test
    fun `each artist has its own progress`() = runBlocking {
        album("a"); album("b", artistId = "other")
        enqueue(page(4, release("a", "CD"), release("a", "CD")))
        server.enqueue(MockResponse().setResponseCode(500))
        val viewModel = viewModel()
        viewModel.checkAllFormats("artist")

        enqueue(page(1, release("b", "Cassette")))
        assertEquals(FormatCheckResult.Done(1), viewModel.checkAllFormats("other"))
    }

    @Test
    fun `every page waits for its slot in the queue`() = runBlocking {
        album("a")
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        enqueue(page(5, release("a", "CD"), release("a", "CD")))
        enqueue(page(5, release("a", "CD")))

        val startedAt = System.nanoTime()
        viewModel(MusicBrainzThrottle(minIntervalMillis = 300)).checkAllFormats("artist")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 580)
    }

    @Test
    fun `the labels of an album are right once its formats are saved, by its category`() = runBlocking {
        album("lp", category = "STUDIO_ALBUM"); album("single", category = "SINGLE")
        enqueue(page(2, release("lp", "12\" Vinyl"), release("single", "12\" Vinyl")))

        viewModel().checkAllFormats("artist")

        val saved = (viewModel().loadSaved("artist") as DiscographyUiState.Success).groupedAlbums.values.flatten()
        assertEquals(listOf("LP"), saved.first { it.id == "lp" }.formats)
        assertEquals(listOf("12\""), saved.first { it.id == "single" }.formats)
    }

    @Test
    fun `a busy answer is asked for again, and the check carries on`() = runBlocking {
        album("a")
        server.enqueue(MockResponse().setResponseCode(503))
        enqueue(page(1, release("a", "CD")))

        val result = viewModel().checkAllFormats("artist")

        assertEquals(FormatCheckResult.Done(1), result)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a busy answer in the middle of the pages is asked for again too`() = runBlocking {
        album("a")
        enqueue(page(4, release("a", "CD"), release("a", "CD")))
        server.enqueue(MockResponse().setResponseCode(429))
        enqueue(page(4, release("a", "Cassette")))

        val result = viewModel().checkAllFormats("artist")

        assertEquals(FormatCheckResult.Done(1), result)
        assertEquals(3, server.requestCount)
        assertEquals(listOf("CD", "Cassette").sorted(), ReleaseFormats.decode(stored("a").formats!!).sorted())
    }

    @Test
    fun `a page that stays busy gives Failed after three tries, and keeps what was read`() = runBlocking {
        album("a")
        enqueue(page(4, release("a", "CD"), release("a", "CD")))
        repeat(3) { server.enqueue(MockResponse().setResponseCode(503)) }
        val viewModel = viewModel()

        assertEquals(FormatCheckResult.Failed, viewModel.checkAllFormats("artist"))
        assertEquals(4, server.requestCount)
        assertNull(stored("a").formats)

        enqueue(page(4, release("a", "Cassette")))
        assertEquals(FormatCheckResult.Done(1), viewModel.checkAllFormats("artist"))
        // The next try asked only for the page that was busy.
        assertEquals(5, server.requestCount)
    }

    @Test
    fun `an answer that is not a busy one is not asked for again`() = runBlocking {
        album("a")
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(FormatCheckResult.Failed, viewModel().checkAllFormats("artist"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `waits out the pause after a busy answer before asking again`() = runBlocking {
        album("a")
        server.enqueue(MockResponse().setResponseCode(503))
        enqueue(page(1, release("a", "CD")))

        val startedAt = System.nanoTime()
        viewModel(MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 400)).checkAllFormats("artist")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 380)
    }
}
