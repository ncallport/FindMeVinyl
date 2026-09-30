package com.nickallport.findmevinyl.overpass

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OverpassApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OverpassApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends the query as a POST with the query text in the body`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "elements": [] }"""))

        val query = OverpassQueryBuilder.shopsNear(51.5074, -0.1278, 5000)
        val body = query.toRequestBody("text/plain".toMediaType())

        api.query(body)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.body.readUtf8().contains("shop=music"))
    }

    @Test
    fun `parses the response body into an OverpassResponse`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "elements": [ { "type": "node", "id": 1, "lat": 51.5, "lon": -0.1, "tags": { "shop": "music", "name": "Test Shop" } } ] }"""
            )
        )

        val query = OverpassQueryBuilder.shopsNear(51.5074, -0.1278, 5000)
        val body = query.toRequestBody("text/plain".toMediaType())

        val response = api.query(body)

        assertEquals(1, response.elements.size)
        assertEquals("Test Shop", response.elements[0].tags?.get("name"))
    }
}
