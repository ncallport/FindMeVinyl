package com.nickallport.findmevinyl.photon

/** What Photon answers, as GeoJSON. Every field can be missing. */
data class PhotonResponseDto(val features: List<PhotonFeatureDto>? = null)

data class PhotonFeatureDto(
    val properties: PhotonPropertiesDto? = null,
    val geometry: PhotonGeometryDto? = null
)

data class PhotonPropertiesDto(
    val name: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    val countrycode: String? = null
)

/** [coordinates] are longitude first, then latitude. */
data class PhotonGeometryDto(val coordinates: List<Double>? = null)

/** A place offered while the user types: how it reads in the list, and where it is. */
data class PlaceSuggestion(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    /** ISO 3166 country code, such as PT. Null when Photon did not say. */
    val countryCode: String?
)

object PhotonMapper {
    fun suggestions(response: PhotonResponseDto): List<PlaceSuggestion> =
        response.features.orEmpty().mapNotNull { feature ->
            val properties = feature.properties ?: return@mapNotNull null
            val name = properties.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val coordinates = feature.geometry?.coordinates?.takeIf { it.size >= 2 } ?: return@mapNotNull null
            // The county and state tell two towns of one name apart: "Newport, Wales" or "Newport, Isle of Wight".
            val label = listOf(name, properties.county, properties.state, properties.country)
                .mapNotNull { it?.trim()?.takeIf { part -> part.isNotEmpty() } }
                .distinct()
                .joinToString(", ")
            PlaceSuggestion(
                label = label,
                latitude = coordinates[1],
                longitude = coordinates[0],
                countryCode = properties.countrycode?.trim()?.takeIf { it.isNotEmpty() }
            )
        }.distinctBy { it.label }
}
