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

class MusicBrainzApiArtistReleasesTest {

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
    fun `asks for a page of an artist's releases with their release groups and media`() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))

        api.getReleasesForArtist(artistId = "artist-1")

        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/release/?artist=artist-1&inc=release-groups+media&limit=100&offset=0&fmt=json", request?.path)
        assertEquals(AppUserAgent.VALUE, request?.getHeader("User-Agent"))
    }

    @Test
    fun `asks for a later page by its offset`() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))

        api.getReleasesForArtist(artistId = "artist-1", limit = 50, offset = 150)

        assertEquals(
            "/release/?artist=artist-1&inc=release-groups+media&limit=50&offset=150&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `reads the total, and each release's release group and media, ignoring other fields`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                { "release-count": 1184, "release-offset": 0,
                  "releases": [
                    { "id": "r1", "title": "Trespass", "status": "Official", "date": "1970",
                      "release-group": { "id": "rg-1", "title": "Trespass", "primary-type": "Album" },
                      "media": [ { "format": "12\" Vinyl", "track-count": 6 }, { "format": "CD" } ] },
                    { "id": "r2" }
                  ] }
                """.trimIndent()
            )
        )

        val page = api.getReleasesForArtist(artistId = "artist-1")

        assertEquals(1184, page.releaseCount)
        assertEquals("rg-1", page.releases!![0].releaseGroup?.id)
        assertEquals(listOf("12\" Vinyl", "CD"), page.releases!![0].media?.map { it.format })
        assertNull(page.releases!![1].releaseGroup)
        assertNull(page.releases!![1].media)
    }

    @Test
    fun `a response with no releases reads as an empty page`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{ "release-count": 0, "releases": [] }"""))

        val page = api.getReleasesForArtist(artistId = "artist-1")

        assertEquals(0, page.releaseCount)
        assertEquals(emptyList<MusicBrainzReleaseDto>(), page.releases)
    }
}
