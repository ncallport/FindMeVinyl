package com.nickallport.findmevinyl

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

internal sealed class Screen {
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

/** Saves a [Screen] as a list of strings, so it fits in the saved-state Bundle. */
internal object ScreenSaver : Saver<Screen, List<String>> {

    override fun SaverScope.save(value: Screen): List<String> = when (value) {
        is Screen.Search -> listOf("search")
        is Screen.Favourites -> listOf("favourites")
        is Screen.WantList -> listOf("wantlist")
        is Screen.Shops -> listOf("shops")
        is Screen.Discography ->
            listOf("discography", value.artistId, value.artistName) + save(value.backTo)
    }

    override fun restore(value: List<String>): Screen? = when (value.firstOrNull()) {
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
