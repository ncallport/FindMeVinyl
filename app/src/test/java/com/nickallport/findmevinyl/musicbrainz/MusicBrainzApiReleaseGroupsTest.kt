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