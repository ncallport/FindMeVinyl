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

class MusicBrainzApiTest {

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

    @Test
    fun `sends a request with a MusicBrainz-compliant User-Agent header`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "artists": [] }"""))

        api.searchArtists(query = "Genesis")

        val request = server.takeRequest()
        assertEquals("/artist/?query=Genesis&fmt=json", request.path)

        val userAgent = request.getHeader("User-Agent")
        assertEquals(true, userAgent != null && Regex("""^\S+/\S+ \(.+\)$""").matches(userAgent))
    }

    @Test
    fun `parses a real response body into the expected type`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "artists": [ { "id": "1", "name": "Genesis", "type": "Group", "country": "GB", "life-span": null, "disambiguation": null } ] }"""
            )
        )

        val response = api.searchArtists(query = "Genesis")

        assertEquals(1, response.artists.size)
        assertEquals("Genesis", response.artists[0].name)
    }
}