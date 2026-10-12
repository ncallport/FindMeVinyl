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
}