package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.favourites.SpecialArtists
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AlbumTrackArtistEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * A Various Artists album the user owns or wants is listed on the page of each artist on its tracklist, as Spotify's
 * "Appears On" does. The track artists are read once for each such album and kept.
 */
@RunWith(RobolectricTestRunner::class)
class AppearsOnTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var api: MusicBrainzApi

    private val various = SpecialArtists.VARIOUS_ID

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MusicBrainzApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private suspend fun album(
        id: String, artistId: String = various, owned: Boolean = true, wanted: Boolean = false,
        checked: Boolean = false, category: String = "COMPILATION"
    ) = database.albumDao().insert(
        AlbumEntity(id, artistId, "Title $id", category, "1983", owned = owned, wanted = wanted, trackArtistsChecked = checked)
    )

    private fun release(vararg artistIds: String) =
        """{ "id": "r1", "media": [ { "tracks": [ ${artistIds.joinToString(",") { """{ "title": "T", "artist-credit": [ { "name": "N", "artist": { "id": "$it", "name": "N" } } ] }""" }} ] } ] }"""

    private fun releases(vararg rows: String) = """{ "release-count": ${rows.size}, "releases": [ ${rows.joinToString(",")} ] }"""

    private fun checker() = AppearsOnChecker(api, database.albumDao(), MusicBrainzThrottle(minIntervalMillis = 0))

    // Reading the track artists.

    @Test
    fun `an owned Various Artists album has the artists of its tracks read and kept, once`() = runBlocking {
        album("now1")
        server.enqueue(MockResponse().setBody(releases(release("phil", "duran", "phil", "ub40"))))

        checker().run()

        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/release/", request.requestUrl!!.encodedPath)
        assertEquals("now1", request.requestUrl!!.queryParameter("release-group"))
        assertEquals("official", request.requestUrl!!.queryParameter("status"))
        // Sent as it is, so MusicBrainz reads the plus signs as spaces.
        assertTrue(request.path!!, request.path!!.contains("inc=recordings+artist-credits"))
        assertEquals("1", request.requestUrl!!.queryParameter("limit"))
        assertEquals(setOf("phil", "duran", "ub40"), database.albumDao().getTrackArtists("now1").toSet())
        assertTrue(database.albumDao().getById("now1")!!.trackArtistsChecked)

        checker().run()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a wanted album is read too, and Various Artists itself is not kept as a track artist`() = runBlocking {
        album("now2", owned = false, wanted = true)
        server.enqueue(MockResponse().setBody(releases(release("phil", various))))

        checker().run()

        assertEquals(listOf("phil"), database.albumDao().getTrackArtists("now2"))
    }

    @Test
    fun `when no official release is listed any release is read`() = runBlocking {
        album("bootleg")
        server.enqueue(MockResponse().setBody(releases()))
        server.enqueue(MockResponse().setBody(releases(release("phil"))))

        checker().run()

        assertEquals("official", server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("status"))
        assertEquals(null, server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("status"))
        assertEquals(listOf("phil"), database.albumDao().getTrackArtists("bootleg"))
    }

    @Test
    fun `an album with no releases at all is marked read, so it is not asked about again`() = runBlocking {
        album("empty")
        server.enqueue(MockResponse().setBody(releases()))
        server.enqueue(MockResponse().setBody(releases()))

        checker().run()
        checker().run()

        assertTrue(database.albumDao().getById("empty")!!.trackArtistsChecked)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a failed request stops the run and changes nothing, so the next run asks again`() = runBlocking {
        album("a")
        album("b")
        server.enqueue(MockResponse().setResponseCode(503))

        checker().run()

        assertFalse(database.albumDao().getById("a")!!.trackArtistsChecked)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `only owned or wanted Various Artists albums not read yet are asked about`() = runBlocking {
        album("mine", artistId = "artist-1")
        album("custom/1")
        album("done", checked = true)
        album("unowned", owned = false)

        checker().run()

        assertEquals(0, server.requestCount)
    }

    // Showing them on the artist's page.

    private fun viewModel() = DiscographyViewModel(
        api, database.albumDao(), database.artistDao(), throttle = MusicBrainzThrottle(minIntervalMillis = 0),
        clock = { 1_800_000_000_000L }, groupDao = database.artistGroupDao(), pageDelayMillis = 0, pageSize = 1
    )

    private suspend fun appears(albumId: String, vararg artists: String) =
        database.albumDao().insertTrackArtists(artists.map { AlbumTrackArtistEntity(albumId, it) })

    private suspend fun appearsOn(page: String) =
        viewModel().loadSaved(page).groupedAlbums[DiscographyCategory.APPEARS_ON].orEmpty().map { it.id }

    @Test
    fun `an artist's page lists the owned Various Artists albums they are on, in a section of their own`() = runBlocking {
        album("now1")
        appears("now1", "phil", "duran")
        album("other")
        appears("other", "someone")

        assertEquals(listOf("now1"), appearsOn("phil"))
        assertEquals(emptyList<String>(), appearsOn("nobody"))
    }

    @Test
    fun `a wanted one is listed, and one neither owned nor wanted is not`() = runBlocking {
        album("wanted", owned = false, wanted = true)
        album("neither", owned = false)
        appears("wanted", "phil")
        appears("neither", "phil")

        assertEquals(listOf("wanted"), appearsOn("phil"))
    }

    @Test
    fun `a row says it is by Various Artists, and keeps its flags and copies`() = runBlocking {
        album("now1")
        appears("now1", "phil")

        val row = viewModel().loadSaved("phil").groupedAlbums[DiscographyCategory.APPEARS_ON]!!.single()

        assertEquals("Various Artists", row.artistName)
        assertTrue(row.owned)
    }

    @Test
    fun `an album filed under the artist is not listed twice`() = runBlocking {
        album("own", artistId = "phil", category = "STUDIO_ALBUM")
        appears("own", "phil")

        val page = viewModel().loadSaved("phil").groupedAlbums

        assertEquals(listOf("own"), page.values.flatten().map { it.id })
        assertEquals(null, page[DiscographyCategory.APPEARS_ON])
    }

    @Test
    fun `a group's page lists what any of its artists appear on`() = runBlocking {
        database.artistGroupDao().insertGroup(ArtistGroupEntity("group/1", "Genesis and Collins", 0))
        database.artistGroupDao().insertMembers(
            listOf(ArtistGroupMemberEntity("genesis", "group/1", "Genesis", 0), ArtistGroupMemberEntity("phil", "group/1", "Phil Collins", 1))
        )
        album("now1")
        appears("now1", "phil", "genesis")

        assertEquals(listOf("now1"), appearsOn("group/1"))
    }

    @Test
    fun `the special pages have no such section`() = runBlocking {
        album("now1")
        appears("now1", "phil")

        assertEquals(emptyList<String>(), appearsOn(various))
        assertEquals(emptyList<String>(), appearsOn(SpecialArtists.SOUNDTRACKS_ID))
    }

    @Test
    fun `those albums do not count towards how many MusicBrainz lists, so paging goes on`() = runBlocking {
        album("now1")
        appears("now1", "phil")
        database.albumDao().insert(AlbumEntity("own1", "phil", "One", "STUDIO_ALBUM", "1980", owned = false, wanted = false))
        server.enqueue(MockResponse().setBody("""{ "release-group-count": 2, "release-groups": [ { "id": "own1", "title": "One", "primary-type": "Album", "secondary-types": [], "first-release-date": "1980" } ] }"""))
        server.enqueue(MockResponse().setBody("""{ "release-group-count": 2, "release-groups": [ { "id": "own2", "title": "Two", "primary-type": "Album", "secondary-types": [], "first-release-date": "1981" } ] }"""))

        viewModel().discography("phil").toList()

        // One album saved and one on show from Various Artists: that is not the two MusicBrainz lists, so the second page is read.
        assertEquals(2, server.requestCount)
        assertTrue(database.albumDao().getById("own2") != null)
    }
}
