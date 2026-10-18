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

/** What the user records about their own copy of an album is kept apart from what MusicBrainz says. */
@RunWith(RobolectricTestRunner::class)
class AlbumCopyDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.albumDao()

    private fun album(id: String = "1") =
        AlbumEntity(id, "artist", "Olympia", "STUDIO_ALBUM", "2010-10-20", owned = true, wanted = false)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun copy(format: String, note: String? = null, position: Int = 0, albumId: String = "1") =
        AlbumCopyEntity(albumId = albumId, format = format, note = note, position = position)

    @Test
    fun `an album has no copies and no note to begin with`() = runBlocking {
        dao.insert(album())

        assertEquals(emptyList<AlbumCopyEntity>(), dao.getCopies("1"))
        assertNull(dao.getById("1")!!.userNote)
    }

    @Test
    fun `copies come back in the order they were listed, several on the same format`() = runBlocking {
        dao.insert(album())

        dao.insertCopies(listOf(copy("CD", "from Mum", 1), copy("LP", "green cover", 0), copy("LP", "blue cover", 2)))

        assertEquals(
            listOf("LP" to "green cover", "CD" to "from Mum", "LP" to "blue cover"),
            dao.getCopies("1").map { it.format to it.note }
        )
    }

    @Test
    fun `the copies of one album are not the copies of another`() = runBlocking {
        dao.insert(album("1"))
        dao.insert(album("2"))
        dao.insertCopies(listOf(copy("LP", albumId = "1"), copy("CD", albumId = "2")))

        assertEquals(listOf("LP"), dao.getCopies("1").map { it.format })
        assertEquals(listOf("1" to "LP", "2" to "CD"), dao.getAllCopies().map { it.albumId to it.format })
    }

    @Test
    fun `old copies go and the ones just saved stay`() = runBlocking {
        dao.insert(album())
        dao.insertCopies(listOf(copy("LP"), copy("CD", position = 1)))
        val fresh = dao.insertCopies(listOf(copy("MC")))

        dao.deleteCopiesExcept("1", fresh)

        assertEquals(listOf("MC"), dao.getCopies("1").map { it.format })
    }

    @Test
    fun `with nothing to keep every copy of the album goes, and other albums keep theirs`() = runBlocking {
        dao.insert(album("1"))
        dao.insert(album("2"))
        dao.insertCopies(listOf(copy("LP", albumId = "1"), copy("CD", albumId = "2")))

        dao.deleteCopiesExcept("1", emptyList())

        assertEquals(emptyList<AlbumCopyEntity>(), dao.getCopies("1"))
        assertEquals(listOf("CD"), dao.getCopies("2").map { it.format })
    }

    @Test
    fun `saves the note about the album, and null clears it`() = runBlocking {
        dao.insert(album())

        dao.setNote("1", "BMG vinyl")
        assertEquals("BMG vinyl", dao.getById("1")!!.userNote)

        dao.setNote("1", null)
        assertNull(dao.getById("1")!!.userNote)
    }

    @Test
    fun `syncing the discography again does not touch the copies or the note`() = runBlocking {
        dao.insert(album())
        dao.insertCopies(listOf(copy("LP", "BMG vinyl")))
        dao.setNote("1", "2018")

        dao.insertAllIfNotExist(listOf(album()))

        assertEquals(listOf("LP"), dao.getCopies("1").map { it.format })
        assertEquals("2018", dao.getById("1")!!.userNote)
    }

    @Test
    fun `a lookup of the release formats does not touch the copies or the note`() = runBlocking {
        dao.insert(album())
        dao.insertCopies(listOf(copy("LP", "BMG vinyl")))
        dao.setNote("1", "2018")

        dao.setChecked("1", "CD", "Official", 5L)
        dao.setFormats("1", "CD", 6L)

        val saved = dao.getById("1")!!
        assertEquals("CD", saved.formats)
        assertEquals(listOf("LP"), dao.getCopies("1").map { it.format })
        assertEquals("2018", saved.userNote)
    }

    @Test
    fun `an album with a copy is in the backup even when it is neither owned nor wanted`() = runBlocking {
        dao.insert(album("1").copy(owned = false))
        dao.insert(album("2").copy(owned = false))
        dao.insertCopies(listOf(copy("LP", "Kept after I sold it")))

        assertEquals(listOf("1"), dao.getOwnedOrWanted().map { it.id })
    }

    @Test
    fun `an album with only a note is in the backup even when it is neither owned nor wanted`() = runBlocking {
        dao.insert(album("1").copy(owned = false))
        dao.insert(album("2").copy(owned = false))
        dao.setNote("1", "Kept after I sold it")

        assertEquals(listOf("1"), dao.getOwnedOrWanted().map { it.id })
    }

    // The pressing the user chose as theirs.

    @Test
    fun `saves the pressing the user chose and a line that describes it`() = runBlocking {
        dao.insert(album())

        dao.setMyPressing("1", "release-9", "1972, Portugal, Charisma")

        assertEquals("release-9", dao.getById("1")!!.myPressingId)
        assertEquals("1972, Portugal, Charisma", dao.getById("1")!!.myPressingText)
    }

    @Test
    fun `saving null for both takes the pressing away`() = runBlocking {
        dao.insert(album())
        dao.setMyPressing("1", "release-9", "1972, Portugal, Charisma")

        dao.setMyPressing("1", null, null)

        assertNull(dao.getById("1")!!.myPressingId)
        assertNull(dao.getById("1")!!.myPressingText)
    }

    @Test
    fun `the pressing is kept apart from the copy details and from syncs and lookups`() = runBlocking {
        dao.insert(album())
        dao.setMyPressing("1", "release-9", "1972, Portugal, Charisma")

        dao.insertCopies(listOf(AlbumCopyEntity(albumId = "1", format = "LP", note = "note")))
        dao.insertAllIfNotExist(listOf(album()))
        dao.setChecked("1", "CD", "Official", 5L)

        val saved = dao.getById("1")!!
        assertEquals("release-9", saved.myPressingId)
        assertEquals(listOf("LP"), dao.getCopies("1").map { it.format })
    }

    @Test
    fun `an album with only a pressing chosen is in the backup even when it is not owned`() = runBlocking {
        dao.insert(album("1").copy(owned = false))
        dao.insert(album("2").copy(owned = false))
        dao.setMyPressing("1", "release-9", "text")

        assertEquals(listOf("1"), dao.getOwnedOrWanted().map { it.id })
    }
}
