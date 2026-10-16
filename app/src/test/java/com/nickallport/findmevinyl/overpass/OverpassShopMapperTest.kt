package com.nickallport.findmevinyl.overpass

import com.nickallport.findmevinyl.shops.ShopAddress
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

    private fun shopWith(tags: String) = OverpassShopMapper.parseResponse(
        """{ "elements": [ { "type": "node", "id": 1, "lat": 38.71, "lon": -9.14, "tags": { "shop": "music", "name": "Louie Louie", $tags } } ] }"""
    ).single()

    @Test
    fun `reads the address, phone, website and email of a shop`() {
        val shop = shopWith(
            """"addr:street": "Escadinhas do Santo Espírito da Pedreira", "addr:housenumber": "3", "addr:postcode": "1100-225",
               "addr:city": "Lisboa", "addr:country": "PT", "phone": "+351 213 472 232",
               "website": "https://www.louielouie.biz/", "email": "louielouie.lisboa@gmail.com""""
        )

        Assert.assertEquals(
            ShopAddress("Escadinhas do Santo Espírito da Pedreira", "3", "1100-225", "Lisboa", "PT"),
            shop.address
        )
        Assert.assertEquals("+351 213 472 232", shop.phone)
        Assert.assertEquals("https://www.louielouie.biz/", shop.website)
        Assert.assertEquals("louielouie.lisboa@gmail.com", shop.email)
    }

    @Test
    fun `reads the contact tags when the plain ones are missing`() {
        val shop = shopWith(
            """"contact:phone": "+44 20 7123 4567", "contact:website": "https://example.com", "contact:email": "hi@example.com""""
        )

        Assert.assertEquals("+44 20 7123 4567", shop.phone)
        Assert.assertEquals("https://example.com", shop.website)
        Assert.assertEquals("hi@example.com", shop.email)
    }

    @Test
    fun `the plain tag wins over the contact one`() {
        val shop = shopWith(""""phone": "111 111 111", "contact:phone": "222 222 222"""")

        Assert.assertEquals("111 111 111", shop.phone)
    }

    @Test
    fun `a shop with none of these has none of them`() {
        val shop = shopWith(""""opening_hours": "Mo-Sa 10:00-18:00"""")

        Assert.assertNull(shop.phone)
        Assert.assertNull(shop.website)
        Assert.assertNull(shop.email)
        Assert.assertNull(shop.address)
    }

    @Test
    fun `blank values are taken as missing`() {
        val shop = shopWith(""""phone": "  ", "website": "", "addr:street": " """")

        Assert.assertNull(shop.phone)
        Assert.assertNull(shop.website)
        Assert.assertNull(shop.address)
    }

    @Test
    fun `a shop that is only second hand says so`() {
        val shop = shopWith(""""second_hand": "only"""")

        Assert.assertEquals(true, shop.secondHandOnly)
    }

    @Test
    fun `a shop id says what kind of element it is, since a node and a way can share a number`() {
        val json = """
            { "elements": [
              { "type": "node", "id": 5, "lat": 1.0, "lon": 2.0, "tags": { "shop": "music", "name": "A" } },
              { "type": "way", "id": 5, "center": { "lat": 1.0, "lon": 2.0 }, "tags": { "shop": "music", "name": "B" } }
            ] }
        """.trimIndent()

        val ids = OverpassShopMapper.parseResponse(json).map { it.id }

        Assert.assertEquals(listOf("node/5", "way/5"), ids)
    }

    @Test
    fun `an element with no type keeps its plain number as its id`() {
        val json = """{ "elements": [ { "id": 7, "lat": 1.0, "lon": 2.0, "tags": { "shop": "music", "name": "A" } } ] }"""

        Assert.assertEquals("7", OverpassShopMapper.parseResponse(json).single().id)
    }
}
