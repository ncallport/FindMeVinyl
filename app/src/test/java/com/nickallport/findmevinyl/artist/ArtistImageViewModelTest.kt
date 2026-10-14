package com.nickallport.findmevinyl.artist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistImageEntity
import com.nickallport.findmevinyl.wikidata.WikidataApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

@RunWith(RobolectricTestRunner::class)
class ArtistImageViewModelTest {

    private lateinit var musicBrainzServer: MockWebServer
    private lateinit var wikidataServer: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var finder: ArtistImageFinder

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val commons = "https://commons.test"

    @Before
    fun setUp() {
        musicBrainzServer = MockWebServer().apply { start() }
        wikidataServer = MockWebServer().apply { start() }
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val musicBrainz = Retrofit.Builder().baseUrl(musicBrainzServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(MusicBrainzApi::class.java)
        val wikidata = Retrofit.Builder().baseUrl(wikidataServer.url("/w/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(WikidataApi::class.java)
        finder = ArtistImageFinder(musicBrainz, wikidata, commonsBaseUrl = commons, width = 120)
    }

    @After
    fun tearDown() {
        musicBrainzServer.shutdown()
        wikidataServer.shutdown()
        database.close()
    }

    private fun viewModel(throttle: MusicBrainzThrottle = MusicBrainzThrottle(minIntervalMillis = 0)) =
        ArtistImageViewModel(finder, database.artistImageDao(), throttle = throttle, clock = { now })

    private fun enqueueWikidataLink(item: String = "Q1") {
        musicBrainzServer.enqueue(
            MockResponse().setBody(
                """{ "relations": [ { "type": "wikidata", "url": { "resource": "https://www.wikidata.org/wiki/$item" } } ] }"""
            )
        )
    }

    private fun enqueueWikidataFile(file: String) {
        wikidataServer.enqueue(
            MockResponse().setBody(
                """{ "claims": { "P18": [ { "rank": "normal", "mainsnak": { "datavalue": { "value": "$file" } } } ] } }"""
            )
        )
    }

    private val pictureUrl = "$commons/wiki/Special:FilePath/Genesis.jpg?width=120"

    @Test
    fun `gives a stored picture without asking anyone`() = runBlocking {
        database.artistImageDao().save(ArtistImageEntity("a", pictureUrl, checkedAt = now - 10 * day))

        assertEquals(pictureUrl, viewModel().loadImageUrl("a"))
        assertEquals(0, musicBrainzServer.requestCount)
        assertEquals(0, wikidataServer.requestCount)
    }

    @Test
    fun `gives no picture for an artist stored as having none, without asking anyone`() = runBlocking {
        database.artistImageDao().save(ArtistImageEntity("a", null, checkedAt = now - 10 * day))

        assertNull(viewModel().loadImageUrl("a"))
        assertEquals(0, musicBrainzServer.requestCount)
    }

    @Test
    fun `finds the picture of an artist never looked up, saves it and returns it`() = runBlocking {
        enqueueWikidataLink("Q151012")
        enqueueWikidataFile("Genesis.jpg")

        val url = viewModel().loadImageUrl("abc")

        assertEquals(pictureUrl, url)
        val saved = database.artistImageDao().get("abc")!!
        assertEquals(pictureUrl, saved.imageUrl)
        assertEquals(now, saved.checkedAt)
        assertEquals(
            "/artist/abc?inc=url-rels&fmt=json",
            musicBrainzServer.takeRequest(2, TimeUnit.SECONDS)?.path
        )
        assertEquals(
            "/w/api.php?action=wbgetclaims&entity=Q151012&property=P18&format=json",
            wikidataServer.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `saves that an artist has no picture so it is not asked for again`() = runBlocking {
        musicBrainzServer.enqueue(MockResponse().setBody("""{ "relations": [] }"""))

        assertNull(viewModel().loadImageUrl("abc"))

        val saved = database.artistImageDao().get("abc")!!
        assertNull(saved.imageUrl)
        assertEquals(now, saved.checkedAt)
        assertEquals(0, wikidataServer.requestCount)
    }

    @Test
    fun `looks again after 90 days and replaces the old picture`() = runBlocking {
        database.artistImageDao().save(ArtistImageEntity("abc", "$commons/old", checkedAt = now - 91 * day))
        enqueueWikidataLink()
        enqueueWikidataFile("Genesis.jpg")

        assertEquals(pictureUrl, viewModel().loadImageUrl("abc"))

        assertEquals(pictureUrl, database.artistImageDao().get("abc")?.imageUrl)
        assertEquals(now, database.artistImageDao().get("abc")?.checkedAt)
    }

    @Test
    fun `does not look again before 90 days`() = runBlocking {
        database.artistImageDao().save(ArtistImageEntity("abc", "$commons/old", checkedAt = now - 89 * day))

        assertEquals("$commons/old", viewModel().loadImageUrl("abc"))
        assertEquals(0, musicBrainzServer.requestCount)
    }

    @Test
    fun `a MusicBrainz failure gives no picture, saves nothing, and is tried again next time`() = runBlocking {
        musicBrainzServer.enqueue(MockResponse().setResponseCode(503))
        enqueueWikidataLink()
        enqueueWikidataFile("Genesis.jpg")
        val viewModel = viewModel()

        assertNull(viewModel.loadImageUrl("abc"))
        assertNull(database.artistImageDao().get("abc"))

        assertEquals(pictureUrl, viewModel.loadImageUrl("abc"))
    }

    @Test
    fun `a Wikidata failure gives no picture and saves nothing`() = runBlocking {
        enqueueWikidataLink()
        wikidataServer.enqueue(MockResponse().setResponseCode(503))

        assertNull(viewModel().loadImageUrl("abc"))

        assertNull(database.artistImageDao().get("abc"))
    }

    @Test
    fun `lookups go through the MusicBrainz queue`() = runBlocking {
        repeat(2) {
            enqueueWikidataLink()
            enqueueWikidataFile("Genesis.jpg")
        }
        val viewModel = viewModel(MusicBrainzThrottle(minIntervalMillis = 300))

        val startedAt = System.nanoTime()
        coroutineScope { listOf("one", "two").map { async { viewModel.loadImageUrl(it) } }.awaitAll() }
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("took $elapsedMillis ms", elapsedMillis >= 280)
    }

    @Test
    fun `a lookup that has started is still saved when the caller is cancelled`() = runBlocking {
        musicBrainzServer.enqueue(
            MockResponse()
                .setBody("""{ "relations": [ { "type": "wikidata", "url": { "resource": "https://www.wikidata.org/wiki/Q1" } } ] }""")
                .setHeadersDelay(400, TimeUnit.MILLISECONDS)
        )
        enqueueWikidataFile("Genesis.jpg")

        val job = launch(Dispatchers.IO) { viewModel().loadImageUrl("abc") }
        assertTrue(musicBrainzServer.takeRequest(2, TimeUnit.SECONDS) != null)
        job.cancelAndJoin()

        assertEquals(pictureUrl, database.artistImageDao().get("abc")?.imageUrl)
    }
}
