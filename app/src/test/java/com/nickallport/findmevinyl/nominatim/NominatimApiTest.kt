package com.nickallport.findmevinyl.nominatim

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NominatimApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NominatimApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends a well-formed search request`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        api.search(query = "London")

        val request = server.takeRequest()
        assertEquals("/search?q=London&format=json&limit=1", request.path)
    }

    @Test
    fun `parses the response into a list of results`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[ { "lat": "51.5073509", "lon": "-0.1277583", "display_name": "London, UK" } ]"""
            )
        )

        val results = api.search(query = "London")

        assertEquals(1, results.size)
        assertEquals("London, UK", results[0].displayName)
    }
}
