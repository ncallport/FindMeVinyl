package com.nickallport.findmevinyl

import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchResultCacheTest {

    @Test
    fun startsEmpty() {
        val cache = SearchResultCache()
        assertNull(cache.artist)
        assertNull(cache.shop)
    }

    @Test
    fun storesArtistStates() {
        val cache = SearchResultCache()
        listOf(
            ArtistSearchUiState.Idle,
            ArtistSearchUiState.Success(emptyList()),
            ArtistSearchUiState.Error("boom")
        ).forEach {
            cache.artist = it
            assertEquals(it, cache.artist)
        }
    }

    @Test
    fun loadingArtistStateIsNeverStored() {
        val cache = SearchResultCache()
        val done = ArtistSearchUiState.Error("boom")
        cache.artist = done

        cache.artist = ArtistSearchUiState.Loading

        assertEquals(done, cache.artist)
    }

    @Test
    fun remembersWhichQueryTheArtistResultsAreFor() {
        val cache = SearchResultCache()
        assertNull(cache.artistQuery)

        cache.artistQuery = "Genesis"

        assertEquals("Genesis", cache.artistQuery)
    }

    @Test
    fun storesShopStates() {
        val cache = SearchResultCache()
        listOf(
            ShopSearchUiState.TownNotFound("Nowhere"),
            ShopSearchUiState.Error("boom")
        ).forEach {
            cache.shop = it
            assertEquals(it, cache.shop)
        }
    }
}