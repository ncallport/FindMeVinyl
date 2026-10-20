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
    fun `lists only the albums that are owned or wanted`() = runBlocking {
        dao.insert(album("1", owned = true))
        dao.insert(album("2", wanted = true))
        dao.insert(album("3"))

        assertEquals(listOf("1", "2"), dao.getOwnedOrWanted().map { it.id }.sorted())
    }

    @Test
    fun `wanting an album at a place sets both together and leaves the owned flag`() = runBlocking {
        dao.insert(album("1"))

        assertEquals(1, dao.setWantedAt("1", 6))

        val saved = dao.getById("1")!!
        assertTrue(saved.wanted)
        assertEquals(6, saved.wantPosition)
        assertEquals(false, saved.owned)
    }

    @Test
    fun `formats and statuses are saved together and leave the flags alone`() = runBlocking {
        dao.insert(album("1", owned = true, wanted = false))
        assertNull(dao.getById("1")?.statuses)

        assertEquals(1, dao.setChecked("1", "CD", "Bootleg", checkedAt = 7))

        val saved = dao.getById("1")!!
        assertEquals("CD", saved.formats)
        assertEquals("Bootleg", saved.statuses)
        assertEquals(7L, saved.formatsCheckedAt)
        assertTrue(saved.owned)
    }

    @Test
    fun `saving only formats leaves the statuses as they were`() = runBlocking {
        dao.insert(album("1"))
        dao.setChecked("1", "CD", "Official", checkedAt = 7)

        dao.setFormats("1", "LP", checkedAt = 8)

        assertEquals("Official", dao.getById("1")?.statuses)
    }

    @Test
    fun `saved formats and their check time can be read back`() = runBlocking {
        dao.insert(album("1"))

        val changed = dao.setFormats("1", "12\" Vinyl|CD", checkedAt = 1_700_000_000_000)

        assertEquals(1, changed)
        val saved = dao.getById("1")
        assertEquals("12\" Vinyl|CD", saved?.formats)
        assertEquals(1_700_000_000_000, saved?.formatsCheckedAt)
    }

    @Test
    fun `saving formats leaves the owned and wanted flags alone`() = runBlocking {
        dao.insert(album("1", owned = true, wanted = true))

        dao.setFormats("1", "CD", checkedAt = 5)

        val saved = dao.getById("1")!!
        assertTrue(saved.owned)
        assertTrue(saved.wanted)
    }

    @Test
    fun `saving formats for an album that is not there changes nothing`() = runBlocking {
        assertEquals(0, dao.setFormats("missing", "CD", checkedAt = 5))
        assertNull(dao.getById("missing"))
    }

    @Test
    fun `an album has no formats until some are saved`() = runBlocking {
        dao.insert(album("1"))

        val saved = dao.getById("1")!!

        assertNull(saved.formats)
        assertNull(saved.formatsCheckedAt)
    }

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

    // The format the user wants an album on.

    private suspend fun wantedOn(format: String?) {
        database.albumDao().insert(AlbumEntity("w", "artist-1", "Desire", "STUDIO_ALBUM", "1976", owned = true, wanted = true))
        database.albumDao().setWantFormat("w", format)
    }

    @Test
    fun `the format an album is wanted on is saved, and null takes it away`() = runBlocking {
        wantedOn("LP")
        assertEquals("LP", database.albumDao().getById("w")!!.wantFormat)

        database.albumDao().setWantFormat("w", null)
        assertEquals(null, database.albumDao().getById("w")!!.wantFormat)
    }

    @Test
    fun `taking the want off an album forgets the format it was wanted on`() = runBlocking {
        wantedOn("LP")

        database.albumDao().setWanted("w", false)

        assertEquals(null, database.albumDao().getById("w")!!.wantFormat)
    }

    @Test
    fun `keeping the want keeps the format`() = runBlocking {
        wantedOn("LP")

        database.albumDao().setWanted("w", true)

        assertEquals("LP", database.albumDao().getById("w")!!.wantFormat)
    }

    @Test
    fun `clearing the wants of an artist forgets their formats`() = runBlocking {
        wantedOn("LP")

        database.albumDao().clearWantedForArtist("artist-1")

        assertEquals(null, database.albumDao().getById("w")!!.wantFormat)
    }

    @Test
    fun `the want list rows carry the format`() = runBlocking {
        database.artistDao().insert(com.nickallport.findmevinyl.storage.ArtistEntity("artist-1", "Bob Dylan", null, null))
        wantedOn("LP")

        assertEquals(listOf("LP"), database.albumDao().getWantedAlbums().map { it.wantFormat })
    }

    // How many albums an artist has owned and wanted.

    @Test
    fun `counts the owned and the wanted albums of each artist, an album both owned and wanted in both`() = runBlocking {
        dao.insert(AlbumEntity("1", "a", "One", "STUDIO_ALBUM", null, owned = true, wanted = false))
        dao.insert(AlbumEntity("2", "a", "Two", "STUDIO_ALBUM", null, owned = true, wanted = true))
        dao.insert(AlbumEntity("3", "a", "Three", "STUDIO_ALBUM", null, owned = false, wanted = true))
        dao.insert(AlbumEntity("4", "a", "Four", "STUDIO_ALBUM", null, owned = false, wanted = false))
        dao.insert(AlbumEntity("5", "b", "Five", "STUDIO_ALBUM", null, owned = true, wanted = false))

        val counts = dao.getAlbumCounts().associateBy { it.artistId }

        assertEquals(2 to 2, counts.getValue("a").owned to counts.getValue("a").wanted)
        assertEquals(1 to 0, counts.getValue("b").owned to counts.getValue("b").wanted)
    }

    @Test
    fun `an artist with no albums has no row of counts`() = runBlocking {
        assertEquals(emptyList<Any>(), dao.getAlbumCounts())
    }

    // A want pinned to a place.

    private suspend fun pinned(id: String, priority: Int?) {
        dao.insert(AlbumEntity(id, "a", "Album $id", "STUDIO_ALBUM", null, owned = false, wanted = true))
        dao.setWantPriority(id, priority)
    }

    @Test
    fun `the place a want is pinned to is saved, and null takes it away`() = runBlocking {
        pinned("1", 3)
        assertEquals(3, dao.getById("1")!!.wantPriority)

        dao.setWantPriority("1", null)
        assertEquals(null, dao.getById("1")!!.wantPriority)
    }

    @Test
    fun `taking the want off an album forgets its place`() = runBlocking {
        pinned("1", 3)

        dao.setWanted("1", false)

        assertEquals(null, dao.getById("1")!!.wantPriority)
    }

    @Test
    fun `keeping the want keeps the place`() = runBlocking {
        pinned("1", 3)

        dao.setWanted("1", true)

        assertEquals(3, dao.getById("1")!!.wantPriority)
    }

    @Test
    fun `clearing the wants of an artist forgets their places`() = runBlocking {
        pinned("1", 3)

        dao.clearWantedForArtist("a")

        assertEquals(null, dao.getById("1")!!.wantPriority)
    }

    @Test
    fun `wanting an album that was taken off does not pin it`() = runBlocking {
        pinned("1", 3)
        dao.setWanted("1", false)

        dao.markWanted("1")

        assertEquals(null, dao.getById("1")!!.wantPriority)
    }

    @Test
    fun `the want list rows carry the place`() = runBlocking {
        database.artistDao().insert(com.nickallport.findmevinyl.storage.ArtistEntity("a", "Abba", null, null))
        pinned("1", 3)
        pinned("2", null)

        assertEquals(mapOf("1" to 3, "2" to null), dao.getWantedAlbums().associate { it.albumId to it.wantPriority })
    }
}
