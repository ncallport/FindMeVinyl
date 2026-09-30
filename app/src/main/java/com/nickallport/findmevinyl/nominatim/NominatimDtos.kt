package com.nickallport.findmevinyl.nominatim

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class NominatimResultDto(
    val lat: String,
    val lon: String,
    @SerializedName("display_name") val displayName: String?
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