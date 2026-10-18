package com.nickallport.findmevinyl.favourites

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The favourites when some artists are joined into groups. */
@RunWith(RobolectricTestRunner::class)
class FavouritesViewModelGroupsTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: FavouritesViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        viewModel = FavouritesViewModel(database.artistDao(), database.artistGroupDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun favourite(id: String, name: String, position: Int) =
        database.artistDao().insert(ArtistEntity(id, name, null, null, position))

    private suspend fun group(id: String, name: String, position: Int, vararg members: Pair<String, String>) {
        database.artistGroupDao().insertGroup(ArtistGroupEntity(id, name, position))
        database.artistGroupDao().insertMembers(members.mapIndexed { i, (artistId, memberName) ->
            ArtistGroupMemberEntity(artistId, id, memberName, i)
        })
    }

    private suspend fun ids() = viewModel.loadFavourites().map { it.id }

    @Test
    fun `a group is listed instead of its members`() = runBlocking {
        favourite("cave", "Nick Cave", 0)
        favourite("other", "Other", 1)
        group("group/1", "Nick Cave", 0, "cave" to "Nick Cave", "seeds" to "Nick Cave & The Bad Seeds")

        assertEquals(listOf("group/1", "other"), ids())
    }

    @Test
    fun `a group says what it is, who is in it, and has its own picture, which falls back to its first member's`() = runBlocking {
        group("group/1", "Acid Mothers Temple Family", 0, "b" to "AMT & The Melting Paraiso", "a" to "Acid Mothers Temple")

        val listed = viewModel.loadFavourites().single()

        assertEquals("Acid Mothers Temple Family", listed.name)
        assertTrue(listed.isGroup)
        assertEquals(listOf("b", "a"), listed.memberIds)
        assertEquals("group/1", listed.imageId)
    }

    @Test
    fun `an artist that is not in a group is listed as before, with its own picture`() = runBlocking {
        favourite("a", "Genesis", 0)

        val listed = viewModel.loadFavourites().single()

        assertEquals(false, listed.isGroup)
        assertEquals(emptyList<String>(), listed.memberIds)
        assertEquals("a", listed.imageId)
    }

    @Test
    fun `groups and artists are in one order, by position and then name`() = runBlocking {
        favourite("a", "Zebra", 2)
        favourite("b", "Apple", 0)
        group("group/1", "Middle", 1, "x" to "X", "y" to "Y")
        group("group/2", "Aaa", 2, "p" to "P", "q" to "Q")

        assertEquals(listOf("b", "group/1", "group/2", "a"), ids())
    }

    @Test
    fun `moving a group down swaps it with the artist below, and positions are written in the right tables`() = runBlocking {
        favourite("a", "A", 0)
        group("group/1", "G", 1, "x" to "X", "y" to "Y")
        favourite("b", "B", 2)

        viewModel.moveUp("group/1")

        assertEquals(listOf("group/1", "a", "b"), ids())
        assertEquals(0, database.artistGroupDao().getGroup("group/1")!!.position)
        assertEquals(1, database.artistDao().getById("a")!!.position)
    }

    @Test
    fun `moving an artist down past a group works too`() = runBlocking {
        favourite("a", "A", 0)
        group("group/1", "G", 1, "x" to "X", "y" to "Y")

        viewModel.moveDown("a")

        assertEquals(listOf("group/1", "a"), ids())
    }

    @Test
    fun `a hidden member is not moved and is not in the order`() = runBlocking {
        favourite("x", "X", 0)
        favourite("a", "A", 1)
        group("group/1", "G", 0, "x" to "X", "y" to "Y")

        viewModel.moveDown("group/1")

        assertEquals(listOf("a", "group/1"), ids())
        assertEquals(0, database.artistDao().getById("x")!!.position)
    }

    @Test
    fun `reordering by dragging works across groups and artists`() = runBlocking {
        favourite("a", "A", 0)
        group("group/1", "G", 1, "x" to "X", "y" to "Y")
        favourite("b", "B", 2)

        viewModel.reorder(listOf("b", "group/1", "a"))

        assertEquals(listOf("b", "group/1", "a"), ids())
        assertEquals(1, database.artistGroupDao().getGroup("group/1")!!.position)
    }

    @Test
    fun `reordering ignores ids that are members of a group, which are not in the list`() = runBlocking {
        favourite("x", "X", 0)
        favourite("a", "A", 1)
        group("group/1", "G", 2, "x" to "X", "y" to "Y")

        viewModel.reorder(listOf("group/1", "x", "a"))

        assertEquals(listOf("group/1", "a"), ids())
    }

    @Test
    fun `members that were favourites come back where they were when the group is gone`() = runBlocking {
        favourite("x", "X", 3)
        group("group/1", "G", 3, "x" to "X", "y" to "Y")
        assertEquals(listOf("group/1"), ids())

        database.artistGroupDao().deleteMembersOf("group/1")
        database.artistGroupDao().deleteGroup("group/1")

        assertEquals(listOf("x"), ids())
    }

    @Test
    fun `a group lists the names of its members, in order`() = runBlocking {
        group("group/1", "G", 0, "b" to "Second name", "a" to "First name")

        assertEquals(listOf("Second name", "First name"), viewModel.loadFavourites().single().memberNames)
    }
}
