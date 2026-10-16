package com.nickallport.findmevinyl.shops

data class Shop(
    val id: String,
    val name: String,
    val category: ShopCategory,
    val latitude: Double,
    val longitude: Double,
    val openingHours: String?,
    val address: ShopAddress? = null,
    val phone: String? = null,
    val website: String? = null,
    val email: String? = null,
    /** OpenStreetMap says the shop sells nothing new (`second_hand=only`). */
    val secondHandOnly: Boolean = false
)

/** The parts of a shop's address that OpenStreetMap has. Any of them can be missing. */
data class ShopAddress(
    val street: String? = null,
    val houseNumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val country: String? = null
) {
    val isEmpty: Boolean
        get() = listOf(street, houseNumber, postcode, city).all { it.isNullOrBlank() }

    /**
     * The address as one or two lines, in the order that is usual in the country: the number before the street
     * and the postcode after the town in Britain, Ireland, the United States and a few more, the number before
     * the street and the postcode first in France, and the street first with the postcode first elsewhere.
     * The country is the one on the address, or else [fallbackCountry], the country of the search. With neither,
     * the street comes first.
     */
    fun lines(fallbackCountry: String?): List<String> {
        val code = (country ?: fallbackCountry)?.trim()?.uppercase()
        val numberFirst = code in NUMBER_FIRST
        val postcodeLast = code in POSTCODE_LAST

        val streetLine = (if (numberFirst) listOf(houseNumber, street) else listOf(street, houseNumber))
            .filterNot { it.isNullOrBlank() }.joinToString(" ")
        val townLine = (if (postcodeLast) listOf(city, postcode) else listOf(postcode, city))
            .filterNot { it.isNullOrBlank() }.joinToString(" ")
        return listOf(streetLine, townLine).filter { it.isNotBlank() }
    }

    private companion object {
        val POSTCODE_LAST = setOf("GB", "IE", "US", "CA", "AU", "NZ", "ZA")
        val NUMBER_FIRST = POSTCODE_LAST + "FR"
    }
}

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
