package com.nickallport.findmevinyl.overpass

import com.google.gson.Gson
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory

data class OverpassResponse(
    val elements: List<OverpassElementDto>
)

data class OverpassElementDto(
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenterDto?,
    val tags: Map<String, String>?
)

data class OverpassCenterDto(
    val lat: Double,
    val lon: Double
)

object OverpassShopMapper {
    private val gson = Gson()

    fun parseResponse(json: String): List<Shop> {
        val response = gson.fromJson(json, OverpassResponse::class.java)
        return response.elements
            .mapNotNull { it.toShopOrNull() }
    }

    private fun OverpassElementDto.toShopOrNull(): Shop? {
        val elementTags = tags ?: return null
        val category = categoryFor(elementTags["shop"]) ?: return null
        val resolvedLat = lat ?: center?.lat ?: return null
        val resolvedLon = lon ?: center?.lon ?: return null

        return Shop(
            id = id.toString(),
            name = elementTags["name"] ?: "Unnamed shop",
            category = category,
            latitude = resolvedLat,
            longitude = resolvedLon,
            openingHours = elementTags["opening_hours"]
        )
    }

    private fun categoryFor(shopTag: String?): ShopCategory? {
        return when (shopTag) {
            "music", "record" -> ShopCategory.RECORD_SHOP
            "charity" -> ShopCategory.CHARITY_SHOP
            "second_hand" -> ShopCategory.SECOND_HAND_SHOP
            else -> null
        }
    }
}