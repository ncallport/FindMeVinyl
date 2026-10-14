package com.nickallport.findmevinyl.musicbrainz

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MusicBrainzApiArtistLinksTest {

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

    // The shape MusicBrainz sends for Genesis, cut down. Extra fields are kept.
    private val linksJson = """
        {
          "id": "8e3fcd7d-bda1-4ca0-b987-b8528d2ee74e",
          "name": "Genesis",
          "relations": [
            {
              "type": "image", "type-id": "221132e9", "target-type": "url", "direction": "forward",
              "url": { "id": "u1", "resource": "https://commons.wikimedia.org/wiki/File:2180_-_Genesis.JPG" }
            },
            {
              "type": "official homepage", "target-type": "url",
              "url": { "id": "u2", "resource": "https://www.genesis-music.com/" }
            },
            {
              "type": "wikidata", "target-type": "url",
              "url": { "id": "u3", "resource": "https://www.wikidata.org/wiki/Q151012" }
            },
            { "type": "member of band", "target-type": "artist" }
          ]
        }
    """.trimIndent()

    @Test
    fun `asks for the links of an artist`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "relations": [] }"""))

        api.getArtistLinks(artistId = "8e3fcd7d-bda1-4ca0-b987-b8528d2ee74e")

        assertEquals(
            "/artist/8e3fcd7d-bda1-4ca0-b987-b8528d2ee74e?inc=url-rels&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `reads each link with its type and address`() = runBlocking {
        server.enqueue(MockResponse().setBody(linksJson))

        val response = api.getArtistLinks(artistId = "abc")

        assertEquals(
            listOf(
                "image" to "https://commons.wikimedia.org/wiki/File:2180_-_Genesis.JPG",
                "official homepage" to "https://www.genesis-music.com/",
                "wikidata" to "https://www.wikidata.org/wiki/Q151012",
                "member of band" to null
            ),
            response.relations.orEmpty().map { it.type to it.url?.resource }
        )
    }

    @Test
    fun `a response with no relations has none`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "id": "abc", "name": "Nobody" }"""))

        assertEquals(emptyList<Any>(), api.getArtistLinks(artistId = "abc").relations.orEmpty())
    }
}
