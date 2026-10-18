package com.nickallport.findmevinyl.favourites

import android.content.SharedPreferences

/**
 * Remembers how the user likes the favourite artists shown: as a list with names (the default), or as a
 * grid of pictures. It is a choice of look, kept on the phone, and is not part of a backup.
 */
class FavouritesLayoutStore(private val prefs: SharedPreferences) {

    fun isGrid(): Boolean = try {
        prefs.getBoolean(KEY, false)
    } catch (e: ClassCastException) {
        false
    }

    fun setGrid(grid: Boolean) {
        prefs.edit().putBoolean(KEY, grid).apply()
    }

    /** Whether the favourites are grouped by genre, which works with the list and with the grid. Off to begin with. */
    fun isGroupedByGenre(): Boolean = try {
        prefs.getBoolean(GENRE_KEY, false)
    } catch (e: ClassCastException) {
        false
    }

    fun setGroupedByGenre(grouped: Boolean) {
        prefs.edit().putBoolean(GENRE_KEY, grouped).apply()
    }

    private companion object {
        const val KEY = "grid"
        const val GENRE_KEY = "grouped_by_genre"
    }
}
