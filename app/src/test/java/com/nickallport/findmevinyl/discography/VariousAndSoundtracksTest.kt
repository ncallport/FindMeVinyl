package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.favourites.FavouritesViewModel
import com.nickallport.findmevinyl.favourites.SpecialArtists
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import com.nickallport.findmevinyl.wantlist.WantListViewModel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * The Various Artists page (albums credited to the special artist Various Artists) and the Soundtracks page (every soundtrack
 * the user owns or wants, by whoever it is by). Both are only what is saved: nothing is loaded from MusicBrainz for them.
 */
@RunWith(RobolectricTestRunner::class)
class VariousAndSoundtracksTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    private val various = SpecialArtists.VARIOUS_ID
    private val soundtracks = SpecialArtists.SOUNDTRACKS_ID

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(), throttle = MusicBrainzThrottle(minIntervalMillis = 0),
            clock = { 1_800_000_000_000L }, groupDao = database.artistGroupDao()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private suspend fun album(
        id: String, artistId: String, title: String = "Title $id", category: String = "STUDIO_ALBUM",
        owned: Boolean = false, wanted: Boolean = false, date: String? = "1987"
    ) = database.albumDao().insert(AlbumEntity(id, artistId, title, category, date, owned = owned, wanted = wanted))

    private suspend fun ids(page: String) = viewModel.loadSaved(page).groupedAlbums.values.flatten().map { it.id }

    // The names.

    @Test
    fun `the two special artists are known by id and name`() {
        assertTrue(SpecialArtists.isSpecial(various))
        assertTrue(SpecialArtists.isSpecial(soundtracks))
        assertFalse(SpecialArtists.isSpecial("artist-1"))
        assertFalse(SpecialArtists.isSpecial("group/1"))
        assertEquals("Various Artists", SpecialArtists.nameOf(various))
        assertEquals("Soundtracks", SpecialArtists.nameOf(soundtracks))
        assertNull(SpecialArtists.nameOf("artist-1"))
    }

    @Test
    fun `an album added to the Soundtracks page is filed under Various Artists`() {
        assertEquals(various, SpecialArtists.ownerOf(soundtracks))
        assertEquals(various, SpecialArtists.ownerOf(various))
        assertEquals("artist-1", SpecialArtists.ownerOf("artist-1"))
    }

    // The Various Artists page.

    @Test
    fun `the Various Artists page lists the albums filed under it, by kind`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", owned = true)
        album("now2", various, "Now 2", category = "COMPILATION", wanted = true)
        album("other", "artist-1", owned = true)

        val page = viewModel.loadSaved(various)

        assertEquals(listOf("now1", "now2"), page.groupedAlbums[DiscographyCategory.COMPILATION]!!.map { it.id })
        assertEquals(setOf(DiscographyCategory.COMPILATION), page.groupedAlbums.keys)
    }

    @Test
    fun `each row on the Various Artists page names who it is by`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", owned = true)

        assertEquals(listOf("Various Artists"), viewModel.loadSaved(various).groupedAlbums.values.flatten().map { it.artistName })
    }

    // The Soundtracks page.

    @Test
    fun `the Soundtracks page lists every soundtrack owned or wanted, whoever it is by`() = runBlocking {
        database.artistDao().insert(ArtistEntity("madonna", "Madonna", null, null, 0))
        album("wtg", "madonna", "Who's That Girl", category = "SOUNDTRACK", wanted = true)
        album("pulp", various, "Pulp Fiction", category = "SOUNDTRACK", owned = true)
        album("hits", "madonna", "Greatest Hits", category = "COMPILATION", owned = true)
        album("unowned", "madonna", "Evita", category = "SOUNDTRACK")

        assertEquals(setOf("wtg", "pulp"), ids(soundtracks).toSet())
        assertEquals(listOf(DiscographyCategory.SOUNDTRACK), viewModel.loadSaved(soundtracks).groupedAlbums.keys.toList())
    }

    @Test
    fun `a soundtrack added by hand and owned is on the Soundtracks page`() = runBlocking {
        val id = viewModel.addAlbum(
            soundtracks, "Soundtracks", AlbumDraft("Blade Runner", "1982", DiscographyCategory.SOUNDTRACK, bootleg = false)
        )!!

        assertEquals(listOf(id), ids(soundtracks))
    }

    @Test
    fun `each row on the Soundtracks page names the artist it is filed under`() = runBlocking {
        database.artistDao().insert(ArtistEntity("madonna", "Madonna", null, null, 0))
        database.artistGroupDao().insertGroup(ArtistGroupEntity("group/1", "Roxy and Ferry", 0))
        database.artistGroupDao().insertMembers(listOf(ArtistGroupMemberEntity("ferry", "group/1", "Bryan Ferry", 0), ArtistGroupMemberEntity("roxy", "group/1", "Roxy Music", 1)))
        album("wtg", "madonna", "Who's That Girl", category = "SOUNDTRACK", owned = true)
        album("pulp", various, "Pulp Fiction", category = "SOUNDTRACK", owned = true)
        album("ferry", "ferry", "Some Film", category = "SOUNDTRACK", owned = true)

        val names = viewModel.loadSaved(soundtracks).groupedAlbums.values.flatten().associate { it.id to it.artistName }

        assertEquals(mapOf("wtg" to "Madonna", "pulp" to "Various Artists", "ferry" to "Bryan Ferry"), names)
    }

    @Test
    fun `an album's own artist is found from the album, for opening it from a special page`() = runBlocking {
        database.artistDao().insert(ArtistEntity("madonna", "Madonna", null, null, 0))
        album("wtg", "madonna", "Who's That Girl", category = "SOUNDTRACK", owned = true)
        album("pulp", various, "Pulp Fiction", category = "SOUNDTRACK", owned = true)

        assertEquals("madonna" to "Madonna", viewModel.artistOfAlbum("wtg"))
        assertEquals(various to "Various Artists", viewModel.artistOfAlbum("pulp"))
        assertNull(viewModel.artistOfAlbum("nothing"))
    }

    // Nothing is asked of MusicBrainz, and nothing becomes a favourite.

    @Test
    fun `opening a special page makes no request, and gives the saved albums`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", owned = true)

        val state = viewModel.discography(various).last() as DiscographyUiState.Success

        assertEquals(0, server.requestCount)
        assertFalse(state.loadingMore)
        assertEquals(listOf("now1"), state.groupedAlbums.values.flatten().map { it.id })
    }

    @Test
    fun `an empty special page is an empty list, not an error`() = runBlocking {
        val state = viewModel.discography(soundtracks).last()

        assertEquals(DiscographyUiState.Success(emptyMap()), state)
    }

    @Test
    fun `the whole-artist check of a special page does nothing`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", owned = true)

        assertEquals(FormatCheckResult.Done(0), viewModel.checkAllFormats(various))
        assertEquals(FormatCheckResult.Done(0), viewModel.checkAllFormats(soundtracks))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a special page is always kept, and saving or removing it changes nothing`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", wanted = true)

        assertTrue(viewModel.isFavourite(various))
        assertTrue(viewModel.isFavourite(soundtracks))
        viewModel.saveFavourite(various, "Various Artists")
        viewModel.removeFavourite(various)

        assertNull(database.artistDao().getById(various))
        assertTrue(database.albumDao().getById("now1")!!.wanted)
    }

    @Test
    fun `owning an album on the Various Artists page does not make Various Artists a favourite`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION")

        viewModel.toggleOwned("now1", false, various, "Various Artists")

        assertTrue(database.albumDao().getById("now1")!!.owned)
        assertNull(database.artistDao().getById(various))
    }

    @Test
    fun `an album added by hand to a special page is owned, filed under Various Artists, with no favourite`() = runBlocking {
        val id = viewModel.addAlbum(various, "Various Artists", AlbumDraft("Now 99", "2018", DiscographyCategory.COMPILATION, bootleg = false))!!

        assertEquals(various, database.albumDao().getById(id)!!.artistId)
        assertTrue(database.albumDao().getById(id)!!.owned)
        assertEquals(listOf("Various Artists"), viewModel.loadSaved(various).groupedAlbums.values.flatten().map { it.artistName })
        assertNull(database.artistDao().getById(various))
    }

    // The two blocks on the Artists page.

    @Test
    fun `the blocks count what is owned and wanted`() = runBlocking {
        database.artistDao().insert(ArtistEntity("madonna", "Madonna", null, null, 0))
        album("now1", various, category = "COMPILATION", owned = true)
        album("now2", various, category = "COMPILATION", owned = true, wanted = true)
        album("now3", various, category = "COMPILATION")
        album("wtg", "madonna", category = "SOUNDTRACK", wanted = true)
        album("pulp", various, category = "SOUNDTRACK", owned = true)
        album("hits", "madonna", category = "COMPILATION", owned = true)

        val blocks = FavouritesViewModel(database.artistDao(), database.artistGroupDao(), database.albumDao()).loadSpecials()

        assertEquals(listOf(various, soundtracks), blocks.map { it.id })
        assertEquals(listOf("Various Artists", "Soundtracks"), blocks.map { it.name })
        // Various Artists: its own albums, whatever their kind. Soundtracks: the soundtracks owned or wanted, of any artist.
        assertEquals(listOf(3 to 1, 1 to 1), blocks.map { it.ownedCount to it.wantedCount })
    }

    @Test
    fun `the blocks are there with no albums at all`() = runBlocking {
        val blocks = FavouritesViewModel(database.artistDao(), database.artistGroupDao(), database.albumDao()).loadSpecials()

        assertEquals(listOf(various, soundtracks), blocks.map { it.id })
        assertEquals(listOf(0 to 0, 0 to 0), blocks.map { it.ownedCount to it.wantedCount })
    }

    @Test
    fun `the special artists are not in the favourites list`() = runBlocking {
        album("now1", various, category = "COMPILATION", owned = true)

        val favourites = FavouritesViewModel(database.artistDao(), database.artistGroupDao(), database.albumDao()).loadFavourites()

        assertEquals(emptyList<String>(), favourites.map { it.id })
    }

    // The Wants list.

    @Test
    fun `a Various Artists album wanted is on the Wants list under Various Artists`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", wanted = true)

        val list = WantListViewModel(database.albumDao()).loadWantList()

        assertEquals(listOf("Various Artists" to "Now 1"), list.map { it.artistName to it.albumTitle })
    }

    @Test
    fun `the shared text says Various Artists, as the album is`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", wanted = true)

        val text = WantListViewModel(database.albumDao()).exportText()

        assertTrue(text, text.contains("1. Various Artists - Now 1"))
    }

    @Test
    fun `a copy added to an album on a special page works as anywhere`() = runBlocking {
        album("now1", various, "Now 1", category = "COMPILATION", owned = true)

        viewModel.saveCopies("now1", listOf(AlbumCopy("LP", "gatefold")))

        assertEquals(listOf(AlbumCopy("LP", "gatefold")), viewModel.loadAlbum("now1")!!.copies)
    }
}
