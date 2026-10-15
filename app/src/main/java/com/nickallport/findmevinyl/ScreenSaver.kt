package com.nickallport.findmevinyl

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

internal sealed class Screen {
    object Home : Screen()
    /** The Artists page: a search field and the favourite artists. */
    object Search : Screen()
    /** The results of an artist search. */
    data class ArtistResults(val query: String) : Screen()
    object WantList : Screen()
    object Shops : Screen()
    data class Discography(
        val artistId: String,
        val artistName: String,
        val backTo: Screen
    ) : Screen()
    /** One album: its tracklist and details. [backTo] is the discography it was opened from. */
    data class AlbumDetail(
        val albumId: String,
        val title: String,
        val artistId: String,
        val artistName: String,
        val backTo: Screen
    ) : Screen()
}

/**
 * Where the Back button goes. From the front screen it leaves the app (null). From a discography it
 * goes where the user came from, and an album page goes to its discography. Every other page goes
 * to the front screen.
 */
internal fun Screen.backTarget(): Screen? = when (this) {
    is Screen.Home -> null
    is Screen.Discography -> backTo
    is Screen.AlbumDetail -> backTo
    is Screen.ArtistResults -> Screen.Search
    else -> Screen.Home
}

/** Saves a [Screen] as a list of strings, so it fits in the saved-state Bundle. */
internal object ScreenSaver : Saver<Screen, List<String>> {

    override fun SaverScope.save(value: Screen): List<String> = when (value) {
        is Screen.Home -> listOf("home")
        is Screen.Search -> listOf("search")
        is Screen.ArtistResults -> listOf("results", value.query)
        is Screen.WantList -> listOf("wantlist")
        is Screen.Shops -> listOf("shops")
        is Screen.Discography ->
            listOf("discography", value.artistId, value.artistName) + save(value.backTo)
        is Screen.AlbumDetail ->
            listOf("album", value.albumId, value.title, value.artistId, value.artistName) + save(value.backTo)
    }

    override fun restore(value: List<String>): Screen? = when (value.firstOrNull()) {
        "home" -> Screen.Home
        "search" -> Screen.Search
        "results" -> value.getOrNull(1)?.let { Screen.ArtistResults(it) }
        "wantlist" -> Screen.WantList
        "shops" -> Screen.Shops
        "discography" -> restore(value.drop(3))?.let {
            Screen.Discography(value[1], value[2], it)
        }
        "album" -> restore(value.drop(5))?.let {
            Screen.AlbumDetail(value[1], value[2], value[3], value[4], it)
        }
        else -> null
    }
}
