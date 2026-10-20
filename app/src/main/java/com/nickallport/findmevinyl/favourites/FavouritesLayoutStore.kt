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

    private companion object {
        const val KEY = "grid"
    }
}
