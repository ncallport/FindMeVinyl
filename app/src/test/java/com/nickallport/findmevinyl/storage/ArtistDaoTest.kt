package com.nickallport.findmevinyl.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `a favourited artist can be retrieved`() {
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
    fun `removing a favourite deletes it`() {
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
}
