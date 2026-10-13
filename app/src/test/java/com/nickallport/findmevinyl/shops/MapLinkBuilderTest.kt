package com.nickallport.findmevinyl.shops

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLinkBuilderTest {

    private fun shop(name: String, lat: Double = 51.5, lon: Double = -0.1) = Shop(
        id = "1", name = name, category = ShopCategory.RECORD_SHOP,
        latitude = lat, longitude = lon, openingHours = null
    )

    @Test
    fun `builds a geo link with the position and the shop name as the label`() {
        assertEquals(
            "geo:51.500000,-0.100000?q=51.500000,-0.100000(Sister%20Ray)",
            MapLinkBuilder.geoUri(shop("Sister Ray"))
        )
    }

    @Test
    fun `encodes characters that would break the link`() {
        assertEquals(
            "geo:51.500000,-0.100000?q=51.500000,-0.100000(Rock%20%26%20Roll)",
            MapLinkBuilder.geoUri(shop("Rock & Roll"))
        )
    }

    @Test
    fun `removes brackets from the name`() {
        assertEquals(
            "geo:51.500000,-0.100000?q=51.500000,-0.100000(Sounds%20Lisbon)",
            MapLinkBuilder.geoUri(shop("Sounds (Lisbon)"))
        )
    }

    @Test
    fun `encodes accented letters`() {
        assertEquals(
            "geo:51.500000,-0.100000?q=51.500000,-0.100000(Caf%C3%A9%20Discos)",
            MapLinkBuilder.geoUri(shop("Café Discos"))
        )
    }

    @Test
    fun `uses a dot as the decimal separator whatever the phone's language`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pt-PT"))

            assertEquals(
                "geo:39.743700,-8.807100?q=39.743700,-8.807100(Sister%20Ray)",
                MapLinkBuilder.geoUri(shop("Sister Ray", lat = 39.7437, lon = -8.8071))
            )
        } finally {
            Locale.setDefault(original)
        }
    }
}