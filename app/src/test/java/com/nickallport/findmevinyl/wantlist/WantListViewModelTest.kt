package com.nickallport.findmevinyl.wantlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WantListViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: WantListViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        viewModel = WantListViewModel(database.albumDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun wantedAlbum(id: String, artistId: String, title: String, date: String?) {
        database.albumDao().insert(
            AlbumEntity(id, artistId, title, "STUDIO_ALBUM", date, owned = false, wanted = true)
        )
    }

    @Test
    fun `orders by artist name ignoring case, then release date with undated last`() = runBlocking {
        database.artistDao().insert(ArtistEntity("r", "roxy music", null, null))
        database.artistDao().insert(ArtistEntity("a", "ABC", null, null))
        wantedAlbum("1", "r", "Avalon", "1982-05-31")
        wantedAlbum("2", "r", "Undated", null)
        wantedAlbum("3", "r", "Roxy Music", "1972-06-16")
        wantedAlbum("4", "a", "Lexicon of Love", "1982-06-21")

        val wanted = viewModel.loadWantList()

        assertEquals(
            listOf("Lexicon of Love", "Roxy Music", "Avalon", "Undated"),
            wanted.map { it.albumTitle }
        )
    }

    @Test
    fun `leaves out owned albums even when they are still flagged as wanted`() = runBlocking {
        database.artistDao().insert(ArtistEntity("r", "roxy music", null, null))
        wantedAlbum("1", "r", "Avalon", "1982-05-31")
        database.albumDao().insert(
            AlbumEntity("2", "r", "Roxy Music", "STUDIO_ALBUM", "1972-06-16", owned = true, wanted = true)
        )

        assertEquals(listOf("Avalon"), viewModel.loadWantList().map { it.albumTitle })
        assertEquals(false, viewModel.exportText().contains("Roxy Music"))
    }

    @Test
    fun `returns an empty list when nothing is wanted`() = runBlocking {
        assertEquals(emptyList<Any>(), viewModel.loadWantList())
    }

    @Test
    fun `export text groups wanted albums under their artist`() = runBlocking {
        database.artistDao().insert(ArtistEntity("r", "roxy music", null, null))
        database.artistDao().insert(ArtistEntity("a", "ABC", null, null))
        wantedAlbum("1", "r", "Avalon", "1982-05-31")
        wantedAlbum("2", "r", "Roxy Music", "1972-06-16")
        wantedAlbum("3", "a", "Lexicon of Love", "1982-06-21")

        val expected = """
            My vinyl want list:

            ABC
            - Lexicon of Love

            roxy music
            - Roxy Music
            - Avalon
        """.trimIndent()

        assertEquals(expected, viewModel.exportText())
    }

    private suspend fun artist(id: String, name: String, position: Int = 0) =
        database.artistDao().insert(ArtistEntity(id, name, null, null, position))

    private suspend fun wanted(id: String, artistId: String, title: String, date: String? = null, wantPosition: Int = 0) {
        database.albumDao().insert(
            AlbumEntity(id, artistId, title, "STUDIO_ALBUM", date, owned = false, wanted = true, wantPosition = wantPosition)
        )
    }

    private suspend fun titles() = viewModel.loadWantList().map { it.albumTitle }

    @Test
    fun `lists artists in the order the user chose for favourites, not by name`() = runBlocking {
        artist("a", "ABC", position = 1)
        artist("r", "Roxy Music", position = 0)
        wanted("1", "a", "Lexicon of Love")
        wanted("2", "r", "Avalon")

        assertEquals(listOf("Avalon", "Lexicon of Love"), titles())
    }

    @Test
    fun `lists an artist's albums in the order the user chose, not by release date`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "Avalon", "1982-05-31", wantPosition = 0)
        wanted("2", "r", "Roxy Music", "1972-06-16", wantPosition = 2)
        wanted("3", "r", "Siren", "1975-10-24", wantPosition = 1)

        assertEquals(listOf("Avalon", "Siren", "Roxy Music"), titles())
    }

    @Test
    fun `the share text follows the same order`() = runBlocking {
        artist("a", "ABC", position = 1)
        artist("r", "Roxy Music", position = 0)
        wanted("1", "a", "Lexicon of Love")
        wanted("2", "r", "Avalon", wantPosition = 1)
        wanted("3", "r", "Siren", wantPosition = 0)

        val expected = """
            My vinyl want list:

            Roxy Music
            - Siren
            - Avalon

            ABC
            - Lexicon of Love
        """.trimIndent()

        assertEquals(expected, viewModel.exportText())
    }

    @Test
    fun `moving an album up swaps it with the one above it`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", wantPosition = 0)
        wanted("2", "r", "Two", wantPosition = 1)
        wanted("3", "r", "Three", wantPosition = 2)

        viewModel.moveUp("3")

        assertEquals(listOf("One", "Three", "Two"), titles())
    }

    @Test
    fun `moving an album down swaps it with the one below it`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", wantPosition = 0)
        wanted("2", "r", "Two", wantPosition = 1)

        viewModel.moveDown("1")

        assertEquals(listOf("Two", "One"), titles())
    }

    @Test
    fun `an album never moves past its own artist's albums`() = runBlocking {
        artist("a", "ABC", position = 0)
        artist("r", "Roxy Music", position = 1)
        wanted("1", "a", "Lexicon of Love")
        wanted("2", "r", "Avalon")
        wanted("3", "r", "Siren", wantPosition = 1)

        viewModel.moveUp("2")
        viewModel.moveDown("3")
        viewModel.moveDown("1")

        assertEquals(listOf("Lexicon of Love", "Avalon", "Siren"), titles())
    }

    @Test
    fun `moving does nothing for an album that is not wanted or does not exist`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One")
        database.albumDao().insert(AlbumEntity("9", "r", "Nine", "STUDIO_ALBUM", null, owned = false, wanted = false))

        viewModel.moveUp("9")
        viewModel.moveDown("missing")

        assertEquals(listOf("One"), titles())
    }

    @Test
    fun `the first move works on albums never ordered, starting from the release order`() = runBlocking {
        artist("r", "Roxy Music")
        // All at position 0, as after the upgrade: shown oldest first, undated last.
        wanted("1", "r", "Siren", "1975-10-24")
        wanted("2", "r", "Avalon", "1982-05-31")
        wanted("3", "r", "Roxy Music", "1972-06-16")
        wanted("4", "r", "Undated", null)

        viewModel.moveUp("2")

        assertEquals(listOf("Roxy Music", "Avalon", "Siren", "Undated"), titles())
        val positions = listOf("3", "2", "1", "4").map { database.albumDao().getById(it)!!.wantPosition }
        assertEquals(listOf(0, 1, 2, 3), positions)
    }

    @Test
    fun `an album that is owned is not counted when moving`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", wantPosition = 0)
        database.albumDao().insert(
            AlbumEntity("2", "r", "Owned", "STUDIO_ALBUM", null, owned = true, wanted = true, wantPosition = 1)
        )
        wanted("3", "r", "Three", wantPosition = 2)

        viewModel.moveUp("3")

        assertEquals(listOf("Three", "One"), titles())
    }

    @Test
    fun `albums not yet put in order fall back to release date, a month before a bare year`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "Year only", "1972")
        wanted("2", "r", "Month", "1972-06")
        wanted("3", "r", "Later", "1973-01")

        assertEquals(listOf("Month", "Year only", "Later"), titles())
    }

    @Test
    fun `an empty release date counts as undated, so it comes last`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "Empty", "")
        wanted("2", "r", "Dated", "1972")

        assertEquals(listOf("Dated", "Empty"), titles())
    }

    @Test
    fun `removing an album from the wants stops it being wanted, and it leaves the list`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", wantPosition = 0)
        wanted("2", "r", "Two", wantPosition = 1)

        viewModel.remove("1")

        assertEquals(listOf("Two"), titles())
        assertEquals(false, database.albumDao().getById("1")?.wanted)
    }

    @Test
    fun `removing a want keeps the album and its other details`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", "1972-06-16")

        viewModel.remove("1")

        val kept = database.albumDao().getById("1")!!
        assertEquals("One", kept.title)
        assertEquals("1972-06-16", kept.firstReleaseDate)
        assertEquals(false, kept.owned)
    }

    @Test
    fun `removing a want does not change the order of the others`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", wantPosition = 0)
        wanted("2", "r", "Two", wantPosition = 1)
        wanted("3", "r", "Three", wantPosition = 2)

        viewModel.remove("2")

        assertEquals(listOf("One", "Three"), titles())
    }

    @Test
    fun `an artist with no wants left is not on the list, and stays a favourite`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One")

        viewModel.remove("1")

        assertEquals(emptyList<String>(), titles())
        assertEquals("Roxy Music", database.artistDao().getById("r")?.name)
    }

    @Test
    fun `an album that was removed and is wanted again goes last`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One", wantPosition = 0)
        wanted("2", "r", "Two", wantPosition = 1)

        viewModel.remove("1")
        database.albumDao().markWanted("1")

        assertEquals(listOf("Two", "One"), titles())
    }

    @Test
    fun `removing an album that is not wanted or does not exist changes nothing`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "One")
        database.albumDao().insert(AlbumEntity("9", "r", "Nine", "STUDIO_ALBUM", null, owned = true, wanted = false))

        viewModel.remove("9")
        viewModel.remove("missing")

        assertEquals(listOf("One"), titles())
        assertEquals(true, database.albumDao().getById("9")?.owned)
    }
}
