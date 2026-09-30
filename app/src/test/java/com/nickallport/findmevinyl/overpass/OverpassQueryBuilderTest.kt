package com.nickallport.findmevinyl.overpass

import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassQueryBuilderTest {

    @Test
    fun `builds a query containing the shop tags, coordinates, and radius`() {
        val query = OverpassQueryBuilder.shopsNear(
            latitude = 51.5074,
            longitude = -0.1278,
            radiusMeters = 5000
        )

        assertTrue(query.contains("shop=music"))
        assertTrue(query.contains("shop=record"))
        assertTrue(query.contains("shop=charity"))
        assertTrue(query.contains("shop=second_hand"))
        assertTrue(query.contains("51.5074"))
        assertTrue(query.contains("-0.1278"))
        assertTrue(query.contains("5000"))
    }
}
