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

/** The genres MusicBrainz lists for an artist, kept so they are not asked for every time. */
@RunWith(RobolectricTestRunner::class)
class ArtistGenresDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.artistGenresDao()

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
    fun `an artist nothing is kept for has no genres record`() = runBlocking {
        assertNull(dao.get("a"))
    }

    @Test
    fun `keeps the genres and when they were asked for`() = runBlocking {
        dao.save(ArtistGenresEntity("a", """[{"name":"post-punk","count":11}]""", 42L))

        assertEquals(ArtistGenresEntity("a", """[{"name":"post-punk","count":11}]""", 42L), dao.get("a"))
    }

    @Test
    fun `a newer answer replaces the older one`() = runBlocking {
        dao.save(ArtistGenresEntity("a", "[]", 1L))

        dao.save(ArtistGenresEntity("a", """[{"name":"rock","count":2}]""", 9L))

        assertEquals(9L, dao.get("a")!!.checkedAt)
    }

    @Test
    fun `each artist has its own record`() = runBlocking {
        dao.save(ArtistGenresEntity("a", "[]", 1L))
        dao.save(ArtistGenresEntity("b", "[]", 2L))

        assertEquals(1L, dao.get("a")!!.checkedAt)
        assertEquals(2L, dao.get("b")!!.checkedAt)
    }
}
