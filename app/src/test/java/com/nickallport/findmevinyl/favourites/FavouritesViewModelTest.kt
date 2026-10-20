package com.nickallport.findmevinyl.favourites

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The favourite artists as the Artists page lists them: A to Z, each with how many albums are owned and wanted. */
@RunWith(RobolectricTestRunner::class)
class FavouritesViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: FavouritesViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        viewModel = FavouritesViewModel(database.artistDao(), database.artistGroupDao(), database.albumDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun artist(id: String, name: String, position: Int = 0) =
        database.artistDao().insert(ArtistEntity(id, name, null, null, position))

    private suspend fun album(id: String, artistId: String, owned: Boolean = false, wanted: Boolean = false) =
        database.albumDao().insert(AlbumEntity(id, artistId, "Album $id", "STUDIO_ALBUM", "1980", owned = owned, wanted = wanted))

    private suspend fun group(id: String, name: String, vararg members: Pair<String, String>) {
        database.artistGroupDao().insertGroup(ArtistGroupEntity(id, name, 0))
        database.artistGroupDao().insertMembers(members.mapIndexed { i, (artistId, memberName) -> ArtistGroupMemberEntity(artistId, id, memberName, i) })
    }

    private suspend fun names() = viewModel.loadFavourites().map { it.name }

    // The order.

    @Test
    fun `returns the favourites A to Z, ignoring case`() = runBlocking {
        artist("1", "roxy music"); artist("2", "ABC"); artist("3", "Genesis")

        assertEquals(listOf("ABC", "Genesis", "roxy music"), names())
    }

    @Test
    fun `a leading The is left off when sorting, so The Smiths is under S`() = runBlocking {
        artist("1", "Talk Talk"); artist("2", "The Smiths"); artist("3", "Simple Minds"); artist("4", "the cure"); artist("5", "Theatre of Hate")

        assertEquals(listOf("the cure", "Simple Minds", "The Smiths", "Talk Talk", "Theatre of Hate"), names())
    }

    @Test
    fun `the old order the user chose makes no difference`() = runBlocking {
        artist("1", "Zebra", position = 0); artist("2", "Abba", position = 5); artist("3", "Middle", position = -3)

        assertEquals(listOf("Abba", "Middle", "Zebra"), names())
    }

    @Test
    fun `returns an empty list when there are no favourites`() = runBlocking {
        assertEquals(emptyList<String>(), names())
    }

    @Test
    fun `a new favourite is listed in its place A to Z, not first`() = runBlocking {
        artist("1", "Abba"); artist("2", "Zebra")

        database.artistDao().insert(ArtistEntity("3", "Madness", null, null, position = -10))

        assertEquals(listOf("Abba", "Madness", "Zebra"), names())
    }

    // Groups.

    @Test
    fun `a group is listed instead of its members, by its own name, among the artists`() = runBlocking {
        artist("cave", "Nick Cave"); artist("seeds", "Nick Cave & The Bad Seeds"); artist("abc", "ABC"); artist("zed", "Zed")
        group("group/1", "Nick Cave, all of it", "cave" to "Nick Cave", "seeds" to "Nick Cave & The Bad Seeds")

        assertEquals(listOf("ABC", "Nick Cave, all of it", "Zed"), names())
    }

    @Test
    fun `a group says what it is, who is in it, and has its own picture, which falls back to its first member's`() = runBlocking {
        artist("cave", "Nick Cave"); artist("seeds", "The Bad Seeds")
        group("group/1", "Cave", "cave" to "Nick Cave", "seeds" to "The Bad Seeds")

        val listed = viewModel.loadFavourites().single()

        assertTrue(listed.isGroup)
        assertEquals("group/1", listed.id)
        assertEquals(listOf("cave", "seeds"), listed.memberIds)
        assertEquals(listOf("Nick Cave", "The Bad Seeds"), listed.memberNames)
        assertEquals("group/1", listed.imageId)
    }

    @Test
    fun `an artist that is not in a group is listed as itself, with its own picture`() = runBlocking {
        artist("1", "Genesis")

        val listed = viewModel.loadFavourites().single()

        assertFalse(listed.isGroup)
        assertEquals("1", listed.imageId)
        assertEquals(emptyList<String>(), listed.memberIds)
    }

    @Test
    fun `members that were favourites come back in their place when the group is gone`() = runBlocking {
        artist("cave", "Nick Cave"); artist("abc", "ABC")
        artist("seeds", "The Bad Seeds")
        group("group/1", "Zzz group", "cave" to "Nick Cave", "seeds" to "The Bad Seeds")
        assertEquals(listOf("ABC", "Zzz group"), names())

        database.artistGroupDao().deleteMembersOf("group/1")
        database.artistGroupDao().deleteGroup("group/1")

        assertEquals(listOf("ABC", "The Bad Seeds", "Nick Cave"), names())
    }

    // How many albums are owned and wanted.

    @Test
    fun `each artist comes with how many albums are owned and how many are wanted`() = runBlocking {
        artist("1", "Genesis")
        album("a", "1", owned = true); album("b", "1", owned = true); album("c", "1", wanted = true); album("d", "1")

        val listed = viewModel.loadFavourites().single()

        assertEquals(2, listed.ownedCount)
        assertEquals(1, listed.wantedCount)
    }

    @Test
    fun `an album that is owned and wanted counts as both`() = runBlocking {
        artist("1", "Genesis")
        album("a", "1", owned = true, wanted = true)

        val listed = viewModel.loadFavourites().single()

        assertEquals(1, listed.ownedCount)
        assertEquals(1, listed.wantedCount)
    }

    @Test
    fun `an artist with no albums marked has none of either`() = runBlocking {
        artist("1", "Genesis")
        album("a", "1")

        val listed = viewModel.loadFavourites().single()

        assertEquals(0, listed.ownedCount)
        assertEquals(0, listed.wantedCount)
    }

    @Test
    fun `the counts of one artist are not those of another`() = runBlocking {
        artist("1", "Genesis"); artist("2", "Roxy Music")
        album("a", "1", owned = true); album("b", "2", wanted = true); album("c", "2", wanted = true)

        val byName = viewModel.loadFavourites().associateBy { it.name }

        assertEquals(1 to 0, byName.getValue("Genesis").ownedCount to byName.getValue("Genesis").wantedCount)
        assertEquals(0 to 2, byName.getValue("Roxy Music").ownedCount to byName.getValue("Roxy Music").wantedCount)
    }

    @Test
    fun `a group counts the albums of all its members`() = runBlocking {
        artist("cave", "Nick Cave"); artist("seeds", "The Bad Seeds")
        group("group/1", "Cave", "cave" to "Nick Cave", "seeds" to "The Bad Seeds")
        album("a", "cave", owned = true); album("b", "seeds", owned = true); album("c", "seeds", wanted = true); album("d", "cave", wanted = true)

        val listed = viewModel.loadFavourites().single()

        assertEquals(2, listed.ownedCount)
        assertEquals(2, listed.wantedCount)
    }

    @Test
    fun `an album added by hand is counted`() = runBlocking {
        artist("1", "Big Country")
        album("custom/1", "1", owned = true)

        assertEquals(1, viewModel.loadFavourites().single().ownedCount)
    }
}
