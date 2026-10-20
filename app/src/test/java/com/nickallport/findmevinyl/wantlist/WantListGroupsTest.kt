package com.nickallport.findmevinyl.wantlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.export.ShareOptions
import com.nickallport.findmevinyl.storage.AlbumEntity
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

/** The want list, and the text shared from it, when some artists are joined into a group. */
@RunWith(RobolectricTestRunner::class)
class WantListGroupsTest {

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

    private suspend fun group(id: String, name: String, position: Int, vararg members: String) {
        database.artistGroupDao().insertGroup(ArtistGroupEntity(id, name, position))
        database.artistGroupDao().insertMembers(members.mapIndexed { i, m -> ArtistGroupMemberEntity(m, id, "Name of $m", i) })
    }

    private suspend fun wanted(id: String, artist: String, title: String, date: String?, owned: Boolean = false) {
        database.albumDao().insert(AlbumEntity(id, artist, title, "STUDIO_ALBUM", date, owned = owned, wanted = !owned))
    }

    @Test
    fun `wanted albums of the members are listed under the names of the artists themselves`() = runBlocking {
        database.artistDao().insert(ArtistEntity("cave", "Nick Cave", null, null, 0))
        group("group/1", "Nick Cave, all of it", 0, "cave", "seeds")
        wanted("a", "cave", "From Her to Eternity", "1984")
        wanted("b", "seeds", "Henry's Dream", "1992")

        val list = viewModel.loadWantList()

        assertEquals(listOf("From Her to Eternity", "Henry's Dream"), list.map { it.albumTitle })
        assertEquals(listOf("Name of cave", "Name of seeds"), list.map { it.artistName })
    }

    @Test
    fun `a member that is not a favourite artist is still listed, under its own name`() = runBlocking {
        group("group/1", "Ian Dury", 0, "dury", "blockheads")
        wanted("a", "blockheads", "Do It Yourself", "1979")

        val list = viewModel.loadWantList()

        assertEquals(listOf("Name of blockheads" to "Do It Yourself"), list.map { it.artistName to it.albumTitle })
    }

    @Test
    fun `an artist that is neither a favourite nor in a group is not listed, as before`() = runBlocking {
        wanted("a", "stranger", "Album", "1990")

        assertEquals(emptyList<String>(), viewModel.loadWantList().map { it.albumTitle })
    }

    @Test
    fun `groups and artists are in one A to Z order, by the name of the artist of each album`() = runBlocking {
        database.artistDao().insert(ArtistEntity("a", "Abba", null, null, 0))
        database.artistDao().insert(ArtistEntity("z", "Zebra", null, null, 2))
        group("group/1", "Middle", 1, "x", "y")
        wanted("1", "a", "A album", "1980")
        wanted("2", "x", "X album", "1980")
        wanted("3", "z", "Z album", "1980")

        assertEquals(listOf("A album", "X album", "Z album"), viewModel.loadWantList().map { it.albumTitle })
    }

    @Test
    fun `the albums of a group are by member name, then release date`() = runBlocking {
        group("group/1", "G", 0, "x", "y")
        wanted("1", "x", "One", "1990")
        wanted("2", "y", "Two", "1980")
        wanted("3", "x", "Three", "1985")

        assertEquals(listOf("Three", "One", "Two"), viewModel.loadWantList().map { it.albumTitle })
    }

    @Test
    fun `moving an album moves it among all the wanted albums, members of a group or not`() = runBlocking {
        group("group/1", "G", 0, "x", "y")
        wanted("1", "x", "One", "1980")
        wanted("2", "y", "Two", "1985")
        wanted("3", "x", "Three", "1990")

        viewModel.moveDown("1")

        assertEquals(listOf("Three", "One", "Two"), viewModel.loadWantList().map { it.albumTitle })
    }

    @Test
    fun `an album that is owned is not on the list, in a group or out of one`() = runBlocking {
        group("group/1", "G", 0, "x", "y")
        wanted("1", "x", "Owned one", "1980", owned = true)
        wanted("2", "y", "Wanted one", "1985")

        assertEquals(listOf("Wanted one"), viewModel.loadWantList().map { it.albumTitle })
    }

    @Test
    fun `an artist that is a favourite and in a group is listed under the name it has in the group`() = runBlocking {
        database.artistDao().insert(ArtistEntity("cave", "Nick Cave", null, null, 5))
        group("group/1", "Nick Cave, all of it", 0, "cave", "seeds")
        wanted("a", "cave", "Album", "1984")

        assertEquals(listOf("Name of cave"), viewModel.loadWantList().map { it.artistName })
    }

    @Test
    fun `taking an album off the list works for an album of a group`() = runBlocking {
        group("group/1", "G", 0, "x", "y")
        wanted("1", "x", "One", "1980")

        database.albumDao().setWanted("1", false)

        assertEquals(emptyList<String>(), viewModel.loadWantList().map { it.albumTitle })
    }

    @Test
    fun `the shared text names the artist of each album, not the group`() = runBlocking {
        group("group/1", "Nick Cave, all of it", 0, "cave", "seeds")
        wanted("a", "cave", "From Her to Eternity", "1984")
        wanted("b", "seeds", "Henry's Dream", "1992")

        val text = viewModel.exportText(ShareOptions())

        assertTrue(text.contains("1. Name of cave - From Her to Eternity"))
        assertTrue(text.contains("2. Name of seeds - Henry's Dream"))
        assertEquals(0, Regex("Nick Cave, all of it").findAll(text).count())
    }
}
