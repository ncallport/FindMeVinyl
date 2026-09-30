package com.nickallport.findmevinyl.overpass

import com.nickallport.findmevinyl.shops.ShopCategory
import org.junit.Assert
import org.junit.Test

class OverpassShopMapperTest {

    @Test
    fun `parses a node element using its direct coordinates`() {
        val json = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 123,
                  "lat": 51.5074,
                  "lon": -0.1278,
                  "tags": {
                    "shop": "music",
                    "name": "Sister Ray",
                    "opening_hours": "Mo-Sa 10:00-18:00"
                  }
                }
              ]
            }
        """.trimIndent()

        val result = OverpassShopMapper.parseResponse(json)

        Assert.assertEquals(1, result.size)
        Assert.assertEquals("Sister Ray", result[0].name)
        Assert.assertEquals(ShopCategory.RECORD_SHOP, result[0].category)
        Assert.assertEquals(51.5074, result[0].latitude, 0.0001)
        Assert.assertEquals(-0.1278, result[0].longitude, 0.0001)
        Assert.assertEquals("Mo-Sa 10:00-18:00", result[0].openingHours)
    }

    @Test
    fun `parses a way element using its center coordinates`() {
        val json = """
            {
              "elements": [
                {
                  "type": "way",
                  "id": 456,
                  "lat": null,
                  "lon": null,
                  "center": { "lat": 51.51, "lon": -0.13 },
                  "tags": {
                    "shop": "charity",
                    "name": "Oxfam"
                  }
                }
              ]
            }
        """.trimIndent()

        val result = OverpassShopMapper.parseResponse(json)

        Assert.assertEquals(1, result.size)
        Assert.assertEquals(ShopCategory.CHARITY_SHOP, result[0].category)
        Assert.assertEquals(51.51, result[0].latitude, 0.0001)
        Assert.assertEquals(-0.13, result[0].longitude, 0.0001)
        Assert.assertEquals(null, result[0].openingHours)
    }

    @Test
    fun `skips an element with no tags`() {
        val json = """
            {
              "elements": [
                { "type": "node", "id": 789, "lat": 51.5, "lon": -0.1, "tags": null }
              ]
            }
        """.trimIndent()

        val result = OverpassShopMapper.parseResponse(json)

        Assert.assertEquals(0, result.size)
    }
}