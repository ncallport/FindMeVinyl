package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nickallport.findmevinyl.storage.ArtistEntity
import org.junit.Assert.assertNull
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import okhttp3.mockwebserver.SocketPolicy

@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel
    private lateinit var api: MusicBrainzApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)

        viewModel = DiscographyViewModel(api, database.albumDao(), database.artistDao())
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun `a newly fetched album defaults to not owned and not wanted`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "release-groups": [ { "id": "1", "title": "Foxtrot", "primary-type": "Album", "secondary-types": [], "first-release-date": "1972-10-06" } ] }"""
            )
        )

        val state = viewModel.loadDiscography(artistId = "some-id")

        val album = (state as DiscographyUiState.Success).groupedAlbums[DiscographyCategory.STUDIO_ALBUM]?.get(0)
        assertEquals(false, album?.owned)
        assertEquals(false, album?.wanted)
    }

    @Test
    fun `an album already marked owned locally keeps that status when refetched`() = runBlocking {
        database.albumDao().insert(
            AlbumEntity(
                id = "1", artistId = "some-id", title = "Foxtrot",
                category = "STUDIO_ALBUM", firstReleaseDate = "1972-10-06",
                owned = true, wanted = false
            )
        )

        server.enqueue(
            MockResponse().setBody(
                """{ "release-groups": [ { "id": "1", "title": "Foxtrot", "primary-type": "Album", "secondary-types": [], "first-release-date": "1972-10-06" } ] }"""
            )
        )

        val state = viewModel.loadDiscography(artistId = "some-id")

        val album = (state as DiscographyUiState.Success).groupedAlbums[DiscographyCategory.STUDIO_ALBUM]?.get(0)
        assertTrue(album?.owned == true)
    }

    private fun page(total: Int?, vararg ids: String): MockResponse {
        val groups = ids.joinToString(",") {
            """{ "id": "$it", "title": "Album $it", "primary-type": "Album", "secondary-types": [], "first-release-date": "1972-10-06" }"""
        }
        val count = if (total != null) "\"release-group-count\": $total," else ""
        return MockResponse().setBody("{ $count \"release-groups\": [ $groups ] }")
    }

    private fun paged(pageSize: Int = 2, pageDelayMillis: Long = 0, maxPages: Int = 10) =
        DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            pageSize = pageSize, pageDelayMillis = pageDelayMillis, maxPages = maxPages
        )

    private fun idsIn(state: DiscographyUiState) =
        (state as DiscographyUiState.Success).groupedAlbums.values.flatten().map { it.id }.sorted()

    @Test
    fun `loads every page of release groups`() = runBlocking {
        server.enqueue(page(5, "a", "b"))
        server.enqueue(page(5, "c", "d"))
        server.enqueue(page(5, "e"))

        val state = paged().loadDiscography("some-id")

        assertEquals(listOf("a", "b", "c", "d", "e"), idsIn(state))
    }

    @Test
    fun `asks for each page with the page size and the next offset`() = runBlocking {
        server.enqueue(page(5, "a", "b"))
        server.enqueue(page(5, "c", "d"))
        server.enqueue(page(5, "e"))

        paged().loadDiscography("some-id")

        assertEquals(
            listOf(
                "/release-group/?artist=some-id&fmt=json&limit=2&offset=0",
                "/release-group/?artist=some-id&fmt=json&limit=2&offset=2",
                "/release-group/?artist=some-id&fmt=json&limit=2&offset=4"
            ),
            (1..3).map { server.takeRequest(2, TimeUnit.SECONDS)?.path }
        )
    }

    @Test
    fun `stops after the page that completes the total`() = runBlocking {
        server.enqueue(page(3, "a", "b"))
        server.enqueue(page(3, "c"))

        paged().loadDiscography("some-id")

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `treats a response without a total as the only page`() = runBlocking {
        server.enqueue(page(null, "a", "b"))

        val state = paged().loadDiscography("some-id")

        assertEquals(1, server.requestCount)
        assertEquals(listOf("a", "b"), idsIn(state))
    }

    @Test
    fun `stops when a page comes back empty even though the total is higher`() = runBlocking {
        server.enqueue(page(10, "a", "b"))
        server.enqueue(page(10))

        val state = paged().loadDiscography("some-id")

        assertEquals(2, server.requestCount)
        assertEquals(listOf("a", "b"), idsIn(state))
    }

    @Test
    fun `stops at the page cap`() = runBlocking {
        repeat(5) { server.enqueue(page(1000, "x$it")) }

        paged(pageSize = 1, maxPages = 3).loadDiscography("some-id")

        assertEquals(3, server.requestCount)
    }

    @Test
    fun `an error on a later page gives an error state and saves nothing`() = runBlocking {
        server.enqueue(page(5, "a", "b"))
        server.enqueue(MockResponse().setResponseCode(503))

        val state = paged().loadDiscography("some-id")

        assertTrue(state is DiscographyUiState.Error)
        assertEquals(emptyList<AlbumEntity>(), database.albumDao().getAlbumsForArtist("some-id"))
    }

    @Test
    fun `waits between pages`() = runBlocking {
        server.enqueue(page(3, "a", "b"))
        server.enqueue(page(3, "c"))

        val startedAt = System.nanoTime()
        paged(pageDelayMillis = 300).loadDiscography("some-id")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 280)
    }

    @Test
    fun `spaces requests by their start times, so a slow page needs no extra wait`() = runBlocking {
        server.enqueue(page(3, "a", "b").setHeadersDelay(700, TimeUnit.MILLISECONDS))
        server.enqueue(page(3, "c").setHeadersDelay(700, TimeUnit.MILLISECONDS))

        val startedAt = System.nanoTime()
        paged(pageDelayMillis = 600).loadDiscography("some-id")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        // Two 700 ms responses back to back. Waiting 600 ms after the first would make it 2000 ms.
        assertTrue("took $elapsedMillis ms", elapsedMillis in 1_300..1_800)
    }

    @Test
    fun `does not wait before the first page`() = runBlocking {
        server.enqueue(page(2, "a", "b"))

        val startedAt = System.nanoTime()
        paged(pageDelayMillis = 5_000).loadDiscography("some-id")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis < 3_000)
    }

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun withFormats(throttle: MusicBrainzThrottle = MusicBrainzThrottle(minIntervalMillis = 0)) =
        DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            pageDelayMillis = 0, throttle = throttle, clock = { now }
        )

    private fun releasesBody(vararg formats: String): MockResponse {
        val media = formats.joinToString(",") { "{ \"format\": \"${it.replace("\"", "\\\"")}\" }" }
        return MockResponse().setBody("{ \"releases\": [ { \"id\": \"r1\", \"media\": [ $media ] } ] }")
    }

    private fun uiAlbum(state: DiscographyUiState, id: String) =
        (state as DiscographyUiState.Success).groupedAlbums.values.flatten().first { it.id == id }

    private val singleJson =
        """{ "release-groups": [ { "id": "s1", "title": "A Single", "primary-type": "Single", "secondary-types": [], "first-release-date": "1972-01-01" } ] }"""

    @Test
    fun `shows the formats saved for an album as labels`() = runBlocking {
        database.albumDao().insert(
            album("a").copy(formats = ReleaseFormats.encode(listOf("12\" Vinyl", "CD")), formatsCheckedAt = now)
        )
        server.enqueue(page(null, "a"))

        val ui = uiAlbum(withFormats().loadDiscography("some-id"), "a")

        assertEquals(listOf("LP", "CD"), ui.formats)
        assertEquals(false, ui.formatsStale)
    }

    @Test
    fun `has no formats to show for an album that was never looked up`() = runBlocking {
        database.albumDao().insert(album("a"))
        server.enqueue(page(null, "a"))

        val ui = uiAlbum(withFormats().loadDiscography("some-id"), "a")

        assertNull(ui.formats)
        assertEquals(false, ui.formatsStale)
    }

    @Test
    fun `an album that was looked up and has no known formats shows an empty list`() = runBlocking {
        database.albumDao().insert(album("a").copy(formats = "", formatsCheckedAt = now))
        server.enqueue(page(null, "a"))

        val ui = uiAlbum(withFormats().loadDiscography("some-id"), "a")

        assertEquals(emptyList<String>(), ui.formats)
    }

    @Test
    fun `formats checked more than 30 days ago are stale but still shown`() = runBlocking {
        database.albumDao().insert(album("old").copy(formats = "CD", formatsCheckedAt = now - 31 * day))
        database.albumDao().insert(album("recent").copy(formats = "CD", formatsCheckedAt = now - 29 * day))
        server.enqueue(page(null, "old", "recent"))

        val state = withFormats().loadDiscography("some-id")

        assertEquals(true, uiAlbum(state, "old").formatsStale)
        assertEquals(listOf("CD"), uiAlbum(state, "old").formats)
        assertEquals(false, uiAlbum(state, "recent").formatsStale)
    }

    @Test
    fun `a 12 inch vinyl is a 12 inch on a single and an LP on an album`() = runBlocking {
        database.albumDao().insert(
            album("s1").copy(category = "SINGLE", formats = "12\" Vinyl", formatsCheckedAt = now)
        )
        server.enqueue(MockResponse().setBody(singleJson))

        val ui = uiAlbum(withFormats().loadDiscography("some-id"), "s1")

        assertEquals(listOf("12\""), ui.formats)
    }

    @Test
    fun `looking up formats fetches the releases, saves the formats and returns the labels`() = runBlocking {
        database.albumDao().insert(album("1"))
        server.enqueue(releasesBody("12\" Vinyl", "CD"))

        val labels = withFormats().loadFormats("1")

        assertEquals(listOf("LP", "CD"), labels)
        val saved = database.albumDao().getById("1")!!
        assertEquals("12\" Vinyl|CD", saved.formats)
        assertEquals(now, saved.formatsCheckedAt)
        assertEquals(
            "/release/?release-group=1&inc=media&limit=100&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `looking up formats uses the category of the album for the labels`() = runBlocking {
        database.albumDao().insert(album("1").copy(category = "EP"))
        server.enqueue(releasesBody("12\" Vinyl"))

        assertEquals(listOf("12\""), withFormats().loadFormats("1"))
    }

    @Test
    fun `an album with no known formats is saved as looked up so it is not asked for again`() = runBlocking {
        database.albumDao().insert(album("1"))
        server.enqueue(MockResponse().setBody("""{ "releases": [] }"""))

        val labels = withFormats().loadFormats("1")

        assertEquals(emptyList<String>(), labels)
        val saved = database.albumDao().getById("1")!!
        assertEquals("", saved.formats)
        assertEquals(now, saved.formatsCheckedAt)
    }

    @Test
    fun `a failed lookup returns null and saves nothing`() = runBlocking {
        database.albumDao().insert(album("1"))
        server.enqueue(MockResponse().setResponseCode(500))

        assertNull(withFormats().loadFormats("1"))

        val saved = database.albumDao().getById("1")!!
        assertNull(saved.formats)
        assertNull(saved.formatsCheckedAt)
    }

    @Test
    fun `a lookup that has started is still saved when the caller is cancelled`() = runBlocking {
        database.albumDao().insert(album("1"))
        server.enqueue(releasesBody("CD").setHeadersDelay(400, TimeUnit.MILLISECONDS))

        val job = launch(Dispatchers.IO) { withFormats().loadFormats("1") }
        assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)
        job.cancelAndJoin()

        assertEquals("CD", database.albumDao().getById("1")?.formats)
    }

    @Test
    fun `lookups go through the throttle`() = runBlocking {
        database.albumDao().insert(album("1"))
        database.albumDao().insert(album("2"))
        server.enqueue(releasesBody("CD"))
        server.enqueue(releasesBody("CD"))
        val viewModel = withFormats(MusicBrainzThrottle(minIntervalMillis = 300))

        val startedAt = System.nanoTime()
        coroutineScope { listOf("1", "2").map { async { viewModel.loadFormats(it) } }.awaitAll() }
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 280)
    }

    @Test
    fun `a server error returns an error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.loadDiscography(artistId = "some-id")

        assertTrue(state is DiscographyUiState.Error)
    }

    private fun album(id: String, owned: Boolean = false, wanted: Boolean = false) = AlbumEntity(
        id = id, artistId = "some-id", title = "Album $id",
        category = "STUDIO_ALBUM", firstReleaseDate = null,
        owned = owned, wanted = wanted
    )

    @Test
    fun `marking an album as wanted saves its artist as a favourite`() = runBlocking {
        database.albumDao().insert(album("1"))

        viewModel.toggleWanted("1", currentlyWanted = false, artistId = "some-id", artistName = "Genesis")

        assertEquals("Genesis", database.artistDao().getById("some-id")?.name)
        assertTrue(database.albumDao().getAlbumsForArtist("some-id")[0].wanted)
    }

    @Test
    fun `un-wanting an album leaves its artist in favourites`() = runBlocking {
        database.artistDao().insert(ArtistEntity("some-id", "Genesis", null, null))
        database.albumDao().insert(album("1", wanted = true))

        viewModel.toggleWanted("1", currentlyWanted = true, artistId = "some-id", artistName = "Genesis")

        assertEquals(false, database.albumDao().getAlbumsForArtist("some-id")[0].wanted)
        assertEquals("Genesis", database.artistDao().getById("some-id")?.name)
    }

    @Test
    fun `marking a wanted album as owned clears its want`() = runBlocking {
        database.albumDao().insert(album("1", wanted = true))

        viewModel.toggleOwned("1", currentlyOwned = false)

        val saved = database.albumDao().getAlbumsForArtist("some-id")[0]
        assertTrue(saved.owned)
        assertEquals(false, saved.wanted)
    }

    @Test
    fun `marking an album as owned leaves the wants on other albums`() = runBlocking {
        database.albumDao().insert(album("1", wanted = true))
        database.albumDao().insert(album("2", wanted = true))

        viewModel.toggleOwned("1", currentlyOwned = false)

        val albums = database.albumDao().getAlbumsForArtist("some-id").sortedBy { it.id }
        assertEquals(false, albums[0].wanted)
        assertTrue(albums[1].wanted)
    }

    @Test
    fun `saving a favourite makes it show as a favourite`() = runBlocking {
        assertEquals(false, viewModel.isFavourite("some-id"))

        viewModel.saveFavourite("some-id", "Genesis")

        assertEquals(true, viewModel.isFavourite("some-id"))
    }

    @Test
    fun `removing a favourite clears its wants but keeps its owned flags`() = runBlocking {
        database.artistDao().insert(ArtistEntity("some-id", "Genesis", null, null))
        database.albumDao().insert(album("1", owned = true, wanted = true))
        database.albumDao().insert(album("2", wanted = true))

        viewModel.removeFavourite("some-id")

        assertNull(database.artistDao().getById("some-id"))
        val albums = database.albumDao().getAlbumsForArtist("some-id").sortedBy { it.id }
        assertTrue(albums[0].owned)
        assertEquals(false, albums[0].wanted)
        assertEquals(false, albums[1].wanted)
    }

    @Test
    fun `a cancelled load does not report a result`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        var state: DiscographyUiState? = null
        val job = launch(Dispatchers.IO) {
            state = viewModel.loadDiscography(artistId = "some-id")
        }
        server.takeRequest(5, TimeUnit.SECONDS)
        job.cancelAndJoin()

        assertNull(state)
    }
}