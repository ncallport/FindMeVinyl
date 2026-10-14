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

class MusicBrainzApiReleaseGroupsTest {

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
    fun `requests release groups for a given artist id`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "release-groups": [] }"""))

        api.getReleaseGroupsForArtist(artistId = "1c70a3fc-fe03-4b23-ba9c-873c33dcd814")

        val request = server.takeRequest()
        assertEquals(
            "/release-group/?artist=1c70a3fc-fe03-4b23-ba9c-873c33dcd814&fmt=json",
            request.path
        )
    }

    @Test
    fun `asks for a page of release groups with a limit and an offset`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "release-groups": [] }"""))

        api.getReleaseGroupsForArtist(artistId = "abc", limit = 100, offset = 200)

        assertEquals(
            "/release-group/?artist=abc&fmt=json&limit=100&offset=200",
            server.takeRequest().path
        )
    }

    @Test
    fun `reads the total number of release groups`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{ "release-group-count": 419, "release-group-offset": 0, "release-groups": [] }""")
        )

        val response = api.getReleaseGroupsForArtist(artistId = "abc")

        assertEquals(419, response.releaseGroupCount)
    }

    @Test
    fun `has no total when the response does not give one`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "release-groups": [] }"""))

        val response = api.getReleaseGroupsForArtist(artistId = "abc")

        assertEquals(null, response.releaseGroupCount)
    }

    @Test
    fun `parses the release groups response body`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "release-groups": [ { "id": "abc", "title": "Foxtrot", "primary-type": "Album", "secondary-types": [], "first-release-date": "1972-10-06" } ] }"""
            )
        )

        val response = api.getReleaseGroupsForArtist(artistId = "1c70a3fc-fe03-4b23-ba9c-873c33dcd814")

        assertEquals(1, response.releaseGroups.size)
        assertEquals("Foxtrot", response.releaseGroups[0].title)
    }
}