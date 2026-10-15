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
class ArtistUserPhotoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ArtistUserPhotoDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.artistUserPhotoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a saved photo can be found by its artist`() = runBlocking {
        dao.save(ArtistUserPhotoEntity("a", "a-1.jpg", 100))

        assertEquals(ArtistUserPhotoEntity("a", "a-1.jpg", 100), dao.get("a"))
    }

    @Test
    fun `an artist with no photo gives nothing`() = runBlocking {
        assertNull(dao.get("a"))
    }

    @Test
    fun `saving again replaces the photo of that artist only`() = runBlocking {
        dao.save(ArtistUserPhotoEntity("a", "a-1.jpg", 100))
        dao.save(ArtistUserPhotoEntity("b", "b-1.jpg", 100))

        dao.save(ArtistUserPhotoEntity("a", "a-2.jpg", 200))

        assertEquals("a-2.jpg", dao.get("a")?.fileName)
        assertEquals("b-1.jpg", dao.get("b")?.fileName)
    }

    @Test
    fun `deleting takes the photo of that artist away and says how many went`() = runBlocking {
        dao.save(ArtistUserPhotoEntity("a", "a-1.jpg", 100))
        dao.save(ArtistUserPhotoEntity("b", "b-1.jpg", 100))

        assertEquals(1, dao.delete("a"))

        assertNull(dao.get("a"))
        assertEquals("b-1.jpg", dao.get("b")?.fileName)
        assertEquals(0, dao.delete("a"))
    }
}
