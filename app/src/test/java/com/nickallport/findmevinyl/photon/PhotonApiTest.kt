package com.nickallport.findmevinyl.photon

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

class PhotonApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: PhotonApi

    private val response = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {
                "osm_type": "R", "osm_id": 3992954, "osm_key": "place", "osm_value": "city", "type": "city",
                "name": "Caldas da Rainha", "county": "Leiria", "country": "Portugal", "countrycode": "PT"
              },
              "geometry": { "type": "Point", "coordinates": [-9.1346004, 39.4071857] }
            },
            {
              "type": "Feature",
              "properties": {
                "osm_type": "R", "osm_id": 58437, "osm_key": "place", "osm_value": "city", "type": "city",
                "name": "Newport", "county": "Newport", "state": "Wales", "country": "United Kingdom", "countrycode": "GB"
              },
              "geometry": { "type": "Point", "coordinates": [-2.9977, 51.5842] }
            }
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotonApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `asks for towns and districts only, a few at a time`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "features": [] }"""))

        api.search("Caldas da Rai")

        val request = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
        assertEquals("/api/?q=Caldas%20da%20Rai&limit=5&layer=city&layer=district", request.path)
    }

    @Test
    fun `sends the app's user agent`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "features": [] }"""))

        api.search("Lisbon")

        assertEquals(AppUserAgent.VALUE, server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.getHeader("User-Agent"))
    }

    @Test
    fun `turns the features into suggestions with a label, a position and the country`() {
        val dto = com.google.gson.Gson().fromJson(response, PhotonResponseDto::class.java)

        val suggestions = PhotonMapper.suggestions(dto)

        assertEquals(
            listOf(
                PlaceSuggestion("Caldas da Rainha, Leiria, Portugal", 39.4071857, -9.1346004, "PT"),
                PlaceSuggestion("Newport, Wales, United Kingdom", 51.5842, -2.9977, "GB")
            ),
            suggestions
        )
    }

    @Test
    fun `a feature with no name or position is left out`() {
        val dto = com.google.gson.Gson().fromJson(
            """
            { "features": [
              { "properties": { "country": "Portugal" }, "geometry": { "coordinates": [-9.1, 39.4] } },
              { "properties": { "name": "Nowhere" }, "geometry": { "coordinates": [] } },
              { "properties": { "name": "Lisbon", "country": "Portugal", "countrycode": "PT" }, "geometry": { "coordinates": [-9.14, 38.72] } }
            ] }
            """.trimIndent(),
            PhotonResponseDto::class.java
        )

        assertEquals(listOf("Lisbon, Portugal"), PhotonMapper.suggestions(dto).map { it.label })
    }

    @Test
    fun `two features with the same label are one suggestion`() {
        val dto = com.google.gson.Gson().fromJson(
            """
            { "features": [
              { "properties": { "name": "Lisbon", "country": "Portugal" }, "geometry": { "coordinates": [-9.14, 38.72] } },
              { "properties": { "name": "Lisbon", "country": "Portugal" }, "geometry": { "coordinates": [-9.20, 38.70] } }
            ] }
            """.trimIndent(),
            PhotonResponseDto::class.java
        )

        assertEquals(1, PhotonMapper.suggestions(dto).size)
    }
}
