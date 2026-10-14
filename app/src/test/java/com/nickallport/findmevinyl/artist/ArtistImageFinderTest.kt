package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.wikidata.WikidataApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ArtistImageFinderTest {

    private lateinit var musicBrainzServer: MockWebServer
    private lateinit var wikidataServer: MockWebServer
    private lateinit var finder: ArtistImageFinder

    private val commons = "https://commons.test"

    @Before
    fun setUp() {
        musicBrainzServer = MockWebServer().apply { start() }
        wikidataServer = MockWebServer().apply { start() }

        val musicBrainz = Retrofit.Builder()
            .baseUrl(musicBrainzServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
        val wikidata = Retrofit.Builder()
            .baseUrl(wikidataServer.url("/w/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikidataApi::class.java)

        finder = ArtistImageFinder(musicBrainz, wikidata, commonsBaseUrl = commons, width = 120)
    }

    @After
    fun tearDown() {
        musicBrainzServer.shutdown()
        wikidataServer.shutdown()
    }

    private fun link(type: String, resource: String) =
        """{ "type": "$type", "url": { "resource": "$resource" } }"""

    private fun musicBrainzLinks(vararg links: String) {
        musicBrainzServer.enqueue(MockResponse().setBody("""{ "relations": [ ${links.joinToString(",")} ] }"""))
    }

    private fun wikidataClaims(vararg claims: Pair<String, String>) {
        val body = claims.joinToString(",") { (rank, file) ->
            """{ "rank": "$rank", "mainsnak": { "datavalue": { "value": "$file", "type": "string" } } }"""
        }
        wikidataServer.enqueue(MockResponse().setBody("""{ "claims": { "P18": [ $body ] } }"""))
    }

    private fun wikidataNoClaims() {
        wikidataServer.enqueue(MockResponse().setBody("""{ "claims": {} }"""))
    }

    private fun find(artistId: String = "abc"): String? = runBlocking {
        finder.pictureUrl(finder.fetchLinks(artistId))
    }

    @Test
    fun `finds the picture through Wikidata`() {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Q151012"))
        wikidataClaims("normal" to "2180 - Pittsburgh - Genesis.JPG")

        assertEquals("$commons/wiki/Special:FilePath/2180%20-%20Pittsburgh%20-%20Genesis.JPG?width=120", find())
        assertEquals(
            "/w/api.php?action=wbgetclaims&entity=Q151012&property=P18&format=json",
            wikidataServer.takeRequest(2, TimeUnit.SECONDS)?.path
        )
    }

    @Test
    fun `reads the item number from the Wikidata link`() = runBlocking {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Q151012"))

        assertEquals("Q151012", finder.fetchLinks("abc").wikidataId)
    }

    @Test
    fun `prefers the Wikidata picture to the MusicBrainz image link`() {
        musicBrainzLinks(
            link("image", "https://commons.wikimedia.org/wiki/File:From_MusicBrainz.jpg"),
            link("wikidata", "https://www.wikidata.org/wiki/Q1")
        )
        wikidataClaims("normal" to "From Wikidata.jpg")

        assertEquals("$commons/wiki/Special:FilePath/From%20Wikidata.jpg?width=120", find())
    }

    @Test
    fun `falls back to the MusicBrainz image link when Wikidata has no picture`() {
        musicBrainzLinks(
            link("image", "https://commons.wikimedia.org/wiki/File:2180_-_Genesis.JPG"),
            link("wikidata", "https://www.wikidata.org/wiki/Q1")
        )
        wikidataNoClaims()

        assertEquals("$commons/wiki/Special:FilePath/2180_-_Genesis.JPG?width=120", find())
    }

    @Test
    fun `uses the MusicBrainz image link without asking Wikidata when there is no Wikidata link`() {
        musicBrainzLinks(link("image", "https://commons.wikimedia.org/wiki/File:Only_Here.jpg"))

        assertEquals("$commons/wiki/Special:FilePath/Only_Here.jpg?width=120", find())
        assertEquals(0, wikidataServer.requestCount)
    }

    @Test
    fun `finds nothing when there are no usable links, and does not ask Wikidata`() {
        musicBrainzLinks(link("official homepage", "https://example.com/"))

        assertNull(find())
        assertEquals(0, wikidataServer.requestCount)
    }

    @Test
    fun `finds nothing when Wikidata has no picture and there is no image link`() {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Q1"))
        wikidataNoClaims()

        assertNull(find())
    }

    @Test
    fun `skips a deprecated Wikidata claim`() {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Q1"))
        wikidataClaims("deprecated" to "Old.jpg", "normal" to "Good.jpg")

        assertEquals("$commons/wiki/Special:FilePath/Good.jpg?width=120", find())
    }

    @Test
    fun `falls back when the only Wikidata claim is deprecated`() {
        musicBrainzLinks(
            link("image", "https://commons.wikimedia.org/wiki/File:Fallback.jpg"),
            link("wikidata", "https://www.wikidata.org/wiki/Q1")
        )
        wikidataClaims("deprecated" to "Old.jpg")

        assertEquals("$commons/wiki/Special:FilePath/Fallback.jpg?width=120", find())
    }

    @Test
    fun `ignores a Wikidata link that is not an item page`() = runBlocking {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Property:P18"))

        assertNull(finder.fetchLinks("abc").wikidataId)
    }

    @Test
    fun `ignores an image link that is not a Commons file`() {
        musicBrainzLinks(link("image", "https://example.com/pictures/band.jpg"))

        assertNull(find())
    }

    @Test
    fun `encodes a file name that needs it`() {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Q1"))
        wikidataClaims("normal" to "Björk & friends (live) 'x'.jpg")

        assertEquals(
            "$commons/wiki/Special:FilePath/Bj%C3%B6rk%20%26%20friends%20%28live%29%20%27x%27.jpg?width=120",
            find()
        )
    }

    @Test
    fun `decodes a percent-encoded MusicBrainz image link`() {
        musicBrainzLinks(link("image", "https://commons.wikimedia.org/wiki/File:Bj%C3%B6rk_live.jpg"))

        assertEquals("$commons/wiki/Special:FilePath/Bj%C3%B6rk_live.jpg?width=120", find())
    }

    @Test
    fun `passes on a MusicBrainz failure`() {
        musicBrainzServer.enqueue(MockResponse().setResponseCode(503))

        try {
            find()
            org.junit.Assert.fail("expected an HttpException")
        } catch (e: HttpException) {
            assertEquals(503, e.code())
        }
    }

    @Test
    fun `passes on a Wikidata failure`() {
        musicBrainzLinks(link("wikidata", "https://www.wikidata.org/wiki/Q1"))
        wikidataServer.enqueue(MockResponse().setResponseCode(503))

        try {
            find()
            org.junit.Assert.fail("expected an HttpException")
        } catch (e: HttpException) {
            assertTrue(e.code() == 503)
        }
    }
}
