package com.nickallport.findmevinyl.wantlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.export.ShareOptions
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        viewModel = WantListViewModel(database.albumDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun artist(id: String, name: String, position: Int = 0) =
        database.artistDao().insert(ArtistEntity(id, name, null, null, position))

    private suspend fun wanted(id: String, artistId: String, title: String, date: String? = null, owned: Boolean = false) {
        database.albumDao().insert(AlbumEntity(id, artistId, title, "STUDIO_ALBUM", date, owned = owned, wanted = true))
    }

    private suspend fun titles() = viewModel.loadWantList().map { it.albumTitle }

    private suspend fun priority(id: String) = database.albumDao().getById(id)!!.wantPriority

    /** Four wants by four artists, A to Z: One, Two, Three, Four. */
    private suspend fun four() {
        artist("a", "Abba"); artist("b", "Bowie"); artist("c", "Cure"); artist("d", "Dury")
        wanted("1", "a", "One"); wanted("2", "b", "Two"); wanted("3", "c", "Three"); wanted("4", "d", "Four")
    }

    // The order to begin with.

    @Test
    fun `with nothing moved it is A to Z by artist and then release date, undated last`() = runBlocking {
        artist("r", "roxy music"); artist("a", "ABC")
        wanted("1", "r", "Avalon", "1982-05-31")
        wanted("2", "r", "Undated")
        wanted("3", "r", "Roxy Music", "1972-06-16")
        wanted("4", "a", "Lexicon of Love", "1982-06-21")

        assertEquals(listOf("Lexicon of Love", "Roxy Music", "Avalon", "Undated"), titles())
    }

    @Test
    fun `the order of the artists in the favourites makes no difference`() = runBlocking {
        artist("z", "Zebra", position = 0); artist("a", "Abba", position = 9)
        wanted("1", "z", "Z album"); wanted("2", "a", "A album")

        assertEquals(listOf("A album", "Z album"), titles())
    }

    @Test
    fun `an artist's name in the list is the artist's, and an album that is owned and wanted is listed`() = runBlocking {
        artist("r", "Roxy Music")
        wanted("1", "r", "Avalon", owned = true)

        val listed = viewModel.loadWantList().single()

        assertEquals("Roxy Music", listed.artistName)
        assertEquals("Avalon", listed.albumTitle)
    }

    @Test
    fun `returns an empty list when nothing is wanted`() = runBlocking {
        assertEquals(emptyList<Any>(), viewModel.loadWantList())
    }

    @Test
    fun `an album wanted on a format carries it into the list and the share text`() = runBlocking {
        artist("b", "Bob Dylan")
        database.albumDao().insert(
            AlbumEntity("1", "b", "Desire", "STUDIO_ALBUM", "1976", owned = true, wanted = true, wantFormat = "LP")
        )
        database.albumDao().insert(AlbumEntity("2", "b", "Blood on the Tracks", "STUDIO_ALBUM", "1975", owned = false, wanted = true))

        assertEquals(listOf(null, "LP"), viewModel.loadWantList().map { it.wantFormat })
        assertEquals(true, viewModel.exportText().contains("- Desire (on LP)"))
    }

    // Moving.

    @Test
    fun `moving a want up swaps it with the one above, and only the one moved is pinned`() = runBlocking {
        four()

        viewModel.moveUp("3")

        assertEquals(listOf("One", "Three", "Two", "Four"), titles())
        assertEquals(1, priority("3"))
        assertNull(priority("1")); assertNull(priority("2")); assertNull(priority("4"))
    }

    @Test
    fun `moving a want down swaps it with the one below`() = runBlocking {
        four()

        viewModel.moveDown("1")

        assertEquals(listOf("Two", "One", "Three", "Four"), titles())
        assertEquals(1, priority("1"))
    }

    @Test
    fun `the first want cannot move up and the last cannot move down, and nothing is pinned`() = runBlocking {
        four()

        viewModel.moveUp("1")
        viewModel.moveDown("4")

        assertEquals(listOf("One", "Two", "Three", "Four"), titles())
        assertNull(priority("1")); assertNull(priority("4"))
    }

    @Test
    fun `moving does nothing for a want that is not there`() = runBlocking {
        four()
        database.albumDao().insert(AlbumEntity("9", "a", "Nine", "STUDIO_ALBUM", null, owned = false, wanted = false))

        viewModel.moveUp("9")
        viewModel.moveDown("missing")
        viewModel.moveToTop("9")

        assertEquals(listOf("One", "Two", "Three", "Four"), titles())
        assertNull(priority("9"))
    }

    @Test
    fun `a want sent to the top is first, pinned at the top, and the others keep their order`() = runBlocking {
        four()

        viewModel.moveToTop("3")

        assertEquals(listOf("Three", "One", "Two", "Four"), titles())
        assertEquals(0, priority("3"))
        assertNull(priority("1")); assertNull(priority("2")); assertNull(priority("4"))
    }

    @Test
    fun `sending the first want to the top changes nothing`() = runBlocking {
        four()

        viewModel.moveToTop("1")

        assertEquals(listOf("One", "Two", "Three", "Four"), titles())
        assertNull(priority("1"))
    }

    @Test
    fun `sending a second want to the top puts the first one place down, and it stays there`() = runBlocking {
        four()
        viewModel.moveToTop("3")

        viewModel.moveToTop("4")

        assertEquals(listOf("Four", "Three", "One", "Two"), titles())
        assertEquals(0, priority("4"))
        assertEquals(1, priority("3"))
        assertNull(priority("1")); assertNull(priority("2"))
    }

    @Test
    fun `moving a pinned want past a pinned neighbour swaps their places`() = runBlocking {
        four()
        viewModel.moveToTop("3")
        viewModel.moveToTop("4")

        viewModel.moveDown("4")

        assertEquals(listOf("Three", "Four", "One", "Two"), titles())
        assertEquals(0, priority("3"))
        assertEquals(1, priority("4"))
    }

    @Test
    fun `a want that is moved stays where it was put, while the rest stay A to Z`() = runBlocking {
        four()
        viewModel.moveDown("1")
        viewModel.moveDown("1")

        assertEquals(listOf("Two", "Three", "One", "Four"), titles())
        artist("e", "Eno"); wanted("5", "e", "Five")

        assertEquals(listOf("Two", "Three", "One", "Four", "Five"), titles())
    }

    @Test
    fun `a want added later goes in its place A to Z among the ones not moved`() = runBlocking {
        four()
        viewModel.moveToTop("4")
        artist("x", "Aha"); wanted("5", "x", "Five")

        assertEquals(listOf("Four", "One", "Five", "Two", "Three"), titles())
    }

    @Test
    fun `a move is kept when the list is loaded again`() = runBlocking {
        four()
        viewModel.moveToTop("2")

        assertEquals(listOf("Two", "One", "Three", "Four"), WantListViewModel(database.albumDao()).loadWantList().map { it.albumTitle })
    }

    // A want that is taken off.

    @Test
    fun `taking a want off the list forgets its place, and the others stay in the places they were put`() = runBlocking {
        four()
        viewModel.moveToTop("3")
        viewModel.moveToTop("4")

        database.albumDao().setWanted("4", false)

        // Three was put second, so it is still second, with One and Two around it.
        assertEquals(listOf("One", "Three", "Two"), titles())
        assertNull(priority("4"))
        assertEquals(1, priority("3"))
    }

    @Test
    fun `an album wanted again after being taken off is not pinned`() = runBlocking {
        four()
        viewModel.moveToTop("3")
        database.albumDao().setWanted("3", false)

        database.albumDao().markWanted("3")

        assertNull(priority("3"))
        assertEquals(listOf("One", "Two", "Three", "Four"), titles())
    }

    // Sharing.

    @Test
    fun `the share text follows the order shown, one numbered line for each want`() = runBlocking {
        four()
        viewModel.moveToTop("3")

        val expected = """
            My vinyl want list:

            1. Cure - Three
            2. Abba - One
            3. Bowie - Two
            4. Dury - Four
        """.trimIndent()

        assertEquals(expected, viewModel.exportText())
    }

    @Test
    fun `export text can start with a message`() = runBlocking {
        artist("a", "ABC")
        wanted("1", "a", "Lexicon of Love", "1982-06-21")

        val text = viewModel.exportText(ShareOptions(intro = "For my birthday", limit = null))

        assertEquals("For my birthday\n\nMy vinyl want list:\n\n1. ABC - Lexicon of Love", text)
    }

    @Test
    fun `export text with a limit takes the top of the list in the order shown`() = runBlocking {
        four()
        viewModel.moveToTop("4")

        val text = viewModel.exportText(ShareOptions(limit = 2))

        assertEquals("My vinyl want list (top 2 of 4):\n\n1. Dury - Four\n2. Abba - One", text)
    }

    @Test
    fun `the default export is the whole list with no message`() = runBlocking {
        four()

        assertEquals(viewModel.exportText(ShareOptions()), viewModel.exportText())
    }

    @Test
    fun `the genre of an artist makes no difference to the want list`() = runBlocking {
        database.artistDao().insert(ArtistEntity("z", "Zebra", null, null, 0, "folk"))
        database.artistDao().insert(ArtistEntity("a", "Abba", null, null, 1, "pop"))
        wanted("1", "z", "Z album", "1980"); wanted("2", "a", "A album", "1980")

        assertEquals(listOf("A album", "Z album"), titles())
    }
}
