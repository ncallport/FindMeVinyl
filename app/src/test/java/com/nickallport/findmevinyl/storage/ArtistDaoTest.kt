package com.nickallport.findmevinyl.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ArtistDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.artistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a favourited artist can be retrieved`() = runBlocking {
        val artist = ArtistEntity(
            id = "1c70a3fc-fe03-4b23-ba9c-873c33dcd814",
            name = "Genesis",
            type = "Group",
            country = "GB"
        )

        dao.insert(artist)

        val favourites = dao.getAllFavourites()
        assertEquals(1, favourites.size)
        assertEquals("Genesis", favourites[0].name)
    }

    @Test
    fun `removing a favourite deletes it`() = runBlocking {
        val artist = ArtistEntity(
            id = "1c70a3fc-fe03-4b23-ba9c-873c33dcd814",
            name = "Genesis",
            type = "Group",
            country = "GB"
        )
        dao.insert(artist)

        dao.deleteById(artist.id)

        assertTrue(dao.getAllFavourites().isEmpty())
    }

    @Test
    fun `an artist can be looked up by id`() = runBlocking {
        dao.insert(ArtistEntity(id = "g-1", name = "Genesis", type = "Group", country = "GB"))

        assertEquals("Genesis", dao.getById("g-1")?.name)
        assertNull(dao.getById("unknown"))
    }

    // The genre the user chose.

    @Test
    fun `an artist has no genre to begin with, and one can be chosen and taken away`() = runBlocking {
        dao.insert(ArtistEntity("a", "Genesis", null, null))
        assertNull(dao.getById("a")!!.genre)

        dao.setGenre("a", "progressive rock")
        assertEquals("progressive rock", dao.getById("a")!!.genre)

        dao.setGenre("a", null)
        assertNull(dao.getById("a")!!.genre)
    }

    @Test
    fun `choosing a genre does not change the name or the position`() = runBlocking {
        dao.insert(ArtistEntity("a", "Genesis", "Group", "GB", position = 4))

        dao.setGenre("a", "rock")

        val artist = dao.getById("a")!!
        assertEquals("Genesis", artist.name)
        assertEquals(4, artist.position)
        assertEquals("Group", artist.type)
    }

    // The genre suggested from MusicBrainz, kept apart from the one the user chose.

    @Test
    fun `an artist has no suggested genre to begin with, and one can be kept and taken away`() = runBlocking {
        dao.insert(ArtistEntity("a", "Genesis", null, null))
        assertNull(dao.getById("a")!!.autoGenre)

        dao.setAutoGenre("a", "progressive rock")
        assertEquals("progressive rock", dao.getById("a")!!.autoGenre)

        dao.setAutoGenre("a", null)
        assertNull(dao.getById("a")!!.autoGenre)
    }

    @Test
    fun `the suggested genre and the chosen genre do not touch each other`() = runBlocking {
        dao.insert(ArtistEntity("a", "Genesis", null, null))

        dao.setAutoGenre("a", "progressive rock")
        dao.setGenre("a", "rock")
        assertEquals("progressive rock", dao.getById("a")!!.autoGenre)
        assertEquals("rock", dao.getById("a")!!.genre)

        dao.setGenre("a", null)
        assertEquals("progressive rock", dao.getById("a")!!.autoGenre)

        dao.setGenre("a", "folk")
        dao.setAutoGenre("a", null)
        assertEquals("folk", dao.getById("a")!!.genre)
    }
}
