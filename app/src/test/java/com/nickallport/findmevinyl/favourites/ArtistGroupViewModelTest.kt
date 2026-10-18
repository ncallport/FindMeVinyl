package com.nickallport.findmevinyl.favourites

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Joining artists into a group, changing a group and splitting it up again. */
@RunWith(RobolectricTestRunner::class)
class ArtistGroupViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: ArtistGroupViewModel
    private var nextId = 0

    private val cave = GroupMember("cave", "Nick Cave")
    private val seeds = GroupMember("seeds", "Nick Cave & The Bad Seeds")
    private val dury = GroupMember("dury", "Ian Dury")
    private val blockheads = GroupMember("blockheads", "Ian Dury & The Blockheads")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        viewModel = ArtistGroupViewModel(database.artistDao(), database.artistGroupDao(), newId = { "group/${++nextId}" })
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun favourite(id: String, name: String, position: Int) =
        database.artistDao().insert(ArtistEntity(id, name, null, null, position))

    private fun saved(result: GroupResult) = (result as GroupResult.Saved).group

    @Test
    fun `joining two artists makes a group with the name the user gave`() = runBlocking {
        val group = saved(viewModel.create("Nick Cave, all of it", listOf(cave, seeds)))

        assertEquals("Nick Cave, all of it", group.name)
        assertEquals(listOf(cave, seeds), group.members)
        assertEquals(group, viewModel.load(group.id))
    }

    @Test
    fun `the group id says it is a group`() = runBlocking {
        val group = saved(viewModel.create("G", listOf(cave, seeds)))

        assertTrue(ArtistGroupViewModel.isGroupId(group.id))
        assertEquals(false, ArtistGroupViewModel.isGroupId("1c70a3fc-fe03-4b23-ba9c-873c33dcd814"))
    }

    @Test
    fun `a blank name becomes the name of the first member, and the name is trimmed`() = runBlocking {
        assertEquals("Nick Cave", saved(viewModel.create("   ", listOf(cave, seeds))).name)
        assertEquals("Ian Dury", saved(viewModel.create("  Ian Dury  ", listOf(dury, blockheads))).name)
    }

    @Test
    fun `one artist, or the same artist twice, is not a group`() = runBlocking {
        assertEquals(GroupResult.TooFewMembers, viewModel.create("G", listOf(cave)))
        assertEquals(GroupResult.TooFewMembers, viewModel.create("G", listOf(cave, cave)))
        assertEquals(GroupResult.TooFewMembers, viewModel.create("G", emptyList()))
    }

    @Test
    fun `an artist already in a group cannot be put in another, and the answer says which`() = runBlocking {
        viewModel.create("Nick Cave", listOf(cave, seeds))

        val result = viewModel.create("Other", listOf(seeds, dury))

        assertEquals(GroupResult.AlreadyInGroup("Nick Cave & The Bad Seeds", "Nick Cave"), result)
        assertNull(viewModel.groupOf("dury"))
    }

    @Test
    fun `the group takes the place in the favourites of its first member`() = runBlocking {
        favourite("cave", "Nick Cave", 4)
        favourite("other", "Other", 1)

        val group = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))

        assertEquals(4, database.artistGroupDao().getGroup(group.id)!!.position)
    }

    @Test
    fun `when the first member is not a favourite the group goes first`() = runBlocking {
        favourite("other", "Other", 3)

        val group = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))

        assertEquals(2, database.artistGroupDao().getGroup(group.id)!!.position)
    }

    @Test
    fun `an artist can say which group it is in`() = runBlocking {
        val group = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))

        assertEquals(group, viewModel.groupOf("seeds"))
        assertNull(viewModel.groupOf("dury"))
    }

    @Test
    fun `members who were favourites stay in the favourites table, so splitting the group brings them back`() = runBlocking {
        favourite("cave", "Nick Cave", 4)

        val group = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))
        viewModel.ungroup(group.id)

        assertEquals(listOf("cave"), database.artistDao().getAllFavourites().map { it.id })
        assertEquals(4, database.artistDao().getById("cave")!!.position)
        assertNull(viewModel.groupOf("cave"))
        assertNull(viewModel.load(group.id))
    }

    @Test
    fun `changing a group can rename it and change who is in it, keeping its place`() = runBlocking {
        favourite("cave", "Nick Cave", 4)
        val group = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))
        val grinderman = GroupMember("grinderman", "Grinderman")

        val changed = saved(viewModel.update(group.id, "Nick Cave and friends", listOf(cave, seeds, grinderman)))

        assertEquals("Nick Cave and friends", changed.name)
        assertEquals(listOf(cave, seeds, grinderman), changed.members)
        assertEquals(4, database.artistGroupDao().getGroup(group.id)!!.position)
        assertEquals(group.id, viewModel.groupOf("grinderman")!!.id)
    }

    @Test
    fun `a member taken out of a group is no longer in it`() = runBlocking {
        val group = saved(viewModel.create("G", listOf(cave, seeds, dury)))

        viewModel.update(group.id, "G", listOf(cave, seeds))

        assertNull(viewModel.groupOf("dury"))
        assertEquals(listOf("cave", "seeds"), viewModel.load(group.id)!!.members.map { it.artistId })
    }

    @Test
    fun `changing a group cannot leave it with fewer than two, and leaves it as it was`() = runBlocking {
        val group = saved(viewModel.create("G", listOf(cave, seeds)))

        val result = viewModel.update(group.id, "Renamed", listOf(cave))

        assertEquals(GroupResult.TooFewMembers, result)
        assertEquals(group, viewModel.load(group.id))
    }

    @Test
    fun `changing a group cannot take an artist from another group`() = runBlocking {
        val first = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))
        viewModel.create("Ian Dury", listOf(dury, blockheads))

        val result = viewModel.update(first.id, "Nick Cave", listOf(cave, seeds, dury))

        assertEquals(GroupResult.AlreadyInGroup("Ian Dury", "Ian Dury"), result)
        assertEquals(listOf("cave", "seeds"), viewModel.load(first.id)!!.members.map { it.artistId })
    }

    @Test
    fun `changing a group that is not there does nothing and says too few`() = runBlocking {
        assertEquals(GroupResult.TooFewMembers, viewModel.update("group/none", "G", listOf(cave)))
    }

    @Test
    fun `splitting up a group leaves other groups alone`() = runBlocking {
        val nick = saved(viewModel.create("Nick Cave", listOf(cave, seeds)))
        val ian = saved(viewModel.create("Ian Dury", listOf(dury, blockheads)))

        viewModel.ungroup(nick.id)

        assertEquals(ian, viewModel.load(ian.id))
    }

    @Test
    fun `the id of a group, as a group page has, gives that group`() = runBlocking {
        val group = saved(viewModel.create("G", listOf(cave, seeds)))

        assertEquals(group, viewModel.groupOf(group.id))
        assertNull(viewModel.groupOf("group/none"))
    }
}

