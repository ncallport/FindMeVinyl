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

@RunWith(RobolectricTestRunner::class)
class ArtistImageDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ArtistImageDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.artistImageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a saved picture can be read back with the time it was checked`() = runBlocking {
        dao.save(ArtistImageEntity("artist-1", "https://commons.test/a.jpg", checkedAt = 1_700_000_000_000))

        val saved = dao.get("artist-1")

        assertEquals("https://commons.test/a.jpg", saved?.imageUrl)
        assertEquals(1_700_000_000_000, saved?.checkedAt)
    }

    @Test
    fun `an artist checked and found to have no picture is stored with no address`() = runBlocking {
        dao.save(ArtistImageEntity("artist-1", null, checkedAt = 5))

        val saved = dao.get("artist-1")

        assertEquals("artist-1", saved?.artistId)
        assertNull(saved?.imageUrl)
        assertEquals(5L, saved?.checkedAt)
    }

    @Test
    fun `an artist that was never checked has nothing stored`() = runBlocking {
        assertNull(dao.get("artist-1"))
    }

    @Test
    fun `saving again replaces what was stored`() = runBlocking {
        dao.save(ArtistImageEntity("artist-1", "https://commons.test/old.jpg", checkedAt = 1))

        dao.save(ArtistImageEntity("artist-1", "https://commons.test/new.jpg", checkedAt = 2))

        val saved = dao.get("artist-1")
        assertEquals("https://commons.test/new.jpg", saved?.imageUrl)
        assertEquals(2L, saved?.checkedAt)
    }

    @Test
    fun `pictures of different artists are kept apart`() = runBlocking {
        dao.save(ArtistImageEntity("a", "https://commons.test/a.jpg", 1))
        dao.save(ArtistImageEntity("b", null, 2))

        assertEquals("https://commons.test/a.jpg", dao.get("a")?.imageUrl)
        assertNull(dao.get("b")?.imageUrl)
    }
}
