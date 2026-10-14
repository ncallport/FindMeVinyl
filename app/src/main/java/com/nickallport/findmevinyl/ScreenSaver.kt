package com.nickallport.findmevinyl

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

internal sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object Favourites : Screen()
    object WantList : Screen()
    object Shops : Screen()
    data class Discography(
        val artistId: String,
        val artistName: String,
        val backTo: Screen
    ) : Screen()
}

/**
 * Where the Back button goes. From the front screen it leaves the app (null). From a discography it
 * goes where the user came from, and from every other page it goes to the front screen.
 */
internal fun Screen.backTarget(): Screen? = when (this) {
    is Screen.Home -> null
    is Screen.Discography -> backTo
    else -> Screen.Home
}

/** Saves a [Screen] as a list of strings, so it fits in the saved-state Bundle. */
internal object ScreenSaver : Saver<Screen, List<String>> {

    override fun SaverScope.save(value: Screen): List<String> = when (value) {
        is Screen.Home -> listOf("home")
        is Screen.Search -> listOf("search")
        is Screen.Favourites -> listOf("favourites")
        is Screen.WantList -> listOf("wantlist")
        is Screen.Shops -> listOf("shops")
        is Screen.Discography ->
            listOf("discography", value.artistId, value.artistName) + save(value.backTo)
    }

    override fun restore(value: List<String>): Screen? = when (value.firstOrNull()) {
        "home" -> Screen.Home
        "search" -> Screen.Search
        "favourites" -> Screen.Favourites
        "wantlist" -> Screen.WantList
        "shops" -> Screen.Shops
        "discography" -> restore(value.drop(3))?.let {
            Screen.Discography(value[1], value[2], it)
        }
        else -> null
    }
}
