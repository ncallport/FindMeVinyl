package com.nickallport.findmevinyl.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Several MusicBrainz artists the user keeps together under a name of their own. */
@RunWith(RobolectricTestRunner::class)
class ArtistGroupDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.artistGroupDao()

    private val group = ArtistGroupEntity("group/1", "Acid Mothers Temple Family", position = 3)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a group can be kept and read back`() = runBlocking {
        dao.insertGroup(group)

        assertEquals(listOf(group), dao.getGroups())
        assertEquals(group, dao.getGroup("group/1"))
        assertNull(dao.getGroup("group/none"))
    }

    @Test
    fun `members are read in the order they were given, first member first`() = runBlocking {
        dao.insertGroup(group)
        dao.insertMembers(
            listOf(
                ArtistGroupMemberEntity("b", "group/1", "Acid Mothers Temple and the Melting Paraiso U.F.O.", 1),
                ArtistGroupMemberEntity("a", "group/1", "Acid Mothers Temple", 0)
            )
        )

        assertEquals(listOf("a", "b"), dao.getMembers("group/1").map { it.artistId })
    }

    @Test
    fun `an artist can say which group it is in, and no group when it is in none`() = runBlocking {
        dao.insertGroup(group)
        dao.insertMembers(listOf(ArtistGroupMemberEntity("a", "group/1", "Acid Mothers Temple", 0)))

        assertEquals("group/1", dao.groupIdOf("a"))
        assertNull(dao.groupIdOf("zzz"))
    }

    @Test
    fun `all member ids can be listed, across groups`() = runBlocking {
        dao.insertGroup(group)
        dao.insertGroup(ArtistGroupEntity("group/2", "Ian Dury", 0))
        dao.insertMembers(
            listOf(
                ArtistGroupMemberEntity("a", "group/1", "A", 0),
                ArtistGroupMemberEntity("d", "group/2", "D", 0)
            )
        )

        assertEquals(setOf("a", "d"), dao.getAllMembers().map { it.artistId }.toSet())
    }

    @Test
    fun `a name and a position can be changed`() = runBlocking {
        dao.insertGroup(group)

        dao.setName("group/1", "AMT")
        dao.setPosition("group/1", 9)

        assertEquals(ArtistGroupEntity("group/1", "AMT", 9), dao.getGroup("group/1"))
    }

    @Test
    fun `removing the members of a group leaves the group and other groups alone`() = runBlocking {
        dao.insertGroup(group)
        dao.insertGroup(ArtistGroupEntity("group/2", "Ian Dury", 0))
        dao.insertMembers(
            listOf(ArtistGroupMemberEntity("a", "group/1", "A", 0), ArtistGroupMemberEntity("d", "group/2", "D", 0))
        )

        dao.deleteMembersOf("group/1")

        assertEquals(emptyList<ArtistGroupMemberEntity>(), dao.getMembers("group/1"))
        assertEquals(listOf("d"), dao.getMembers("group/2").map { it.artistId })
        assertEquals(2, dao.getGroups().size)
    }

    @Test
    fun `deleting a group removes just that group`() = runBlocking {
        dao.insertGroup(group)
        dao.insertGroup(ArtistGroupEntity("group/2", "Ian Dury", 0))

        dao.deleteGroup("group/1")

        assertEquals(listOf("group/2"), dao.getGroups().map { it.id })
    }

    @Test
    fun `an artist cannot be a member of two groups, the later one replaces`() = runBlocking {
        dao.insertGroup(group)
        dao.insertGroup(ArtistGroupEntity("group/2", "Ian Dury", 0))
        dao.insertMembers(listOf(ArtistGroupMemberEntity("a", "group/1", "A", 0)))

        dao.insertMembers(listOf(ArtistGroupMemberEntity("a", "group/2", "A", 0)))

        assertEquals("group/2", dao.groupIdOf("a"))
    }

    @Test
    fun `the lowest position counts the groups as well as the favourite artists`() = runBlocking {
        database.artistDao().insert(ArtistEntity("x", "X", null, null, position = 5))
        dao.insertGroup(ArtistGroupEntity("group/1", "G", position = -2))

        assertEquals(-2, database.artistDao().minPosition())
    }

    @Test
    fun `with no favourites and no groups there is no lowest position`() = runBlocking {
        assertNull(database.artistDao().minPosition())
    }
}
