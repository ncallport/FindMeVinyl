package com.nickallport.findmevinyl.barcode

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.favourites.SpecialArtists
import com.nickallport.findmevinyl.discogs.DiscogsApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AppDatabase
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** The barcode lookup when MusicBrainz does not know the barcode and Discogs is asked. */
@RunWith(RobolectricTestRunner::class)
class BarcodeDiscogsTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: BarcodeViewModel

    private val barcode = "198029779813"

    private fun build(withDiscogs: Boolean = true) {
        val retrofit = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        val api = retrofit.create(MusicBrainzApi::class.java)
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 0)
        val discography = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(), throttle = throttle, groupDao = database.artistGroupDao()
        )
        viewModel = BarcodeViewModel(
            api, database.albumDao(), database.artistDao(), database.artistGroupDao(), discography, throttle,
            discogs = if (withDiscogs) retrofit.create(DiscogsApi::class.java) else null
        )
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        build()
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private val mbEmpty = """{"count": 0, "releases": []}"""

    private fun discogsBody(title: String = "Adam & The Ants* - The Singles", year: String = "2026", vararg descriptions: String) = """
        {"pagination": {"items": 1}, "results": [
          {"title": "$title", "year": "$year", "label": ["Sony Music"],
           "formats": [{"name": "Vinyl", "qty": "2", "descriptions": [${(descriptions.ifEmpty { arrayOf("LP", "Compilation") }).joinToString(",") { "\"$it\"" }}]}]}
        ]}
    """.trimIndent()

    private fun artists(vararg names: String) =
        """{"artists": [${names.mapIndexed { i, n -> """{"id": "mb-${n.lowercase().replace(" ", "")}", "name": "$n"}""" }.joinToString(",")}]}"""

    private fun groups(vararg titles: String) =
        """{"release-groups": [${titles.joinToString(",") { """{"id": "rg-${it.lowercase().replace(" ", "")}", "title": "$it", "primary-type": "Album", "secondary-types": ["Compilation"], "first-release-date": "2025-10-31"}""" }}]}"""

    private fun paths(count: Int) = (1..count).map { server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath }

    @Test
    fun `asks Discogs when MusicBrainz has no release with the barcode`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody()))
        server.enqueue(MockResponse().setBody(artists("Adam & The Ants")))
        server.enqueue(MockResponse().setBody(groups("The Singles")))

        val result = viewModel.lookUp(barcode) as BarcodeLookup.Found

        val match = result.matches.single()
        assertEquals("mb-adam&theants", match.artistId)
        assertEquals("Adam & The Ants", match.artistName)
        assertEquals("rg-thesingles", match.releaseGroupId)
        assertEquals("The Singles", match.albumTitle)
        assertEquals(listOf("LP"), match.formats)
        assertFalse(match.byHand)
        assertTrue(match.fromDiscogs)
        assertEquals(listOf("/release/", "/database/search", "/artist/", "/release-group/"), paths(4))
    }

    @Test
    fun `sends the barcode to Discogs and the names to MusicBrainz`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody()))
        server.enqueue(MockResponse().setBody(artists("Adam & The Ants")))
        server.enqueue(MockResponse().setBody(groups("The Singles")))

        viewModel.lookUp(barcode)

        server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals(barcode, server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("barcode"))
        assertEquals("artist:\"Adam & The Ants\"", server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("query"))
        val groupQuery = server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("query")!!
        assertTrue(groupQuery, groupQuery.contains("releasegroup:\"The Singles\"") && groupQuery.contains("arid:mb-adam&theants"))
    }

    @Test
    fun `MusicBrainz knowing the barcode means Discogs is not asked`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"releases": [{"id": "r1", "title": "Avalon", "artist-credit": [{"name": "Roxy Music", "artist": {"id": "roxy", "name": "Roxy Music"}}], "release-group": {"id": "rg1", "title": "Avalon", "primary-type": "Album"}}]}"""))

        val result = viewModel.lookUp("5012985305020") as BarcodeLookup.Found

        assertFalse(result.matches.single().fromDiscogs)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `an album MusicBrainz does not list is offered to add by hand under the artist`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody()))
        server.enqueue(MockResponse().setBody(artists("Adam & The Ants")))
        server.enqueue(MockResponse().setBody(groups("Some Other Album")))

        val match = (viewModel.lookUp(barcode) as BarcodeLookup.Found).matches.single()

        assertTrue(match.byHand)
        assertEquals("mb-adam&theants", match.artistId)
        assertEquals("The Singles", match.albumTitle)
        assertEquals("2026", match.year)
        assertEquals(DiscographyCategory.COMPILATION, match.category)
        assertEquals(listOf("LP"), match.formats)
    }

    @Test
    fun `an artist MusicBrainz does not know leaves only what Discogs said`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody()))
        server.enqueue(MockResponse().setBody(artists("Adam Ant", "Ants")))

        val result = viewModel.lookUp(barcode) as BarcodeLookup.OnlyOnDiscogs

        assertEquals("Adam & The Ants", result.finds.single().artist)
        assertEquals("The Singles", result.finds.single().album)
    }

    @Test
    fun `the artist is the one whose name matches, not just the first MusicBrainz lists`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody()))
        server.enqueue(MockResponse().setBody(artists("Adam Ant", "Adam and the Ants")))
        server.enqueue(MockResponse().setBody(groups("The Singles")))

        val match = (viewModel.lookUp(barcode) as BarcodeLookup.Found).matches.single()

        assertEquals("mb-adamandtheants", match.artistId)
        assertEquals("Adam and the Ants", match.artistName)
    }

    @Test
    fun `a compilation by Various is looked for under Various Artists, with no artist search`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody("Various - Now 99", "2026", "LP", "Compilation")))
        server.enqueue(MockResponse().setBody(groups("Now 99")))

        val match = (viewModel.lookUp(barcode) as BarcodeLookup.Found).matches.single()

        assertEquals(SpecialArtists.VARIOUS_ID, match.artistId)
        assertEquals("Various Artists", match.artistName)
        assertEquals("rg-now99", match.releaseGroupId)
        assertFalse(match.byHand)
        assertEquals(listOf("/release/", "/database/search", "/release-group/"), paths(3))
    }

    @Test
    fun `a compilation by Various that MusicBrainz does not list is added by hand under Various Artists`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody(discogsBody("Various - Now 99", "2026", "LP", "Compilation")))
        server.enqueue(MockResponse().setBody(groups("Something Else")))

        val match = (viewModel.lookUp(barcode) as BarcodeLookup.Found).matches.single()
        val added = viewModel.addOwned(match, "LP") as BarcodeAdd.Added

        assertTrue(match.byHand)
        assertEquals(SpecialArtists.VARIOUS_ID, database.albumDao().getById(added.albumId)!!.artistId)
        assertEquals("COMPILATION", database.albumDao().getById(added.albumId)!!.category)
        assertNull("Various Artists is not a favourite", database.artistDao().getById(SpecialArtists.VARIOUS_ID))
    }

    @Test
    fun `the status says an album by Various Artists goes in its page, not the favourites`() = runBlocking {
        val match = BarcodeMatch(barcode, SpecialArtists.VARIOUS_ID, "Various Artists", "rg1", "Now 99", listOf("CD"))

        val status = viewModel.statusOf(match)

        assertEquals("Various Artists", status.special)
        assertFalse(status.artistIsFavourite)
        assertNull(viewModel.statusOf(match.copy(artistId = "artist-1")).special)
    }

    @Test
    fun `nothing on Discogs either is NotFound`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setBody("""{"results": []}"""))

        assertEquals(BarcodeLookup.NotFound, viewModel.lookUp(barcode))
    }

    @Test
    fun `a Discogs failure says Discogs could not be asked`() = runBlocking {
        server.enqueue(MockResponse().setBody(mbEmpty))
        server.enqueue(MockResponse().setResponseCode(500))

        val failed = viewModel.lookUp(barcode) as BarcodeLookup.Failed

        assertTrue(failed.message, failed.message.contains("Discogs"))
    }

    @Test
    fun `without Discogs the answer is NotFound as before`() = runBlocking {
        build(withDiscogs = false)
        server.enqueue(MockResponse().setBody(mbEmpty))

        assertEquals(BarcodeLookup.NotFound, viewModel.lookUp(barcode))
        assertEquals(1, server.requestCount)
    }

    // Adding an album found on Discogs that MusicBrainz does not list.

    private val byHand = BarcodeMatch(
        barcode, "mb-ants", "Adam & The Ants", "byhand:adam and ants|singles", "The Singles", listOf("LP"),
        byHand = true, fromDiscogs = true, year = "2026", category = DiscographyCategory.COMPILATION
    )

    @Test
    fun `adding it by hand saves the artist and an album added by hand, owned, with a copy`() = runBlocking {
        val result = viewModel.addOwned(byHand, "LP") as BarcodeAdd.Added

        assertTrue(result.albumId.startsWith("custom/"))
        assertNotNull(database.artistDao().getById("mb-ants"))
        val album = database.albumDao().getById(result.albumId)!!
        assertEquals("The Singles", album.title)
        assertEquals("2026", album.firstReleaseDate)
        assertEquals("COMPILATION", album.category)
        assertTrue(album.owned)
        assertEquals(listOf("LP"), database.albumDao().getCopies(result.albumId).map { it.format })
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `scanning it again finds the album added by hand and adds another copy to it`() = runBlocking {
        val first = viewModel.addOwned(byHand, "LP") as BarcodeAdd.Added

        val status = viewModel.statusOf(byHand)
        val second = viewModel.addOwned(byHand, "CD") as BarcodeAdd.Added

        assertTrue(status.owned)
        assertEquals(listOf("LP"), status.copyFormats)
        assertEquals(first.albumId, second.albumId)
        assertEquals(listOf("LP", "CD"), database.albumDao().getCopies(first.albumId).map { it.format })
        assertEquals(1, database.albumDao().getAlbumsForArtist("mb-ants").size)
    }

    @Test
    fun `an album not added yet is new in the status`() = runBlocking {
        assertEquals(BarcodeStatus(), viewModel.statusOf(byHand))
        assertNull(database.albumDao().getById(byHand.releaseGroupId))
    }
}
