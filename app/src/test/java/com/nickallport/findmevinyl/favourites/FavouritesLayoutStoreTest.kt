package com.nickallport.findmevinyl.favourites

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouritesLayoutStoreTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("favourites-layout-test", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `is the list until the grid is chosen`() {
        assertEquals(false, FavouritesLayoutStore(prefs).isGrid())
    }

    @Test
    fun `remembers the grid`() {
        FavouritesLayoutStore(prefs).setGrid(true)

        assertEquals(true, FavouritesLayoutStore(prefs).isGrid())
    }

    @Test
    fun `remembers a change back to the list`() {
        val store = FavouritesLayoutStore(prefs)
        store.setGrid(true)

        store.setGrid(false)

        assertEquals(false, FavouritesLayoutStore(prefs).isGrid())
    }

    @Test
    fun `a value of the wrong kind reads as the list`() {
        prefs.edit().putString("grid", "yes").commit()

        assertEquals(false, FavouritesLayoutStore(prefs).isGrid())
    }

    @Test
    fun `grouping by genre is off to begin with, and the choice is kept`() {
        val store = FavouritesLayoutStore(prefs)
        assertEquals(false, store.isGroupedByGenre())

        store.setGroupedByGenre(true)

        assertEquals(true, FavouritesLayoutStore(prefs).isGroupedByGenre())
    }

    @Test
    fun `grouping by genre and the grid are kept apart`() {
        val store = FavouritesLayoutStore(prefs)

        store.setGroupedByGenre(true)
        assertEquals(false, store.isGrid())

        store.setGrid(true)
        store.setGroupedByGenre(false)
        assertEquals(true, store.isGrid())
    }

    @Test
    fun `sorting from A to Z is off to begin with, and the choice is kept`() {
        val store = FavouritesLayoutStore(prefs)
        assertEquals(false, store.isSortedAz())

        store.setSortedAz(true)

        assertEquals(true, FavouritesLayoutStore(prefs).isSortedAz())
    }

    @Test
    fun `sorting, grouping by genre and the grid are kept apart`() {
        val store = FavouritesLayoutStore(prefs)

        store.setSortedAz(true)
        assertEquals(false, store.isGrid())
        assertEquals(false, store.isGroupedByGenre())

        store.setGrid(true)
        store.setGroupedByGenre(true)
        store.setSortedAz(false)
        assertEquals(true, store.isGrid())
        assertEquals(true, store.isGroupedByGenre())
    }

    @Test
    fun `a value of the wrong kind reads as not sorted`() {
        prefs.edit().putString("sorted_az", "yes").commit()

        assertEquals(false, FavouritesLayoutStore(prefs).isSortedAz())
    }
}
