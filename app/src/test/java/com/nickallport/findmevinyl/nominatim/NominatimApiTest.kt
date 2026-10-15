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
        assertEquals("/search?q=London&format=json&limit=1&addressdetails=1", request.path)
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

    @Test
    fun `sends a well-formed reverse lookup request`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "display_name": "Leiria, Portugal" }"""))

        api.reverse(latitude = 39.7437, longitude = -8.8071)

        val request = server.takeRequest()
        assertEquals("/reverse?lat=39.7437&lon=-8.8071&format=json&zoom=10&addressdetails=1", request.path)
    }

    @Test
    fun `parses the place name from a reverse lookup`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "display_name": "Leiria, Portugal" }"""))

        val result = api.reverse(latitude = 39.7437, longitude = -8.8071)

        assertEquals("Leiria, Portugal", result.displayName)
    }

    @Test
    fun `reads the country and the region from the address of a search result`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[ { "lat": "55.95", "lon": "-3.19", "display_name": "Edinburgh",
                       "address": { "state": "Scotland", "ISO3166-2-lvl4": "GB-SCT", "country": "United Kingdom", "country_code": "gb" } } ]"""
            )
        )

        val address = api.search("Edinburgh").single().address

        assertEquals("gb", address?.countryCode)
        assertEquals("GB-SCT", address?.subdivisionCode)
    }

    @Test
    fun `reads the country and the region from the address of a reverse lookup`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "display_name": "Westminster", "address": { "ISO3166-2-lvl4": "GB-ENG", "country_code": "gb" } }"""
            )
        )

        val address = api.reverse(51.5, -0.12).address

        assertEquals("gb", address?.countryCode)
        assertEquals("GB-ENG", address?.subdivisionCode)
    }

    @Test
    fun `a result with no address reads as no address`() = runBlocking {
        server.enqueue(MockResponse().setBody("""[ { "lat": "1", "lon": "2", "display_name": "Somewhere" } ]"""))

        assertEquals(null, api.search("Somewhere").single().address)
    }
}
