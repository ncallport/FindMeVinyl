package com.nickallport.findmevinyl.overpass

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertFalse

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

    @Test
    fun `asks the server for a short timeout`() {
        val query = OverpassQueryBuilder.shopsNear(51.5074, -0.1278, 5000)

        assertTrue(query.contains("[timeout:25]"))
    }

    @Test
    fun `searches nodes, ways and relations`() {
        val query = OverpassQueryBuilder.shopsNear(51.5074, -0.1278, 5000)

        assertTrue(query.contains("nwr[shop=music]"))
        assertTrue(query.contains("nwr[shop=second_hand]"))
        assertFalse(query.contains("node["))
    }
}
