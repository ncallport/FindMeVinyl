package com.nickallport.findmevinyl

import androidx.compose.runtime.saveable.Saver
import com.google.gson.Gson
import androidx.compose.runtime.saveable.SaverScope

internal sealed class Screen {
    object Home : Screen()
    /** The Artists page: a search field and the favourite artists. */
    object Search : Screen()
    /** The results of an artist search. */
    data class ArtistResults(val query: String) : Screen()
    object WantList : Screen()
    object Shops : Screen()
    /** What to hide, as the user chooses. Reached from the front screen. */
    object Settings : Screen()
    data class Discography(
        val artistId: String,
        val artistName: String,
        val backTo: Screen
    ) : Screen()
    /** One shop, with the region it was found in, for its public holidays and the layout of its address. */
    data class ShopDetail(
        val shop: com.nickallport.findmevinyl.shops.Shop,
        val region: com.nickallport.findmevinyl.holidays.HolidayRegion?,
        /** The page it was opened from: the Shops page, or the favourite shops. */
        val backTo: Screen = Shops
    ) : Screen()
    /** The shops the user keeps as favourites. */
    object FavouriteShops : Screen()
    /** Adding a shop that OpenStreetMap does not have. [sharedText] is what Google Maps shared into the app, if anything. */
    data class AddShop(val sharedText: String?) : Screen()
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
    is Screen.ShopDetail -> backTo
    is Screen.FavouriteShops -> Screen.Shops
    is Screen.AddShop -> Screen.FavouriteShops
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
        is Screen.Settings -> listOf("settings")
        is Screen.ShopDetail ->
            listOf("shop", Gson().toJson(value.shop), value.region?.country.orEmpty(), value.region?.subdivision.orEmpty()) +
                save(value.backTo)
        is Screen.FavouriteShops -> listOf("favouriteshops")
        is Screen.AddShop -> listOf("addshop", value.sharedText.orEmpty())
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
        "settings" -> Screen.Settings
        "shop" -> runCatching {
            val shop = Gson().fromJson(value[1], com.nickallport.findmevinyl.shops.Shop::class.java)
            val country = value.getOrNull(2).orEmpty()
            val region = if (country.isEmpty()) null else
                com.nickallport.findmevinyl.holidays.HolidayRegion(country, value.getOrNull(3)?.ifEmpty { null })
            Screen.ShopDetail(shop, region, restore(value.drop(4)) ?: Screen.Shops)
        }.getOrNull()
        "favouriteshops" -> Screen.FavouriteShops
        "addshop" -> Screen.AddShop(value.getOrNull(1)?.ifEmpty { null })
        "discography" -> restore(value.drop(3))?.let {
            Screen.Discography(value[1], value[2], it)
        }
        "album" -> restore(value.drop(5))?.let {
            Screen.AlbumDetail(value[1], value[2], value[3], value[4], it)
        }
        else -> null
    }
}
