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
    val secondHandOnly: Boolean = false,
    /** The position is only that of the town, because the street was not found. */
    val approximatePosition: Boolean = false,
    /** What to ask a map app for, when the position alone does not find the shop: its name and address. */
    val mapQuery: String? = null
) {
    /** A shop the user added, because OpenStreetMap does not have it. */
    val isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

    companion object {
        const val CUSTOM_PREFIX = "custom/"
    }
}

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

    /** The address on one line, such as "R. Raul Proença 56a, 2500 Caldas da Rainha": the street, then the postcode and town. */
    fun toText(): String {
        val streetPart = listOf(street, houseNumber).filterNot { it.isNullOrBlank() }.joinToString(" ")
        val townPart = listOf(postcode, city).filterNot { it.isNullOrBlank() }.joinToString(" ")
        return listOf(streetPart, townPart).filter { it.isNotEmpty() }.joinToString(", ")
    }

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

    companion object {
        private val POSTCODE_LAST = setOf("GB", "IE", "US", "CA", "AU", "NZ", "ZA")
        private val NUMBER_FIRST = POSTCODE_LAST + "FR"
        private val POSTCODE_AND_TOWN = Regex("""^(\d{4}(?:-\d{3})?)\s+(.+)$""")

        /**
         * An address written on one line, as Google Maps gives it, split in parts: the last part after a comma is the
         * town, with a four digit (or 1234-567) postcode in front of it taken off, and everything before it is the street.
         * A line with no comma is all street.
         */
        fun fromText(text: String): ShopAddress {
            val parts = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return ShopAddress()
            if (parts.size == 1) return ShopAddress(street = parts[0])

            val town = parts.last()
            val match = POSTCODE_AND_TOWN.matchEntire(town)
            return ShopAddress(
                street = parts.dropLast(1).joinToString(", "),
                postcode = match?.groupValues?.get(1),
                city = match?.groupValues?.get(2) ?: town
            )
        }
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
