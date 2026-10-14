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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull

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
    fun `an inserted album can be retrieved for its artist`() = runBlocking {
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
    fun `marking an album as owned persists the change`() = runBlocking {
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
    fun `inserting an album that already exists does not overwrite its owned flag`() = runBlocking {
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

    private fun album(id: String, owned: Boolean = false, wanted: Boolean = false) = AlbumEntity(
        id = id,
        artistId = "artist-1",
        title = "Album $id",
        category = "STUDIO_ALBUM",
        firstReleaseDate = null,
        owned = owned,
        wanted = wanted
    )

    @Test
    fun `inserting a list of albums saves all of them`() = runBlocking {
        dao.insertAllIfNotExist(listOf(album("1"), album("2"), album("3")))

        assertEquals(listOf("1", "2", "3"), dao.getAlbumsForArtist("artist-1").map { it.id }.sorted())
    }

    @Test
    fun `inserting a list leaves the owned and wanted flags of albums that already exist`() = runBlocking {
        dao.insert(album("1", owned = true))
        dao.insert(album("2", wanted = true))

        dao.insertAllIfNotExist(listOf(album("1"), album("2"), album("3")))

        val saved = dao.getAlbumsForArtist("artist-1").sortedBy { it.id }
        assertEquals(3, saved.size)
        assertTrue(saved[0].owned)
        assertTrue(saved[1].wanted)
        assertEquals(false, saved[2].owned)
    }

    @Test
    fun `marking an album as wanted persists the change`() = runBlocking {
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

        dao.setWanted("abc-123", true)

        val albums = dao.getAlbumsForArtist("artist-1")
        assertTrue(albums[0].wanted)
    }

    @Test
    fun `wanted albums are returned with their artist's name`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", null, null))
        dao.insert(
            AlbumEntity("a1", "artist-1", "Foxtrot", "STUDIO_ALBUM", "1972-10-06", owned = false, wanted = true)
        )
        dao.insert(
            AlbumEntity("a2", "artist-1", "Trespass", "STUDIO_ALBUM", "1970-10-23", owned = false, wanted = false)
        )
        dao.insert(
            AlbumEntity("a3", "artist-2", "Orphan", "STUDIO_ALBUM", null, owned = false, wanted = true)
        )

        val wanted = dao.getWantedAlbums()

        assertEquals(1, wanted.size)
        assertEquals("Foxtrot", wanted[0].albumTitle)
        assertEquals("Genesis", wanted[0].artistName)
    }
}
