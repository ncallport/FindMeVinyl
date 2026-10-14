package com.nickallport.findmevinyl.wikidata

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class WikidataApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: WikidataApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/w/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikidataApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // What Wikidata sends for the image (P18) of Genesis, cut down.
    private val claimsJson = """
        {
          "claims": {
            "P18": [
              {
                "mainsnak": {
                  "snaktype": "value", "property": "P18",
                  "datavalue": { "value": "2180 - Pittsburgh - Mellon Arena - Genesis.JPG", "type": "string" },
                  "datatype": "commonsMedia"
                },
                "type": "statement", "rank": "normal"
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `asks for the image of an item`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "claims": {} }"""))

        api.getImageClaims(entityId = "Q151012")

        assertEquals(
            "/w/api.php?action=wbgetclaims&entity=Q151012&property=P18&format=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `reads the file name of the image`() = runBlocking {
        server.enqueue(MockResponse().setBody(claimsJson))

        val claims = api.getImageClaims(entityId = "Q151012").claims?.get("P18").orEmpty()

        assertEquals("2180 - Pittsburgh - Mellon Arena - Genesis.JPG", claims.single().fileName())
        assertEquals("normal", claims.single().rank)
    }

    @Test
    fun `an item with no image has no claims`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "claims": {} }"""))

        assertEquals(emptyList<Any>(), api.getImageClaims(entityId = "Q1").claims?.get("P18").orEmpty())
    }

    @Test
    fun `a claim without a value has no file name`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "claims": { "P18": [ { "mainsnak": { "snaktype": "novalue" }, "rank": "normal" } ] } }"""
            )
        )

        val claim = api.getImageClaims(entityId = "Q1").claims!!.getValue("P18").single()

        assertNull(claim.fileName())
    }
}
