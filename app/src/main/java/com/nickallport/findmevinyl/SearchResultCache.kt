package com.nickallport.findmevinyl

import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import com.nickallport.findmevinyl.shops.ShopSearchUiState

/**
 * Holds the last search results in memory so they survive the activity being recreated on
 * rotation. Results can be large, so they are kept here and not in the saved-state Bundle.
 * The cache is empty after the process is killed.
 */
class SearchResultCache {

    /** The last finished artist search. Setting [ArtistSearchUiState.Loading] is ignored. */
    var artist: ArtistSearchUiState? = null
        set(value) {
            if (value !is ArtistSearchUiState.Loading) field = value
        }

    var shop: ShopSearchUiState? = null

    companion object {
        /** The instance the app uses. It lives as long as the process, so it outlasts the activity. */
        val shared = SearchResultCache()
    }
}
