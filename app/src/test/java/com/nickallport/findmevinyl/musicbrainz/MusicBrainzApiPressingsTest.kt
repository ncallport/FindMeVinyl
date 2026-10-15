package com.nickallport.findmevinyl.musicbrainz

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

class MusicBrainzApiPressingsTest {

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
    fun `the release list can ask for labels as well as media`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "releases": [] }"""))

        api.getReleasesForReleaseGroup(releaseGroupId = "rg", include = "media+labels")

        assertEquals(
            "/release/?release-group=rg&inc=media+labels&limit=100&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `the release list still asks for media only by default, as the format lookup does`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "releases": [] }"""))

        api.getReleasesForReleaseGroup(releaseGroupId = "rg")

        assertEquals(
            "/release/?release-group=rg&inc=media&limit=100&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `a listed release carries its country, label and catalogue number`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                { "releases": [
                    { "id": "r1", "status": "Official", "date": "1970", "country": "DE",
                      "label-info": [ { "catalog-number": "6369 905", "label": { "name": "Charisma Records Ltd." } } ],
                      "media": [ { "format": "12\" Vinyl" } ] },
                    { "id": "r2" }
                ] }
                """.trimIndent()
            )
        )

        val releases = api.getReleasesForReleaseGroup(releaseGroupId = "rg", include = "media+labels").releases!!

        assertEquals("DE", releases[0].country)
        assertEquals("6369 905", releases[0].labelInfo?.single()?.catalogNumber)
        assertEquals("Charisma Records Ltd.", releases[0].labelInfo?.single()?.label?.name)
        assertNull(releases[1].country)
        assertNull(releases[1].labelInfo)
    }

    @Test
    fun `asks for the rating of a release group`() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))

        api.getReleaseGroupRating(releaseGroupId = "rg")

        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/release-group/rg?inc=ratings&fmt=json", request?.path)
        assertEquals(AppUserAgent.VALUE, request?.getHeader("User-Agent"))
    }

    @Test
    fun `reads the rating and the number of votes`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "id": "rg", "title": "Trespass", "rating": { "votes-count": 27, "value": 4.3 } }"""
            )
        )

        val rating = api.getReleaseGroupRating(releaseGroupId = "rg").rating

        assertEquals(4.3, rating?.value ?: 0.0, 0.0001)
        assertEquals(27, rating?.votesCount)
    }

    @Test
    fun `a release group nobody has rated has a rating with no value`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "rating": { "votes-count": 0, "value": null } }"""))

        val rating = api.getReleaseGroupRating(releaseGroupId = "rg").rating

        assertNull(rating?.value)
        assertEquals(0, rating?.votesCount)
    }

    @Test
    fun `a response without a rating reads as no rating`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "id": "rg" }"""))

        assertNull(api.getReleaseGroupRating(releaseGroupId = "rg").rating)
    }
}
