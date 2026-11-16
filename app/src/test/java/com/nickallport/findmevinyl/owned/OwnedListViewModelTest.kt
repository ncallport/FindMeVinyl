package com.nickallport.findmevinyl.owned

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AlbumCopyEntity
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OwnedListViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: OwnedListViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        viewModel = OwnedListViewModel(database.albumDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun artist(id: String, name: String) = database.artistDao().insert(ArtistEntity(id, name, null, null, 0))

    private suspend fun album(id: String, artistId: String, title: String, date: String? = null, owned: Boolean = true, wanted: Boolean = false) {
        database.albumDao().insert(AlbumEntity(id, artistId, title, "STUDIO_ALBUM", date, owned = owned, wanted = wanted))
    }

    private suspend fun titles() = viewModel.load().map { it.title }

    @Test
    fun `lists only the albums that are owned`() = runBlocking {
        artist("a", "Abba")
        album("1", "a", "Arrival")
        album("2", "a", "Voulez-Vous", owned = false)
        album("3", "a", "Waterloo", owned = false, wanted = true)

        assertEquals(listOf("Arrival"), titles())
    }

    @Test
    fun `is A to Z by artist, then release date with undated last, then title`() = runBlocking {
        artist("r", "roxy music"); artist("a", "ABC"); artist("s", "The Smiths")
        album("1", "r", "Avalon", "1982-05-31")
        album("2", "r", "Undated")
        album("3", "r", "Roxy Music", "1972-06-16")
        album("4", "a", "Lexicon of Love", "1982")
        album("5", "s", "Meat Is Murder", "1985")

        assertEquals(listOf("Lexicon of Love", "Roxy Music", "Avalon", "Undated", "Meat Is Murder"), titles())
    }

    @Test
    fun `an album owned and wanted is listed once`() = runBlocking {
        artist("a", "Abba")
        album("1", "a", "Arrival", wanted = true)

        assertEquals(listOf("Arrival"), titles())
    }

    @Test
    fun `each album carries the name of its artist and what to open it with`() = runBlocking {
        artist("a", "Abba")
        album("1", "a", "Arrival", "1976-10-11")

        val row = viewModel.load().single()

        assertEquals("1", row.albumId)
        assertEquals("a", row.artistId)
        assertEquals("Abba", row.artistName)
        assertEquals("1976", row.year)
    }

    @Test
    fun `an album of a member of a group shows the member's own name as the group knows it`() = runBlocking {
        artist("n", "Nick Cave"); artist("b", "Nick Cave & The Bad Seeds")
        database.artistGroupDao().insertGroup(ArtistGroupEntity("group/1", "Nick Cave (all)"))
        database.artistGroupDao().insertMembers(
            listOf(
                ArtistGroupMemberEntity(artistId = "n", groupId = "group/1", name = "Nick Cave", ordinal = 0),
                ArtistGroupMemberEntity(artistId = "b", groupId = "group/1", name = "Nick Cave & The Bad Seeds", ordinal = 1)
            )
        )
        album("1", "b", "Let Love In")

        assertEquals("Nick Cave & The Bad Seeds", viewModel.load().single().artistName)
    }

    @Test
    fun `an album under Various Artists says so`() = runBlocking {
        album("1", "89ad4ac3-39f7-470e-963a-56509c546377", "Now 99")

        assertEquals("Various Artists", viewModel.load().single().artistName)
    }

    @Test
    fun `an owned album whose artist is no longer a favourite is still listed`() = runBlocking {
        album("1", "gone", "Somewhere")

        val row = viewModel.load().single()

        assertEquals("Somewhere", row.title)
        assertEquals("Unknown artist", row.artistName)
    }

    @Test
    fun `the copies come with the album in the order they were listed`() = runBlocking {
        artist("a", "Abba")
        album("1", "a", "Arrival")
        database.albumDao().insertCopies(
            listOf(
                AlbumCopyEntity(albumId = "1", format = "CD", note = null, position = 1),
                AlbumCopyEntity(albumId = "1", format = "LP", note = "Green cover", position = 0)
            )
        )

        val copies = viewModel.load().single().copies

        assertEquals(listOf("LP" to "Green cover", "CD" to null), copies.map { it.format to it.note })
    }

    @Test
    fun `the share text is the whole list in the same order, with the formats`() = runBlocking {
        artist("b", "Bob Dylan"); artist("a", "Abba")
        album("1", "b", "Desire"); album("2", "a", "Arrival")
        database.albumDao().insertCopies(listOf(AlbumCopyEntity(albumId = "1", format = "LP", position = 0)))

        assertEquals(
            "My record collection (2 albums):\n\n1. Abba - Arrival\n2. Bob Dylan - Desire (LP)",
            viewModel.exportText()
        )
    }
}
