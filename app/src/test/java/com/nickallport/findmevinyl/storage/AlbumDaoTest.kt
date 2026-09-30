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
class AlbumDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AlbumDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.albumDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `an inserted album can be retrieved for its artist`() {
        val album = AlbumEntity(
            id = "abc-123",
            artistId = "artist-1",
            title = "The Dark Side of the Moon",
            category = "STUDIO_ALBUM",
            firstReleaseDate = "1973-03-01",
            owned = false,
            wanted = false
        )

        dao.insert(album)

        val albums = dao.getAlbumsForArtist("artist-1")
        assertEquals(1, albums.size)
        assertEquals("The Dark Side of the Moon", albums[0].title)
    }

    @Test
    fun `marking an album as owned persists the change`() {
        val album = AlbumEntity(
            id = "abc-123",
            artistId = "artist-1",
            title = "The Dark Side of the Moon",
            category = "STUDIO_ALBUM",
            firstReleaseDate = "1973-03-01",
            owned = false,
            wanted = false
        )
        dao.insert(album)

        dao.setOwned("abc-123", true)

        val albums = dao.getAlbumsForArtist("artist-1")
        assertTrue(albums[0].owned)
    }

    @Test
    fun `inserting an album that already exists does not overwrite its owned flag`() {
        val original = AlbumEntity(
            id = "abc-123",
            artistId = "artist-1",
            title = "The Dark Side of the Moon",
            category = "STUDIO_ALBUM",
            firstReleaseDate = "1973-03-01",
            owned = true,
            wanted = false
        )
        dao.insert(original)

        val refetched = original.copy(owned = false)
        dao.insertIfNotExists(refetched)

        val albums = dao.getAlbumsForArtist("artist-1")
        assertTrue(albums[0].owned)
    }
}
