package com.nickallport.findmevinyl.musicbrainz

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MusicBrainzApiBarcodeTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MusicBrainzApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // Cut down from what MusicBrainz sent on 2026-10-20 for barcode 5012985305020.
    private val json = """
        {
          "created": "2026-10-20T10:00:00.000Z", "count": 2, "offset": 0,
          "releases": [
            {
              "id": "4dfa21e4-9526-3038-b3df-9110be65deb3", "title": "Avalon", "status": "Official",
              "date": "1987", "country": "GB", "barcode": "5012985305020",
              "artist-credit": [ { "name": "Roxy Music", "artist": { "id": "331ce348", "name": "Roxy Music", "sort-name": "Roxy Music" } } ],
              "release-group": { "id": "4b51d75c", "title": "Avalon", "primary-type": "Album" },
              "media": [ { "id": "m1", "format": "CD", "disc-count": 14, "track-count": 10 } ]
            },
            {
              "id": "r2", "title": "Avalon",
              "release-group": { "id": "4b51d75c", "title": "Avalon" }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `asks for the releases with the barcode`() = runBlocking {
        server.enqueue(MockResponse().setBody(json))

        api.searchReleasesByBarcode("barcode:5012985305020")

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("/release/", request!!.requestUrl!!.encodedPath)
        assertEquals("barcode:5012985305020", request.requestUrl!!.queryParameter("query"))
        assertEquals("json", request.requestUrl!!.queryParameter("fmt"))
    }

    @Test
    fun `reads the artist, the release group and the formats of each release`() = runBlocking {
        server.enqueue(MockResponse().setBody(json))

        val releases = api.searchReleasesByBarcode("barcode:5012985305020").releases.orEmpty()

        assertEquals(2, releases.size)
        val first = releases[0]
        assertEquals("Avalon", first.title)
        assertEquals("331ce348", first.artistCredit!!.first().artist!!.id)
        assertEquals("Roxy Music", first.artistCredit!!.first().artist!!.name)
        assertEquals("4b51d75c", first.releaseGroup!!.id)
        assertEquals("Album", first.releaseGroup!!.primaryType)
        assertEquals(listOf("CD"), first.media!!.map { it.format })
        assertEquals(null, releases[1].artistCredit)
    }

    @Test
    fun `gets one release group with its type and first release date`() = runBlocking {
        server.enqueue(MockResponse().setBody("""
            {"id": "4b51d75c", "title": "Avalon", "primary-type": "Album", "secondary-types": ["Live"], "first-release-date": "1982-05-31"}
        """.trimIndent()))

        val group = api.getReleaseGroup("4b51d75c")

        assertEquals("/release-group/4b51d75c", server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
        assertEquals("Avalon", group.title)
        assertEquals(listOf("Live"), group.secondaryTypes)
        assertEquals("1982-05-31", group.firstReleaseDate)
    }
}
