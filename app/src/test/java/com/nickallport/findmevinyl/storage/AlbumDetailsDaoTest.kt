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
class AlbumDetailsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AlbumDetailsDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.albumDetailsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saved details can be read back with the time they were fetched`() = runBlocking {
        dao.save(AlbumDetailsEntity("album-1", """{"label":"Charisma"}""", fetchedAt = 1_700_000_000_000))

        val saved = dao.get("album-1")

        assertEquals("""{"label":"Charisma"}""", saved?.json)
        assertEquals(1_700_000_000_000, saved?.fetchedAt)
    }

    @Test
    fun `an album that was never fetched has nothing stored`() = runBlocking {
        assertNull(dao.get("album-1"))
    }

    @Test
    fun `saving again replaces what was stored`() = runBlocking {
        dao.save(AlbumDetailsEntity("album-1", "old", 1))

        dao.save(AlbumDetailsEntity("album-1", "new", 2))

        assertEquals("new", dao.get("album-1")?.json)
        assertEquals(2L, dao.get("album-1")?.fetchedAt)
    }

    @Test
    fun `details of different albums are kept apart`() = runBlocking {
        dao.save(AlbumDetailsEntity("a", "one", 1))
        dao.save(AlbumDetailsEntity("b", "two", 2))

        assertEquals("one", dao.get("a")?.json)
        assertEquals("two", dao.get("b")?.json)
    }
}
