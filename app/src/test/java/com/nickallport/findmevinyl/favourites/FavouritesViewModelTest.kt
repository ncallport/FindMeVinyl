package com.nickallport.findmevinyl.favourites

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class FavouritesViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: FavouritesViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        viewModel = FavouritesViewModel(database.artistDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `returns favourites sorted by name ignoring case`() = runBlocking {
        database.artistDao().insert(ArtistEntity("1", "The Smiths", null, null))
        database.artistDao().insert(ArtistEntity("2", "roxy music", null, null))
        database.artistDao().insert(ArtistEntity("3", "ABC", null, null))

        val favourites = viewModel.loadFavourites()

        assertEquals(listOf("ABC", "roxy music", "The Smiths"), favourites.map { it.name })
        assertEquals(listOf("3", "2", "1"), favourites.map { it.id })
    }

    @Test
    fun `returns an empty list when there are no favourites`() = runBlocking {
        assertEquals(emptyList<Any>(), viewModel.loadFavourites())
    }

    private suspend fun favourite(id: String, name: String, position: Int = 0) =
        database.artistDao().insert(ArtistEntity(id, name, null, null, position))

    private suspend fun ids() = viewModel.loadFavourites().map { it.id }

    @Test
    fun `puts the artists in the order the user chose, whatever their names`() = runBlocking {
        favourite("a", "ABC", position = 2)
        favourite("b", "Bowie", position = 0)
        favourite("c", "Cure", position = 1)

        assertEquals(listOf("b", "c", "a"), ids())
    }

    @Test
    fun `artists with the same position are ordered by name`() = runBlocking {
        favourite("1", "The Smiths", position = 1)
        favourite("2", "roxy music", position = 1)
        favourite("3", "ABC", position = 0)

        assertEquals(listOf("3", "2", "1"), ids())
    }

    @Test
    fun `moving an artist up swaps it with the one above`() = runBlocking {
        favourite("a", "ABC", 0); favourite("b", "Bowie", 1); favourite("c", "Cure", 2)

        viewModel.moveUp("c")

        assertEquals(listOf("a", "c", "b"), ids())
    }

    @Test
    fun `moving an artist down swaps it with the one below`() = runBlocking {
        favourite("a", "ABC", 0); favourite("b", "Bowie", 1); favourite("c", "Cure", 2)

        viewModel.moveDown("a")

        assertEquals(listOf("b", "a", "c"), ids())
    }

    @Test
    fun `the first artist cannot move up and the last cannot move down`() = runBlocking {
        favourite("a", "ABC", 0); favourite("b", "Bowie", 1)

        viewModel.moveUp("a")
        viewModel.moveDown("b")

        assertEquals(listOf("a", "b"), ids())
    }

    @Test
    fun `moving an artist that is not a favourite changes nothing`() = runBlocking {
        favourite("a", "ABC", 0); favourite("b", "Bowie", 1)

        viewModel.moveUp("missing")
        viewModel.moveDown("missing")

        assertEquals(listOf("a", "b"), ids())
    }

    @Test
    fun `the first move works on artists that have never been ordered, starting from the name order`() = runBlocking {
        // All at position 0, as after the upgrade: shown as ABC, Bowie, Cure.
        favourite("c", "Cure"); favourite("a", "ABC"); favourite("b", "Bowie")

        viewModel.moveUp("c")

        assertEquals(listOf("a", "c", "b"), ids())
        // Every artist now has its own position, so later saves and moves are not ties.
        assertEquals(listOf(0, 1, 2), database.artistDao().getAllFavourites().sortedBy { it.position }.map { it.position })
    }

    @Test
    fun `a move is kept when the list is loaded again`() = runBlocking {
        favourite("a", "ABC", 0); favourite("b", "Bowie", 1)

        viewModel.moveDown("a")

        assertEquals(listOf("b", "a"), FavouritesViewModel(database.artistDao()).loadFavourites().map { it.id })
    }
}
