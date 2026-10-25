package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumDetailsEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class AlbumDetailsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var api: MusicBrainzApi

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

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

    private fun viewModel(throttle: MusicBrainzThrottle = MusicBrainzThrottle(minIntervalMillis = 0)) =
        AlbumDetailsViewModel(api, database.albumDetailsDao(), throttle = throttle, clock = { now })

    private val releaseList = """
        { "releases": [
            { "id": "cd-1987", "status": "Official", "date": "1987", "media": [ { "format": "CD" } ] },
            { "id": "vinyl-1972", "status": "Official", "date": "1972-05", "media": [ { "format": "12\" Vinyl" } ] },
            { "id": "vinyl-1971", "status": "Official", "date": "1971", "media": [ { "format": "12\" Vinyl" } ] }
        ] }
    """.trimIndent()

    private val releaseDetail = """
        { "title": "Trespass", "date": "1971", "country": "GB",
          "label-info": [ { "catalog-number": "CAS 1020", "label": { "name": "Charisma" } } ],
          "release-group": { "genres": [ { "name": "rock", "count": 4 } ] },
          "media": [ { "position": 1, "format": "12\" Vinyl", "tracks": [ { "number": "A1", "title": "Looking for Someone", "length": 424000 } ] } ] }
    """.trimIndent()

    private val rating = """{ "id": "album", "rating": { "votes-count": 27, "value": 4.3 } }"""

    // The three requests of a new lookup, in the order they are made.
    private fun enqueueFetch() {
        server.enqueue(MockResponse().setBody(releaseList))
        server.enqueue(MockResponse().setBody(releaseDetail))
        server.enqueue(MockResponse().setBody(rating))
    }

    private val stored = AlbumDetails(
        "1970", "GB", "Stored Label", null, null, null, emptyList(), emptyList()
    )

    private suspend fun store(details: AlbumDetails = stored, fetchedAt: Long = now - 10 * day) {
        database.albumDetailsDao().save(AlbumDetailsEntity("album", AlbumDetailsJson.encode(details), fetchedAt))
    }

    @Test
    fun `gives stored details without asking anyone`() = runBlocking {
        store()

        assertEquals(stored, viewModel().load("album"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `finds the details of an album never looked up, saves them and returns them`() = runBlocking {
        enqueueFetch()

        val details = viewModel().load("album")

        assertEquals("Charisma", details?.label)
        assertEquals("CAS 1020", details?.catalogNumber)
        assertEquals(listOf("rock"), details?.genres)
        assertEquals("Looking for Someone", details?.discs?.single()?.tracks?.single()?.title)
        val saved = database.albumDetailsDao().get("album")!!
        assertEquals(now, saved.fetchedAt)
        assertEquals(details, AlbumDetailsJson.decode(saved.json))
    }

    @Test
    fun `lists the releases of the album, fetches the earliest vinyl one, then asks for the rating`() = runBlocking {
        enqueueFetch()

        viewModel().load("album")

        assertEquals(
            "/release/?release-group=album&inc=media+labels&limit=100&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
        assertEquals(
            "/release/vinyl-1971?inc=recordings+artist-credits+labels+release-groups+genres&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
        assertEquals("/release-group/album?inc=ratings&fmt=json", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `gives the rating of the album`() = runBlocking {
        enqueueFetch()

        assertEquals(Rating(4.3, 27), viewModel().load("album")?.rating)
    }

    @Test
    fun `an album nobody has rated has no rating`() = runBlocking {
        server.enqueue(MockResponse().setBody(releaseList))
        server.enqueue(MockResponse().setBody(releaseDetail))
        server.enqueue(MockResponse().setBody("""{ "rating": { "votes-count": 0, "value": null } }"""))

        val details = viewModel().load("album")

        assertNull(details?.rating)
        assertEquals("Charisma", details?.label)
    }

    @Test
    fun `lists the vinyl pressings, earliest first, and says which one the page opens on`() = runBlocking {
        enqueueFetch()

        val details = viewModel().load("album")!!

        assertEquals(listOf("vinyl-1971", "vinyl-1972"), details.pressings.map { it.releaseId })
        assertEquals("vinyl-1971", details.pressingId)
    }

    @Test
    fun `a pressing in the list has what the release list says about it`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "releases": [ { "id": "a", "status": "Official", "date": "1971", "country": "GB",
                     "label-info": [ { "catalog-number": "CAS 1020", "label": { "name": "Charisma" } } ],
                     "media": [ { "format": "12\" Vinyl" } ] } ] }"""
            )
        )
        server.enqueue(MockResponse().setBody(releaseDetail))
        server.enqueue(MockResponse().setBody(rating))

        val pressing = viewModel().load("album")!!.pressings.single()

        assertEquals(Pressing("a", "1971", "GB", "Charisma", "CAS 1020", "12\" Vinyl"), pressing)
    }

    @Test
    fun `lists at most 30 pressings`() = runBlocking {
        val many = (1..35).joinToString(",") {
            """{ "id": "v$it", "status": "Official", "date": "${1970 + it}", "media": [ { "format": "12\" Vinyl" } ] }"""
        }
        server.enqueue(MockResponse().setBody("""{ "releases": [ $many ] }"""))
        server.enqueue(MockResponse().setBody(releaseDetail))
        server.enqueue(MockResponse().setBody(rating))

        val details = viewModel().load("album")!!

        assertEquals(30, details.pressings.size)
        assertEquals("v1", details.pressings.first().releaseId)
        assertEquals("v30", details.pressings.last().releaseId)
    }

    @Test
    fun `looks again after 90 days and replaces the old details`() = runBlocking {
        store(fetchedAt = now - 91 * day)
        enqueueFetch()

        assertEquals("Charisma", viewModel().load("album")?.label)

        assertEquals("Charisma", AlbumDetailsJson.decode(database.albumDetailsDao().get("album")!!.json)?.label)
        assertEquals(now, database.albumDetailsDao().get("album")?.fetchedAt)
    }

    @Test
    fun `does not look again before 90 days`() = runBlocking {
        store(fetchedAt = now - 89 * day)

        assertEquals("Stored Label", viewModel().load("album")?.label)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `fetches again when the stored row cannot be read`() = runBlocking {
        database.albumDetailsDao().save(AlbumDetailsEntity("album", "not json", now - day))
        enqueueFetch()

        assertEquals("Charisma", viewModel().load("album")?.label)
    }

    @Test
    fun `an album with no releases is saved as having no details, so it is not asked for again`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "releases": [] }"""))
        val viewModel = viewModel()

        val details = viewModel.load("album")

        assertFalse(details!!.hasContent)
        assertEquals(1, server.requestCount)
        assertNotNull(database.albumDetailsDao().get("album"))

        viewModel.load("album")
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a failure listing the releases gives nothing, saves nothing, and is tried again next time`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        enqueueFetch()
        val viewModel = viewModel()

        assertNull(viewModel.load("album"))
        assertNull(database.albumDetailsDao().get("album"))

        assertEquals("Charisma", viewModel.load("album")?.label)
    }

    @Test
    fun `a failure fetching the chosen release gives nothing and saves nothing`() = runBlocking {
        server.enqueue(MockResponse().setBody(releaseList))
        server.enqueue(MockResponse().setResponseCode(500))

        assertNull(viewModel().load("album"))

        assertNull(database.albumDetailsDao().get("album"))
    }

    @Test
    fun `a failure asking for the rating gives nothing and saves nothing, so it is tried again`() = runBlocking {
        server.enqueue(MockResponse().setBody(releaseList))
        server.enqueue(MockResponse().setBody(releaseDetail))
        server.enqueue(MockResponse().setResponseCode(503))
        val viewModel = viewModel()

        assertNull(viewModel.load("album"))
        assertNull(database.albumDetailsDao().get("album"))

        enqueueFetch()
        assertEquals(Rating(4.3, 27), viewModel.load("album")?.rating)
    }

    @Test
    fun `fetches again when the stored row was written before pressings and ratings were kept`() = runBlocking {
        database.albumDetailsDao().save(
            AlbumDetailsEntity("album", """{ "label": "Old Label", "genres": [], "discs": [] }""", now - day)
        )
        enqueueFetch()

        val details = viewModel().load("album")

        assertEquals("Charisma", details?.label)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `each request waits for its slot in the queue`() = runBlocking {
        enqueueFetch()

        val startedAt = System.nanoTime()
        viewModel(MusicBrainzThrottle(minIntervalMillis = 400)).load("album")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 780)
    }

    @Test
    fun `a lookup that has started is still saved when the caller is cancelled`() = runBlocking {
        server.enqueue(MockResponse().setBody(releaseList).setHeadersDelay(300, TimeUnit.MILLISECONDS))
        server.enqueue(MockResponse().setBody(releaseDetail))
        server.enqueue(MockResponse().setBody(rating))

        val job = launch(Dispatchers.IO) { viewModel().load("album") }
        assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)
        job.cancelAndJoin()

        assertEquals("Charisma", AlbumDetailsJson.decode(database.albumDetailsDao().get("album")!!.json)?.label)
    }

    // Choosing another pressing of an album already loaded.

    private val otherDetail = """
        { "title": "Trespass", "date": "1972-05", "country": "US",
          "label-info": [ { "catalog-number": "ABC 1", "label": { "name": "Buddah" } } ],
          "media": [ { "position": 1, "format": "12\" Vinyl", "tracks": [ { "number": "A1", "title": "Another Take", "length": 1000 } ] } ] }
    """.trimIndent()

    @Test
    fun `choosing another pressing fetches it and shows it with the album's own genres, pressings and rating`() = runBlocking {
        enqueueFetch()
        val viewModel = viewModel()
        viewModel.load("album")
        server.takeRequest(2, TimeUnit.SECONDS); server.takeRequest(2, TimeUnit.SECONDS); server.takeRequest(2, TimeUnit.SECONDS)
        server.enqueue(MockResponse().setBody(otherDetail))

        val details = viewModel.selectPressing("album", "vinyl-1972")!!

        assertEquals("/release/vinyl-1972?inc=recordings+artist-credits+labels&fmt=json", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        assertEquals("vinyl-1972", details.pressingId)
        assertEquals("US", details.country)
        assertEquals("Buddah", details.label)
        assertEquals("Another Take", details.discs.single().tracks.single().title)
        assertEquals(listOf("rock"), details.genres)
        assertEquals(listOf("vinyl-1971", "vinyl-1972"), details.pressings.map { it.releaseId })
        assertEquals(Rating(4.3, 27), details.rating)
    }

    @Test
    fun `choosing the pressing the page opened on needs no request`() = runBlocking {
        enqueueFetch()
        val viewModel = viewModel()
        val loaded = viewModel.load("album")!!
        val before = server.requestCount

        assertEquals(loaded, viewModel.selectPressing("album", "vinyl-1971"))
        assertEquals(before, server.requestCount)
    }

    @Test
    fun `choosing a pressing again in the same run needs no second request`() = runBlocking {
        enqueueFetch()
        val viewModel = viewModel()
        viewModel.load("album")
        server.enqueue(MockResponse().setBody(otherDetail))

        val first = viewModel.selectPressing("album", "vinyl-1972")
        val before = server.requestCount
        val second = viewModel.selectPressing("album", "vinyl-1972")

        assertEquals(first, second)
        assertEquals(before, server.requestCount)
    }

    @Test
    fun `choosing a pressing does not change what is stored for the album`() = runBlocking {
        enqueueFetch()
        val viewModel = viewModel()
        val loaded = viewModel.load("album")
        server.enqueue(MockResponse().setBody(otherDetail))

        viewModel.selectPressing("album", "vinyl-1972")

        assertEquals(loaded, AlbumDetailsJson.decode(database.albumDetailsDao().get("album")!!.json))
    }

    @Test
    fun `a failure fetching the pressing gives nothing, and it can be tried again`() = runBlocking {
        enqueueFetch()
        val viewModel = viewModel()
        viewModel.load("album")
        server.enqueue(MockResponse().setResponseCode(503))

        assertNull(viewModel.selectPressing("album", "vinyl-1972"))

        server.enqueue(MockResponse().setBody(otherDetail))
        assertEquals("Buddah", viewModel.selectPressing("album", "vinyl-1972")?.label)
    }

    @Test
    fun `choosing a pressing of an album with no stored details gives nothing and asks no one`() = runBlocking {
        assertNull(viewModel().selectPressing("album", "vinyl-1972"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `choosing a pressing waits for its slot in the queue`() = runBlocking {
        enqueueFetch()
        val viewModel = viewModel(MusicBrainzThrottle(minIntervalMillis = 400))
        viewModel.load("album")
        server.enqueue(MockResponse().setBody(otherDetail))

        val startedAt = System.nanoTime()
        viewModel.selectPressing("album", "vinyl-1972")
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 380)
    }
}
