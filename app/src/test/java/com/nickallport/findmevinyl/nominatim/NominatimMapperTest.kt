package com.nickallport.findmevinyl.nominatim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NominatimMapperTest {

    @Test
    fun `parses the first result's string coordinates into doubles`() {
        val json = """
            [
              {
                "lat": "51.5073509",
                "lon": "-0.1277583",
                "display_name": "London, Greater London, England, United Kingdom"
              }
            ]
        """.trimIndent()

        val result = NominatimMapper.parseFirstResult(json)

        assertEquals(51.5073509, result?.latitude ?: 0.0, 0.0000001)
        assertEquals(-0.1277583, result?.longitude ?: 0.0, 0.0000001)
    }

    @Test
    fun `returns null when the town isn't found`() {
        val json = "[]"

        val result = NominatimMapper.parseFirstResult(json)

        assertNull(result)
    }
}