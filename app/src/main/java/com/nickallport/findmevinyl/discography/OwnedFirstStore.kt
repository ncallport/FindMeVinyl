package com.nickallport.findmevinyl.discography

import android.content.SharedPreferences

/**
 * Remembers, for each artist, that the discography is shown with the owned albums first. Only artists
 * it is on for are kept, so turning it off forgets the artist.
 */
class OwnedFirstStore(private val prefs: SharedPreferences) {

    fun isOn(artistId: String): Boolean = try {
        prefs.getBoolean(artistId, false)
    } catch (e: ClassCastException) {
        false
    }

    fun set(artistId: String, on: Boolean) {
        prefs.edit().apply { if (on) putBoolean(artistId, true) else remove(artistId) }.apply()
    }
}
