package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumCopyEntity
import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
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

/** An album the user adds by hand, because MusicBrainz does not list it (most of their bootlegs, say). */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelAddAlbumTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            throttle = MusicBrainzThrottle(minIntervalMillis = 0), clock = { 1_800_000_000_000L },
            groupDao = database.artistGroupDao()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private val live = AlbumDraft("Live in a Pub", "1983", DiscographyCategory.LIVE_ALBUM, bootleg = true)

    private suspend fun stored(id: String) = database.albumDao().getById(id)!!

    @Test
    fun `an album added by hand is saved as owned, with an id of its own, under the artist`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!

        assertTrue(id.startsWith("custom/"))
        val saved = stored(id)
        assertEquals("artist-1", saved.artistId)
        assertEquals("Live in a Pub", saved.title)
        assertEquals("LIVE_ALBUM", saved.category)
        assertEquals("1983", saved.firstReleaseDate)
        assertTrue(saved.owned)
        assertFalse(saved.wanted)
    }

    @Test
    fun `two albums added get different ids`() = runBlocking {
        val first = viewModel.addAlbum("artist-1", "Big Country", live)
        val second = viewModel.addAlbum("artist-1", "Big Country", live)

        assertTrue(first != second)
    }

    @Test
    fun `it shows in the discography under its kind, with the bootleg status, and is not looked up`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!

        val album = viewModel.loadSaved("artist-1").groupedAlbums.getValue(DiscographyCategory.LIVE_ALBUM).single()

        assertEquals(id, album.id)
        assertTrue(album.owned)
        // Looked at already, so nothing asks MusicBrainz about it.
        assertEquals(emptyList<String>(), album.formats)
        assertEquals(false, album.formatsStale)
        assertEquals(listOf("Bootleg"), album.statuses)
    }

    @Test
    fun `without the bootleg tick it is an official release`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live.copy(bootleg = false))!!

        assertEquals("Official", stored(id).statuses)
        assertEquals(listOf("Official"), viewModel.loadAlbum(id)!!.statuses)
    }

    @Test
    fun `adding an album makes the artist a favourite, as owning an album does`() = runBlocking {
        viewModel.addAlbum("artist-1", "Big Country", live)

        assertEquals("Big Country", database.artistDao().getById("artist-1")?.name)
    }

    @Test
    fun `the title is trimmed, and a blank title adds nothing`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live.copy(title = "  Live in a Pub  "))!!

        assertEquals("Live in a Pub", stored(id).title)
        assertNull(viewModel.addAlbum("artist-1", "Big Country", live.copy(title = "   ")))
        assertEquals(1, database.albumDao().getAlbumsForArtist("artist-1").size)
    }

    @Test
    fun `a title is cut to a sensible length`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live.copy(title = "x".repeat(500)))!!

        assertEquals(200, stored(id).title.length)
    }

    @Test
    fun `the year is optional, and must be four digits when given`() = runBlocking {
        val none = viewModel.addAlbum("artist-1", "Big Country", live.copy(year = ""))!!
        assertNull(stored(none).firstReleaseDate)

        assertNull(viewModel.addAlbum("artist-1", "Big Country", live.copy(year = "83")))
        assertNull(viewModel.addAlbum("artist-1", "Big Country", live.copy(year = "nineteen")))
        assertNull(viewModel.addAlbum("artist-1", "Big Country", live.copy(year = "3024")))
        assertEquals(1, database.albumDao().getAlbumsForArtist("artist-1").size)
    }

    @Test
    fun `an album added to a group is filed under the first member, and shows on the group's page`() = runBlocking {
        database.artistGroupDao().insertGroup(ArtistGroupEntity("group/1", "Nick Cave, all of it", 0))
        database.artistGroupDao().insertMembers(
            listOf(ArtistGroupMemberEntity("cave", "group/1", "Nick Cave", 0), ArtistGroupMemberEntity("seeds", "group/1", "The Bad Seeds", 1))
        )

        val id = viewModel.addAlbum("group/1", "Nick Cave, all of it", live)!!

        assertEquals("cave", stored(id).artistId)
        assertEquals(listOf(id), viewModel.loadSaved("group/1").groupedAlbums.values.flatten().map { it.id })
        assertNull(database.artistDao().getById("group/1"))
    }

    @Test
    fun `the draft of an album added by hand can be read back to change it`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!

        assertEquals(live, viewModel.loadDraft(id))
    }

    @Test
    fun `an album from MusicBrainz has no draft, so it cannot be changed or removed`() = runBlocking {
        database.albumDao().insert(AlbumEntity("mb-1", "artist-1", "Steeltown", "STUDIO_ALBUM", "1984", owned = true, wanted = false))

        assertNull(viewModel.loadDraft("mb-1"))
        assertFalse(viewModel.updateAlbum("mb-1", live))
        assertFalse(viewModel.deleteAlbum("mb-1"))
        assertEquals("Steeltown", stored("mb-1").title)
    }

    @Test
    fun `changing an album keeps whether it is owned or wanted, and its copies`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!
        viewModel.saveCopies(id, listOf(AlbumCopy("LP", "green vinyl")))
        database.albumDao().setWanted(id, true)

        assertTrue(viewModel.updateAlbum(id, AlbumDraft("Live at the Pub", "1984", DiscographyCategory.OTHER, bootleg = false)))

        val saved = stored(id)
        assertEquals("Live at the Pub", saved.title)
        assertEquals("1984", saved.firstReleaseDate)
        assertEquals("OTHER", saved.category)
        assertEquals("Official", saved.statuses)
        assertTrue(saved.owned)
        assertTrue(saved.wanted)
        assertEquals(listOf(AlbumCopy("LP", "green vinyl")), viewModel.loadAlbum(id)!!.copies)
    }

    @Test
    fun `changing an album with a blank title or a bad year changes nothing`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!

        assertFalse(viewModel.updateAlbum(id, live.copy(title = " ")))
        assertFalse(viewModel.updateAlbum(id, live.copy(year = "83")))

        assertEquals(live, viewModel.loadDraft(id))
    }

    @Test
    fun `removing an album takes it and its copies away, and nothing else`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!
        viewModel.saveCopies(id, listOf(AlbumCopy("LP")))
        database.albumDao().insert(AlbumEntity("mb-1", "artist-1", "Steeltown", "STUDIO_ALBUM", "1984", owned = true, wanted = false))
        viewModel.saveCopies("mb-1", listOf(AlbumCopy("CD")))

        assertTrue(viewModel.deleteAlbum(id))

        assertNull(database.albumDao().getById(id))
        assertEquals(emptyList<AlbumCopyEntity>(), database.albumDao().getCopies(id))
        assertEquals(listOf("CD"), database.albumDao().getCopies("mb-1").map { it.format })
        assertNotNull(database.albumDao().getById("mb-1"))
    }

    @Test
    fun `an album that is not there cannot be changed or removed`() = runBlocking {
        assertFalse(viewModel.updateAlbum("custom/nothing", live))
        assertFalse(viewModel.deleteAlbum("custom/nothing"))
        assertNull(viewModel.loadDraft("custom/nothing"))
    }

    @Test
    fun `a lookup of the formats of an album added by hand asks MusicBrainz nothing`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!
        server.enqueue(MockResponse().setBody("{ \"releases\": [] }"))

        assertEquals(emptyList<String>(), viewModel.loadFormats(id))

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `the whole-artist check leaves an album added by hand alone`() = runBlocking {
        val id = viewModel.addAlbum("artist-1", "Big Country", live)!!
        database.albumDao().insert(AlbumEntity("mb-1", "artist-1", "Steeltown", "STUDIO_ALBUM", "1984", owned = false, wanted = false))
        server.enqueue(
            MockResponse().setBody(
                """{ "release-count": 1, "releases": [ { "id": "r", "release-group": { "id": "mb-1" }, "status": "Official", "media": [ { "format": "CD" } ] } ] }"""
            )
        )

        viewModel.checkAllFormats("artist-1")

        val saved = stored(id)
        assertEquals("", saved.formats)
        assertEquals("Bootleg", saved.statuses)
        assertNull(saved.formatsCheckedAt)
    }
}
