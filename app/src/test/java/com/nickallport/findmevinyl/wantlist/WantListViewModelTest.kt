package com.nickallport.findmevinyl.wantlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WantListViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: WantListViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        viewModel = WantListViewModel(database.albumDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun wantedAlbum(id: String, artistId: String, title: String, date: String?) {
        database.albumDao().insert(
            AlbumEntity(id, artistId, title, "STUDIO_ALBUM", date, owned = false, wanted = true)
        )
    }

    @Test
    fun `orders by artist name ignoring case, then release date with undated last`() = runBlocking {
        database.artistDao().insert(ArtistEntity("r", "roxy music", null, null))
        database.artistDao().insert(ArtistEntity("a", "ABC", null, null))
        wantedAlbum("1", "r", "Avalon", "1982-05-31")
        wantedAlbum("2", "r", "Undated", null)
        wantedAlbum("3", "r", "Roxy Music", "1972-06-16")
        wantedAlbum("4", "a", "Lexicon of Love", "1982-06-21")

        val wanted = viewModel.loadWantList()

        assertEquals(
            listOf("Lexicon of Love", "Roxy Music", "Avalon", "Undated"),
            wanted.map { it.albumTitle }
        )
    }

    @Test
    fun `returns an empty list when nothing is wanted`() = runBlocking {
        assertEquals(emptyList<Any>(), viewModel.loadWantList())
    }

    @Test
    fun `export text groups wanted albums under their artist`() = runBlocking {
        database.artistDao().insert(ArtistEntity("r", "roxy music", null, null))
        database.artistDao().insert(ArtistEntity("a", "ABC", null, null))
        wantedAlbum("1", "r", "Avalon", "1982-05-31")
        wantedAlbum("2", "r", "Roxy Music", "1972-06-16")
        wantedAlbum("3", "a", "Lexicon of Love", "1982-06-21")

        val expected = """
            My vinyl want list:

            ABC
            - Lexicon of Love

            roxy music
            - Roxy Music
            - Avalon
        """.trimIndent()

        assertEquals(expected, viewModel.exportText())
    }
}