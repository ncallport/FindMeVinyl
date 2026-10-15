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

/** The release statuses are saved with the formats, by the check of a whole artist and by a lookup of one album. */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelStatusesTest {

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

    private fun release(group: String, status: String?, format: String = "CD") =
        """{ "id": "r-$group-$status", "release-group": { "id": "$group" },
              ${if (status == null) "" else """"status": "$status",""" }
              "media": [ { "format": "$format" } ] }"""

    private fun page(total: Int, vararg releases: String) =
        """{ "release-count": $total, "releases": [ ${releases.joinToString(",")} ] }"""

    private fun enqueue(body: String) = server.enqueue(MockResponse().setBody(body))

    private fun stored(id: String) = runBlocking { database.albumDao().getById(id)!! }

    @Test
    fun `the check saves the statuses of each album with its formats, across pages`() = runBlocking {
        album("a"); album("b"); album("c")
        enqueue(page(4, release("a", "Bootleg"), release("b", "Official")))
        enqueue(page(4, release("a", "Official"), release("c", "Promotion")))

        viewModel().checkAllFormats("artist")

        assertEquals("Bootleg|Official", stored("a").statuses)
        assertEquals("Official", stored("b").statuses)
        assertEquals("Promotion", stored("c").statuses)
        assertEquals("CD", stored("a").formats)
    }

    @Test
    fun `withdrawn and missing statuses are saved as other`() = runBlocking {
        album("a"); album("b")
        enqueue(page(3, release("a", "Withdrawn"), release("b", null), release("b", "Cancelled")))

        viewModel(pageSize = 3).checkAllFormats("artist")

        assertEquals("Other", stored("a").statuses)
        assertEquals("Other", stored("b").statuses)
    }

    @Test
    fun `an album the releases do not mention keeps no statuses`() = runBlocking {
        album("a"); album("z")
        enqueue(page(1, release("a", "Official")))

        viewModel().checkAllFormats("artist")

        assertNull(stored("z").statuses)
    }

    @Test
    fun `a lookup of one album saves its statuses with its formats`() = runBlocking {
        album("a")
        enqueue("""{ "releases": [ { "id": "1", "status": "Bootleg", "media": [ { "format": "CD" } ] },
            { "id": "2", "status": "Official", "media": [ { "format": "12\" Vinyl" } ] } ] }""")

        viewModel().loadFormats("a")

        assertEquals("Bootleg|Official", stored("a").statuses)
        assertEquals("12\" Vinyl|CD", stored("a").formats)
    }

    @Test
    fun `a saved album shows its statuses to the screens`() = runBlocking {
        database.albumDao().insert(
            AlbumEntity("a", "artist", "Album a", "STUDIO_ALBUM", null, owned = false, wanted = false, statuses = "Bootleg|Official")
        )
        album("b")

        val state = viewModel().loadSaved("artist")
        val all = state.groupedAlbums.values.flatten().associateBy { it.id }

        assertEquals(listOf("Bootleg", "Official"), all.getValue("a").statuses)
        assertNull(all.getValue("b").statuses)
    }
}
