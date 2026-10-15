package com.nickallport.findmevinyl.nominatim

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Only there when the request asked for address details. */
data class NominatimAddressDto(
    @SerializedName("country_code") val countryCode: String? = null,
    /** The region of the country as an ISO 3166-2 code, such as GB-SCT. */
    @SerializedName("ISO3166-2-lvl4") val subdivisionCode: String? = null
)

data class NominatimResultDto(
    val lat: String,
    val lon: String,
    @SerializedName("display_name") val displayName: String?,
    val address: NominatimAddressDto? = null
)

data class NominatimReverseDto(
    @SerializedName("display_name") val displayName: String?,
    val address: NominatimAddressDto? = null
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

object NominatimMapper {
    private val gson = Gson()
    private val resultListType = object : com.google.gson.reflect.TypeToken<List<NominatimResultDto>>() {}.type

    fun parseFirstResult(json: String): Coordinates? {
        val results: List<NominatimResultDto> = gson.fromJson(json, resultListType)
        val first = results.firstOrNull() ?: return null

        return Coordinates(
            latitude = first.lat.toDouble(),
            longitude = first.lon.toDouble()
        )
    }
}