package com.nickallport.findmevinyl.shops

data class Shop(
    val id: String,
    val name: String,
    val category: ShopCategory,
    val latitude: Double,
    val longitude: Double,
    val openingHours: String?
)

enum class ShopCategory {
    RECORD_SHOP, CHARITY_SHOP, SECOND_HAND_SHOP
}

object ShopSorter {
    fun sortByDistance(shops: List<Shop>, fromLat: Double, fromLon: Double): List<Shop> {
        return shops.sortedBy {
            DistanceCalculator.distanceKm(fromLat, fromLon, it.latitude, it.longitude)
        }
    }
}
