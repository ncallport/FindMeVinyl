package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.nominatim.NominatimAddressDto
import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.nominatim.NominatimResultDto
import com.nickallport.findmevinyl.nominatim.NominatimReverseDto
import com.nickallport.findmevinyl.overpass.OverpassApi
import com.nickallport.findmevinyl.overpass.OverpassQueryBuilder
import com.nickallport.findmevinyl.overpass.OverpassResponse
import com.nickallport.findmevinyl.overpass.OverpassShopMapper.toShopOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

data class PlaceCandidate(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Where the place is, for public holidays. Null when Nominatim did not say. */
    val region: HolidayRegion? = null
)

sealed class ShopSearchUiState {
    data class Success(
        val shops: List<Shop>,
        val placeName: String,
        val centerLatitude: Double,
        val centerLongitude: Double,
        val otherPlaces: List<PlaceCandidate> = emptyList(),
        /** The country and region the shops are in, so public holidays can be looked up. Null when unknown. */
        val region: HolidayRegion? = null
    ) : ShopSearchUiState()

    data class TownNotFound(val town: String) : ShopSearchUiState()
    data class Error(val message: String) : ShopSearchUiState()
}

class ShopSearchViewModel(
    private val nominatimApi: NominatimApi,
    private val overpassApi: OverpassApi,
    private val retryDelayMillis: Long = 1500
) {
    suspend fun searchTown(town: String, radiusMeters: Int = 5000): ShopSearchUiState {
        return try {
            val matches = nominatimApi.search(query = town, limit = MAX_PLACE_MATCHES)
            val geocoded = matches.firstOrNull()
                ?: return ShopSearchUiState.TownNotFound(town)
            val latitude = geocoded.lat.toDouble()
            val longitude = geocoded.lon.toDouble()
            val placeName = geocoded.displayName ?: town

            findShops(
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                placeName = placeName
            ).copy(
                otherPlaces = alternativesTo(placeName, latitude, longitude, matches.drop(1)),
                region = regionOf(geocoded.address)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShopSearchUiState.Error(e.message ?: "Something went wrong")
        }
    }

    suspend fun searchAround(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 5000
    ): ShopSearchUiState {
        return try {
            val result = findShops(latitude, longitude, radiusMeters, placeName = DEFAULT_PLACE_NAME)
            val place = lookUpPlace(latitude, longitude)
            result.copy(placeName = place?.displayName ?: DEFAULT_PLACE_NAME, region = regionOf(place?.address))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShopSearchUiState.Error(e.message ?: "Something went wrong")
        }
    }

    suspend fun searchPlace(
        place: PlaceCandidate,
        otherPlaces: List<PlaceCandidate> = emptyList(),
        radiusMeters: Int = 5000
    ): ShopSearchUiState {
        return try {
            findShops(place.latitude, place.longitude, radiusMeters, place.name)
                .copy(otherPlaces = otherPlaces, region = place.region)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShopSearchUiState.Error(e.message ?: "Something went wrong")
        }
    }

    private fun alternativesTo(
        chosenName: String,
        chosenLatitude: Double,
        chosenLongitude: Double,
        others: List<NominatimResultDto>
    ): List<PlaceCandidate> {
        val kept = mutableListOf<PlaceCandidate>()
        val positionsToAvoid = mutableListOf(chosenLatitude to chosenLongitude)

        for (result in others) {
            val name = result.displayName ?: continue
            val latitude = result.lat.toDoubleOrNull() ?: continue
            val longitude = result.lon.toDoubleOrNull() ?: continue

            if (name == chosenName || kept.any { it.name == name }) continue

            val tooClose = positionsToAvoid.any { (otherLat, otherLon) ->
                DistanceCalculator.distanceKm(otherLat, otherLon, latitude, longitude) <
                        MIN_ALTERNATIVE_DISTANCE_KM
            }
            if (tooClose) continue

            kept.add(PlaceCandidate(name, latitude, longitude, regionOf(result.address)))
            positionsToAvoid.add(latitude to longitude)
        }
        return kept
    }

    private suspend fun lookUpPlace(latitude: Double, longitude: Double): NominatimReverseDto? {
        return try {
            nominatimApi.reverse(latitude, longitude)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun regionOf(address: NominatimAddressDto?): HolidayRegion? {
        val country = address?.countryCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return HolidayRegion(country.uppercase(), address.subdivisionCode?.trim()?.takeIf { it.isNotEmpty() }?.uppercase())
    }

    private suspend fun findShops(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        placeName: String
    ): ShopSearchUiState.Success {
        val query = OverpassQueryBuilder.shopsNear(latitude, longitude, radiusMeters)
        val response = queryWithRetry(query)

        val shops = response.elements.mapNotNull { it.toShopOrNull() }
        return ShopSearchUiState.Success(
            shops = ShopSorter.sortByDistance(shops, latitude, longitude),
            placeName = placeName,
            centerLatitude = latitude,
            centerLongitude = longitude
        )
    }

    private suspend fun queryWithRetry(query: String): OverpassResponse {
        var attempt = 1
        while (true) {
            try {
                return overpassApi.query(query.toRequestBody("text/plain".toMediaType()))
            } catch (e: HttpException) {
                if (attempt >= MAX_ATTEMPTS || e.code() !in BUSY_CODES) throw e
                attempt++
                delay(retryDelayMillis)
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MAX_PLACE_MATCHES = 5
        const val MIN_ALTERNATIVE_DISTANCE_KM = 10.0
        const val DEFAULT_PLACE_NAME = "Your location"
        val BUSY_CODES = setOf(429, 502, 503, 504)
    }
}
