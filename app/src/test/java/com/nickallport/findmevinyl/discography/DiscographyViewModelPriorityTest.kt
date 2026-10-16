package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.favourites.FavouritesViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** A new favourite goes first and a new want goes last in the user's own order, and saving again changes nothing. */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelPriorityTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            throttle = MusicBrainzThrottle(minIntervalMillis = 0)
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private suspend fun album(id: String, artistId: String = "g") = database.albumDao().insert(
        AlbumEntity(id, artistId, "Album $id", "STUDIO_ALBUM", null, owned = false, wanted = false)
    )

    private fun position(artistId: String) = runBlocking { database.artistDao().getById(artistId)?.position }

    private fun wantPosition(albumId: String) = runBlocking { database.albumDao().getById(albumId)?.wantPosition }

    @Test
    fun `the first favourite gets position 0 and each new one goes before the first`() = runBlocking {
        viewModel.saveFavourite("g", "Genesis")
        viewModel.saveFavourite("r", "Ramones")
        viewModel.saveFavourite("x", "Roxy Music")

        assertEquals(0, position("g"))
        assertEquals(-1, position("r"))
        assertEquals(-2, position("x"))
    }

    @Test
    fun `a new favourite is first in the list the Artists page shows`() = runBlocking {
        viewModel.saveFavourite("g", "Genesis")
        viewModel.saveFavourite("r", "Ramones")

        viewModel.saveFavourite("x", "Roxy Music")

        val names = FavouritesViewModel(database.artistDao()).loadFavourites().map { it.name }
        assertEquals(listOf("Roxy Music", "Ramones", "Genesis"), names)
    }

    @Test
    fun `a new favourite is first even when the others were never ordered and all sit at 0`() = runBlocking {
        database.artistDao().insert(ArtistEntity("a", "Zed", null, null, position = 0))
        database.artistDao().insert(ArtistEntity("b", "Abba", null, null, position = 0))

        viewModel.saveFavourite("x", "Roxy Music")

        val names = FavouritesViewModel(database.artistDao()).loadFavourites().map { it.name }
        assertEquals(listOf("Roxy Music", "Abba", "Zed"), names)
    }

    @Test
    fun `a new favourite is first after the others were moved, which rewrites their positions`() = runBlocking {
        viewModel.saveFavourite("g", "Genesis")
        viewModel.saveFavourite("r", "Ramones")
        val favourites = FavouritesViewModel(database.artistDao())
        favourites.moveDown("r")

        viewModel.saveFavourite("x", "Roxy Music")

        assertEquals(listOf("Roxy Music", "Genesis", "Ramones"), favourites.loadFavourites().map { it.name })
    }

    @Test
    fun `saving an artist that is already a favourite keeps its place`() = runBlocking {
        viewModel.saveFavourite("g", "Genesis")
        viewModel.saveFavourite("r", "Ramones")
        database.artistDao().setPosition("g", 7)

        viewModel.saveFavourite("g", "Genesis")

        assertEquals(7, position("g"))
        assertEquals(-1, position("r"))
    }

    @Test
    fun `wanting an album puts it last among that artist's wanted albums`() = runBlocking {
        album("1"); album("2"); album("3")

        viewModel.toggleWanted("2", currentlyWanted = false, artistId = "g", artistName = "Genesis")
        viewModel.toggleWanted("1", currentlyWanted = false, artistId = "g", artistName = "Genesis")
        viewModel.toggleWanted("3", currentlyWanted = false, artistId = "g", artistName = "Genesis")

        assertEquals(0, wantPosition("2"))
        assertEquals(1, wantPosition("1"))
        assertEquals(2, wantPosition("3"))
    }

    @Test
    fun `the want order of one artist does not depend on another artist`() = runBlocking {
        album("1", "g"); album("2", "r")

        viewModel.toggleWanted("1", false, "g", "Genesis")
        viewModel.toggleWanted("2", false, "r", "Ramones")

        assertEquals(0, wantPosition("1"))
        assertEquals(0, wantPosition("2"))
    }

    @Test
    fun `an album wanted again after being dropped goes to the end`() = runBlocking {
        album("1"); album("2")
        viewModel.toggleWanted("1", false, "g", "Genesis")
        viewModel.toggleWanted("2", false, "g", "Genesis")

        viewModel.toggleWanted("1", currentlyWanted = true, artistId = "g", artistName = "Genesis")
        viewModel.toggleWanted("1", currentlyWanted = false, artistId = "g", artistName = "Genesis")

        assertEquals(2, wantPosition("1"))
        assertEquals(1, wantPosition("2"))
    }

    @Test
    fun `wanting an album that is already wanted does not move it`() = runBlocking {
        album("1"); album("2")
        viewModel.toggleWanted("1", false, "g", "Genesis")
        viewModel.toggleWanted("2", false, "g", "Genesis")

        database.albumDao().markWanted("1")

        assertEquals(0, wantPosition("1"))
    }
}
