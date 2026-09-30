package com.nickallport.findmevinyl.shops

import org.junit.Assert.assertEquals
import org.junit.Test

class ShopSorterTest {

    private fun shop(id: String, name: String, lat: Double, lon: Double) = Shop(
        id = id,
        name = name,
        category = ShopCategory.RECORD_SHOP,
        latitude = lat,
        longitude = lon,
        openingHours = null
    )

    @Test
    fun `shops are sorted by distance from the given point, nearest first`() {
        // Reference point: central London, 51.5074, -0.1278
        val shops = listOf(
            shop("paris", "Paris Record Shop", 48.8566, 2.3522),
            shop("nearby", "Nearby Record Shop", 51.5080, -0.1290),
            shop("edinburgh", "Edinburgh Record Shop", 55.9533, -3.1883)
        )

        val sorted = ShopSorter.sortByDistance(shops, fromLat = 51.5074, fromLon = -0.1278)

        assertEquals(
            listOf("nearby", "paris", "edinburgh"),
            sorted.map { it.id }
        )
    }
}