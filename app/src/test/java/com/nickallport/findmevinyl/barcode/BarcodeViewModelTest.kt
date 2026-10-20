package com.nickallport.findmevinyl.barcode

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumCopyEntity
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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

@RunWith(RobolectricTestRunner::class)
class BarcodeViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: BarcodeViewModel

    private val cdBarcode = "5012985305020"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MusicBrainzApi::class.java)
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 0)
        val discography = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(), throttle = throttle, groupDao = database.artistGroupDao()
        )
        viewModel = BarcodeViewModel(api, database.albumDao(), database.artistDao(), database.artistGroupDao(), discography, throttle)
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private fun release(
        id: String, groupId: String = "rg1", title: String = "Avalon", artistId: String? = "roxy",
        artist: String = "Roxy Music", format: String? = "CD", primary: String = "Album"
    ): String {
        val credit = if (artistId == null) "" else """"artist-credit": [{"name": "$artist", "artist": {"id": "$artistId", "name": "$artist"}}],"""
        val media = if (format == null) "" else """, "media": [{"format": "$format"}]"""
        return """{"id": "$id", "title": "$title", $credit "release-group": {"id": "$groupId", "title": "$title", "primary-type": "$primary"}$media}"""
    }

    private fun searchBody(vararg releases: String) = """{"count": ${releases.size}, "releases": [${releases.joinToString(",")}]}"""

    private val groupBody = """{"id": "rg1", "title": "Avalon", "primary-type": "Album", "secondary-types": [], "first-release-date": "1982-05-31"}"""

    private suspend fun giveCopies(albumId: String, vararg formats: String) {
        database.albumDao().insertCopies(formats.mapIndexed { i, f -> AlbumCopyEntity(albumId = albumId, format = f, note = null, position = i) })
    }

    private fun saved(owned: Boolean = false, wanted: Boolean = false, wantFormat: String? = null, formats: String? = null) =
        AlbumEntity("rg1", "roxy", "Avalon", "STUDIO_ALBUM", "1982", owned = owned, wanted = wanted, wantFormat = wantFormat, formats = formats)

    // Looking up.

    @Test
    fun `a barcode that is not valid is not looked up`() = runBlocking {
        assertEquals(BarcodeLookup.BadNumber, viewModel.lookUp("12345"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `finds the album, its artist and its format`() = runBlocking {
        server.enqueue(MockResponse().setBody(searchBody(release("r1"))))

        val result = viewModel.lookUp(cdBarcode) as BarcodeLookup.Found

        assertEquals(BarcodeMatch(cdBarcode, "roxy", "Roxy Music", "rg1", "Avalon", listOf("CD")), result.matches.single())
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("barcode:$cdBarcode", request.requestUrl!!.queryParameter("query"))
    }

    @Test
    fun `several releases of one album are one match, with every format`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                searchBody(release("r1", format = "CD"), release("r2", format = "12\\\" Vinyl"), release("r3", format = "CD"))
            )
        )

        val match = (viewModel.lookUp(cdBarcode) as BarcodeLookup.Found).matches.single()

        assertEquals(listOf("LP", "CD"), match.formats)
    }

    @Test
    fun `different albums with the same barcode are separate matches`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                searchBody(
                    release("r1", groupId = "rg1", title = "One"),
                    release("r2", groupId = "rg2", title = "Two", artistId = "other", artist = "Other")
                )
            )
        )

        val matches = (viewModel.lookUp(cdBarcode) as BarcodeLookup.Found).matches

        assertEquals(listOf("One", "Two"), matches.map { it.albumTitle })
    }

    @Test
    fun `a release with no artist or no release group is left out`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                searchBody(
                    release("r1", artistId = null),
                    """{"id": "r2", "title": "X", "artist-credit": [{"name": "A", "artist": {"id": "a", "name": "A"}}]}"""
                )
            )
        )

        assertEquals(BarcodeLookup.NotFound, viewModel.lookUp(cdBarcode))
    }

    @Test
    fun `a twelve inch single is a 12 inch`() = runBlocking {
        server.enqueue(MockResponse().setBody(searchBody(release("r1", format = "12\\\" Vinyl", primary = "Single"))))

        val match = (viewModel.lookUp(cdBarcode) as BarcodeLookup.Found).matches.single()

        assertEquals(listOf("12\""), match.formats)
    }

    @Test
    fun `no release with the barcode is NotFound`() = runBlocking {
        server.enqueue(MockResponse().setBody(searchBody()))

        assertEquals(BarcodeLookup.NotFound, viewModel.lookUp(cdBarcode))
    }

    @Test
    fun `a failed request is Failed, and a busy answer says so`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(500))

        val busy = viewModel.lookUp(cdBarcode) as BarcodeLookup.Failed
        val other = viewModel.lookUp(cdBarcode) as BarcodeLookup.Failed

        assertTrue(busy.message.contains("busy"))
        assertTrue(other.message.isNotBlank())
    }

    // What the app already has.

    private val match = BarcodeMatch(cdBarcode, "roxy", "Roxy Music", "rg1", "Avalon", listOf("CD"))

    @Test
    fun `an artist and album that are not saved are new`() = runBlocking {
        assertEquals(BarcodeStatus(), viewModel.statusOf(match))
    }

    @Test
    fun `says when the artist is a favourite, and when it is in a group`() = runBlocking {
        database.artistDao().insert(ArtistEntity("roxy", "Roxy Music", null, null, 0))
        database.artistGroupDao().insertGroup(ArtistGroupEntity("group/1", "Roxy and Ferry", 0))
        database.artistGroupDao().insertMembers(listOf(ArtistGroupMemberEntity("roxy", "group/1", "Roxy Music", 0)))

        val status = viewModel.statusOf(match)

        assertTrue(status.artistIsFavourite)
        assertEquals("Roxy and Ferry", status.groupName)
    }

    @Test
    fun `says what the album is, saved, owned, wanted, and the copies it has`() = runBlocking {
        database.albumDao().insert(saved(owned = true, wanted = true, wantFormat = "LP"))
        giveCopies("rg1", "CD", "CD")

        val status = viewModel.statusOf(match)

        assertTrue(status.albumSaved)
        assertTrue(status.owned)
        assertTrue(status.wanted)
        assertEquals("LP", status.wantFormat)
        assertEquals(listOf("CD", "CD"), status.copyFormats)
    }

    // Adding.

    @Test
    fun `a new artist and album are saved, owned, with a copy on the format`() = runBlocking {
        server.enqueue(MockResponse().setBody(groupBody))

        val result = viewModel.addOwned(match, "CD")

        assertEquals(BarcodeAdd.Added("rg1"), result)
        assertNotNull(database.artistDao().getById("roxy"))
        val album = database.albumDao().getById("rg1")!!
        assertTrue(album.owned)
        assertEquals("Avalon", album.title)
        assertEquals("STUDIO_ALBUM", album.category)
        assertEquals("1982-05-31", album.firstReleaseDate)
        assertNull(album.formats)
        assertEquals(listOf("CD"), database.albumDao().getCopies("rg1").map { it.format })
        assertEquals("/release-group/rg1", server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
    }

    @Test
    fun `a saved album is not fetched again, and keeps what the user has set`() = runBlocking {
        database.artistDao().insert(ArtistEntity("roxy", "Roxy Music", null, null, 7))
        database.albumDao().insert(saved(formats = "LP"))

        val result = viewModel.addOwned(match, "CD")

        assertEquals(BarcodeAdd.Added("rg1"), result)
        assertEquals(0, server.requestCount)
        assertEquals(7, database.artistDao().getById("roxy")!!.position)
        assertTrue(database.albumDao().getById("rg1")!!.owned)
        assertEquals("LP", database.albumDao().getById("rg1")!!.formats)
    }

    @Test
    fun `an album already owned gets one more copy and keeps its others`() = runBlocking {
        database.artistDao().insert(ArtistEntity("roxy", "Roxy Music", null, null, 0))
        database.albumDao().insert(saved(owned = true))
        giveCopies("rg1", "LP")

        viewModel.addOwned(match, "CD")

        assertEquals(listOf("LP", "CD"), database.albumDao().getCopies("rg1").map { it.format })
    }

    @Test
    fun `a want on the same format is taken off, on another format it stays`() = runBlocking {
        database.artistDao().insert(ArtistEntity("roxy", "Roxy Music", null, null, 0))
        database.albumDao().insert(saved(wanted = true, wantFormat = "LP"))

        viewModel.addOwned(match, "CD")
        assertTrue(database.albumDao().getById("rg1")!!.wanted)

        viewModel.addOwned(match, "LP")
        assertFalse(database.albumDao().getById("rg1")!!.wanted)
    }

    @Test
    fun `a want on any format is taken off only when asked`() = runBlocking {
        database.artistDao().insert(ArtistEntity("roxy", "Roxy Music", null, null, 0))
        database.albumDao().insert(saved(wanted = true))

        viewModel.addOwned(match, "CD")
        assertTrue(database.albumDao().getById("rg1")!!.wanted)

        viewModel.addOwned(match, "CD", takeOffWant = true)
        assertFalse(database.albumDao().getById("rg1")!!.wanted)
    }

    @Test
    fun `when the album cannot be fetched nothing is saved`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = viewModel.addOwned(match, "CD")

        assertTrue(result is BarcodeAdd.Failed)
        assertNull(database.artistDao().getById("roxy"))
        assertNull(database.albumDao().getById("rg1"))
    }

    @Test
    fun `a format that is not a label is refused`() = runBlocking {
        val result = viewModel.addOwned(match, "DVD")

        assertTrue(result is BarcodeAdd.Failed)
        assertNull(database.albumDao().getById("rg1"))
    }
}
