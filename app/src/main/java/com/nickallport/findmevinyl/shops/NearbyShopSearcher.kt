package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.Coordinates
import kotlinx.coroutines.CancellationException

class NearbyShopSearcher(
    private val requestPermission: suspend () -> Boolean,
    private val currentLocation: suspend () -> Coordinates?,
    private val searchAround: suspend (latitude: Double, longitude: Double) -> ShopSearchUiState
) {
    suspend fun findNearby(): ShopSearchUiState {
        if (!requestPermission()) {
            return ShopSearchUiState.Error(PERMISSION_MESSAGE)
        }

        val location = try {
            currentLocation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (location == null) {
            return ShopSearchUiState.Error(LOCATION_MESSAGE)
        }

        return searchAround(location.latitude, location.longitude)
    }

    private companion object {
        const val PERMISSION_MESSAGE = "Location permission is needed to find shops near you"
        const val LOCATION_MESSAGE = "Couldn't get your location. Check that location is turned on"
    }
}