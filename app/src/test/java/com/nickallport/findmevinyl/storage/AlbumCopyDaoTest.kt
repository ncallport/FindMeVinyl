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

    @Test
    fun `an album has no copy details to begin with`() = runBlocking {
        dao.insert(album())

        assertNull(dao.getById("1")!!.userFormats)
        assertNull(dao.getById("1")!!.userNote)
    }

    @Test
    fun `saves the formats and the note of the copy`() = runBlocking {
        dao.insert(album())

        dao.setCopy("1", "LP", "BMG vinyl")

        assertEquals("LP", dao.getById("1")!!.userFormats)
        assertEquals("BMG vinyl", dao.getById("1")!!.userNote)
    }

    @Test
    fun `saving null for both clears them`() = runBlocking {
        dao.insert(album())
        dao.setCopy("1", "LP", "BMG vinyl")

        dao.setCopy("1", null, null)

        assertNull(dao.getById("1")!!.userFormats)
        assertNull(dao.getById("1")!!.userNote)
    }

    @Test
    fun `syncing the discography again does not touch the copy details`() = runBlocking {
        dao.insert(album())
        dao.setCopy("1", "LP", "BMG vinyl")

        dao.insertAllIfNotExist(listOf(album()))

        assertEquals("LP", dao.getById("1")!!.userFormats)
        assertEquals("BMG vinyl", dao.getById("1")!!.userNote)
    }

    @Test
    fun `a lookup of the release formats does not touch the copy details`() = runBlocking {
        dao.insert(album())
        dao.setCopy("1", "LP", "BMG vinyl")

        dao.setChecked("1", "CD", "Official", 5L)
        dao.setFormats("1", "CD", 6L)

        val saved = dao.getById("1")!!
        assertEquals("CD", saved.formats)
        assertEquals("LP", saved.userFormats)
        assertEquals("BMG vinyl", saved.userNote)
    }

    @Test
    fun `an album with copy details is in the backup even when it is neither owned nor wanted`() = runBlocking {
        dao.insert(album("1").copy(owned = false))
        dao.insert(album("2").copy(owned = false))
        dao.setCopy("1", null, "Kept after I sold it")

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

        dao.setCopy("1", "LP", "note")
        dao.insertAllIfNotExist(listOf(album()))
        dao.setChecked("1", "CD", "Official", 5L)

        val saved = dao.getById("1")!!
        assertEquals("release-9", saved.myPressingId)
        assertEquals("LP", saved.userFormats)
    }

    @Test
    fun `an album with only a pressing chosen is in the backup even when it is not owned`() = runBlocking {
        dao.insert(album("1").copy(owned = false))
        dao.insert(album("2").copy(owned = false))
        dao.setMyPressing("1", "release-9", "text")

        assertEquals(listOf("1"), dao.getOwnedOrWanted().map { it.id })
    }

    // A note for each format.

    @Test
    fun `saves the notes for the formats, and null takes them away`() = runBlocking {
        dao.insert(album())

        dao.setFormatNotes("1", """{"LP":"scratched"}""")
        assertEquals("""{"LP":"scratched"}""", dao.getById("1")!!.userFormatNotes)

        dao.setFormatNotes("1", null)
        assertEquals(null, dao.getById("1")!!.userFormatNotes)
    }

    @Test
    fun `an album with only format notes is in the backup even when it is neither owned nor wanted`() = runBlocking {
        dao.insert(album("1").copy(owned = false))
        dao.insert(album("2").copy(owned = false))
        dao.setFormatNotes("1", """{"CD":"from Mum"}""")

        assertEquals(listOf("1"), dao.getOwnedOrWanted().map { it.id })
    }
}
