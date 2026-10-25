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

class MusicBrainzApiReleaseDetailTest {

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

    // The shape MusicBrainz sends for the 1970 UK pressing of Trespass, cut down. Extra fields are kept.
    private val trespassJson = """
        {
          "id": "f0e2a5a8", "title": "Trespass", "status": "Official", "date": "1970-10", "country": "GB",
          "barcode": "", "packaging": null, "quality": "normal",
          "label-info": [ { "catalog-number": "CAS 1020", "label": { "id": "l1", "name": "Charisma" } } ],
          "release-group": {
            "id": "aa86c6bb", "title": "Trespass", "primary-type": "Album", "secondary-types": [],
            "first-release-date": "1970-10-23", "disambiguation": "",
            "genres": [
              { "name": "rock", "count": 4 }, { "name": "progressive rock", "count": 3 }, { "name": "art rock", "count": 1 }
            ]
          },
          "genres": [],
          "media": [
            {
              "position": 1, "format": "12\" Vinyl", "title": "", "track-count": 6,
              "tracks": [
                { "id": "t1", "number": "A1", "position": 1, "title": "Looking for Someone", "length": 424000 },
                { "id": "t2", "number": "A2", "position": 2, "title": "White Mountain", "length": 400000 },
                { "id": "t3", "number": "B1", "position": 4, "title": "Stagnation", "length": null }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `asks for the release with its recordings, labels and genres`() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))

        api.getReleaseDetail(releaseId = "f0e2a5a8")

        assertEquals(
            "/release/f0e2a5a8?inc=recordings+artist-credits+labels+release-groups+genres&fmt=json",
            server.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `reads the details of the pressing`() = runBlocking {
        server.enqueue(MockResponse().setBody(trespassJson))

        val release = api.getReleaseDetail(releaseId = "f0e2a5a8")

        assertEquals("Trespass", release.title)
        assertEquals("1970-10", release.date)
        assertEquals("GB", release.country)
        assertEquals("Charisma", release.labelInfo?.single()?.label?.name)
        assertEquals("CAS 1020", release.labelInfo?.single()?.catalogNumber)
    }

    @Test
    fun `reads the genres of the release group in the order MusicBrainz gives them`() = runBlocking {
        server.enqueue(MockResponse().setBody(trespassJson))

        val release = api.getReleaseDetail(releaseId = "f0e2a5a8")

        assertEquals(listOf("rock", "progressive rock", "art rock"), release.releaseGroup?.genres?.map { it.name })
        assertEquals(4, release.releaseGroup?.genres?.first()?.count)
        assertEquals("Album", release.releaseGroup?.primaryType)
        assertEquals("1970-10-23", release.releaseGroup?.firstReleaseDate)
    }

    @Test
    fun `reads each track with its number, title and length`() = runBlocking {
        server.enqueue(MockResponse().setBody(trespassJson))

        val medium = api.getReleaseDetail(releaseId = "f0e2a5a8").media!!.single()

        assertEquals("12\" Vinyl", medium.format)
        assertEquals(1, medium.position)
        assertEquals(
            listOf(Triple("A1", "Looking for Someone", 424000L), Triple("A2", "White Mountain", 400000L), Triple("B1", "Stagnation", null)),
            medium.tracks!!.map { Triple(it.number, it.title, it.length) }
        )
    }

    @Test
    fun `a response with nothing in it still reads`() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))

        val release = api.getReleaseDetail(releaseId = "x")

        assertNull(release.title)
        assertNull(release.media)
        assertNull(release.labelInfo)
    }

    @Test
    fun `the release list reads the date and status of each release`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "releases": [ { "id": "r1", "status": "Official", "date": "1970-10", "media": [ { "format": "12\" Vinyl" } ] } ] }"""
            )
        )

        val release = api.getReleasesForReleaseGroup(releaseGroupId = "abc").releases!!.single()

        assertEquals("Official", release.status)
        assertEquals("1970-10", release.date)
    }
}
