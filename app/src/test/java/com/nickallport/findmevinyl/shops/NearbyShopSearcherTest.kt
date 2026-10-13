package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.Coordinates
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NearbyShopSearcherTest {

    private val foundShops = ShopSearchUiState.Success(
        shops = emptyList(),
        placeName = "Your location",
        centerLatitude = 39.16,
        centerLongitude = -8.79
    )

    @Test
    fun `searches around the device location when permission is granted`() = runBlocking {
        var searchedAt: Pair<Double, Double>? = null
        val searcher = NearbyShopSearcher(
            requestPermission = { true },
            currentLocation = { Coordinates(39.16, -8.79) },
            searchAround = { lat, lon ->
                searchedAt = lat to lon
                foundShops
            }
        )

        val state = searcher.findNearby()

        assertEquals(foundShops, state)
        assertEquals(39.16 to -8.79, searchedAt)
    }

    @Test
    fun `reports a permission error without looking up the location or searching`() = runBlocking {
        var locationRequested = false
        var searched = false
        val searcher = NearbyShopSearcher(
            requestPermission = { false },
            currentLocation = {
                locationRequested = true
                Coordinates(0.0, 0.0)
            },
            searchAround = { _, _ ->
                searched = true
                foundShops
            }
        )

        val state = searcher.findNearby()

        assertEquals(
            ShopSearchUiState.Error("Location permission is needed to find shops near you"),
            state
        )
        assertEquals(false, locationRequested)
        assertEquals(false, searched)
    }

    @Test
    fun `reports an error when no location is available`() = runBlocking {
        var searched = false
        val searcher = NearbyShopSearcher(
            requestPermission = { true },
            currentLocation = { null },
            searchAround = { _, _ ->
                searched = true
                foundShops
            }
        )

        val state = searcher.findNearby()

        assertEquals(
            ShopSearchUiState.Error("Couldn't get your location. Check that location is turned on"),
            state
        )
        assertEquals(false, searched)
    }

    @Test
    fun `reports an error when the location lookup fails`() = runBlocking {
        val searcher = NearbyShopSearcher(
            requestPermission = { true },
            currentLocation = { throw IllegalStateException("boom") },
            searchAround = { _, _ -> foundShops }
        )

        val state = searcher.findNearby()

        assertEquals(
            ShopSearchUiState.Error("Couldn't get your location. Check that location is turned on"),
            state
        )
    }
}
