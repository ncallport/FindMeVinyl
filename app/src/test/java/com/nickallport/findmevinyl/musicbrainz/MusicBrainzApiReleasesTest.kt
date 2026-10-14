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

class MusicBrainzApiReleasesTest {

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

    // The shape MusicBrainz sends, cut down: a vinyl release, a release of a CD and a cassette, a
    // release whose medium has no format, and a release with no media at all. Extra fields are kept.
    private val releasesJson = """
        {
          "release-offset": 0,
          "release-count": 4,
          "releases": [
            {
              "id": "r1", "title": "Trespass", "status": "Official", "country": "GB",
              "media": [ { "format": "12\" Vinyl", "format-id": "f1", "position": 1, "track-count": 6, "title": "" } ]
            },
            {
              "id": "r2", "title": "Trespass", "status": "Official",
              "media": [
                { "format": "CD", "position": 1, "track-count": 6 },
                { "format": "Cassette", "position": 2, "track-count": 6 }
              ]
            },
            {
              "id": "r3", "title": "Trespass",
              "media": [ { "format": null, "position": 1, "track-count": 6 }, { "format": "CD", "position": 2 } ]
            },
            { "id": "r4", "title": "Trespass" }
          ]
        }
    """.trimIndent()

    @Test
    fun `asks for the releases of a release group with their media`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "releases": [] }"""))

        api.getReleasesForReleaseGroup(releaseGroupId = "abc")

        assertEquals(
            "/release/?release-group=abc&inc=media&limit=100&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `lists each format once, in the order they first appear`() = runBlocking {
        server.enqueue(MockResponse().setBody(releasesJson))

        val response = api.getReleasesForReleaseGroup(releaseGroupId = "abc")

        assertEquals(listOf("12\" Vinyl", "CD", "Cassette"), response.formatNames())
    }

    @Test
    fun `a response with no releases has no formats`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "release-count": 0 }"""))

        val response = api.getReleasesForReleaseGroup(releaseGroupId = "abc")

        assertEquals(emptyList<String>(), response.formatNames())
    }
}
