package com.nickallport.findmevinyl.shops

import org.junit.Assert.assertEquals
import org.junit.Test

class ShopAddressTest {

    @Test
    fun `a Portuguese address puts the street first and the postcode before the town`() {
        val address = ShopAddress(street = "Rua do Telhal", houseNumber = "6 B", postcode = "1150-346", city = "Lisboa", country = "PT")

        assertEquals(listOf("Rua do Telhal 6 B", "1150-346 Lisboa"), address.lines(fallbackCountry = null))
    }

    @Test
    fun `a British address puts the number first and the postcode after the town`() {
        val address = ShopAddress(street = "Berwick Street", houseNumber = "34", postcode = "W1F 8RP", city = "London", country = "GB")

        assertEquals(listOf("34 Berwick Street", "London W1F 8RP"), address.lines(fallbackCountry = null))
    }

    @Test
    fun `without a country on the address, the country of the search decides`() {
        val address = ShopAddress(street = "Berwick Street", houseNumber = "34", postcode = "W1F 8RP", city = "London")

        assertEquals(listOf("34 Berwick Street", "London W1F 8RP"), address.lines(fallbackCountry = "GB"))
        assertEquals(listOf("Berwick Street 34", "W1F 8RP London"), address.lines(fallbackCountry = "PT"))
    }

    @Test
    fun `the country on the address wins over the country of the search`() {
        val address = ShopAddress(street = "Rua do Telhal", houseNumber = "6", city = "Lisboa", country = "PT")

        assertEquals(listOf("Rua do Telhal 6", "Lisboa"), address.lines(fallbackCountry = "GB"))
    }

    @Test
    fun `with no country at all the street comes first`() {
        val address = ShopAddress(street = "Rua do Telhal", houseNumber = "6")

        assertEquals(listOf("Rua do Telhal 6"), address.lines(fallbackCountry = null))
    }

    @Test
    fun `parts that are missing are left out`() {
        assertEquals(listOf("Rua do Telhal"), ShopAddress(street = "Rua do Telhal").lines(null))
        assertEquals(listOf("Lisboa"), ShopAddress(city = "Lisboa").lines(null))
        assertEquals(listOf("1150-346"), ShopAddress(postcode = "1150-346").lines(null))
        assertEquals(emptyList<String>(), ShopAddress().lines(null))
    }

    @Test
    fun `country codes are matched whatever their case`() {
        val address = ShopAddress(street = "Berwick Street", houseNumber = "34", country = "gb")

        assertEquals(listOf("34 Berwick Street"), address.lines(null))
    }

    @Test
    fun `an address with nothing in it is empty`() {
        assertEquals(true, ShopAddress().isEmpty)
        assertEquals(false, ShopAddress(city = "Lisboa").isEmpty)
    }

    // An address typed or shared as one line.

    @Test
    fun `a line from Maps is split into street, postcode and town`() {
        assertEquals(
            ShopAddress(street = "R. Raul Proença 56a", postcode = "2500", city = "Caldas da Rainha"),
            ShopAddress.fromText("R. Raul Proença 56a, 2500 Caldas da Rainha")
        )
        assertEquals(
            ShopAddress(street = "Rua do Telhal 6 B", postcode = "1150-346", city = "Lisboa"),
            ShopAddress.fromText("Rua do Telhal 6 B, 1150-346 Lisboa")
        )
    }

    @Test
    fun `the town after a comma is the town, whatever the street part holds`() {
        assertEquals(
            ShopAddress(street = "R. Raul Proença 56a 2500", city = "Caldas da Rainha"),
            ShopAddress.fromText("R. Raul Proença 56a 2500, Caldas da Rainha")
        )
    }

    @Test
    fun `a line with no comma is all street`() {
        assertEquals(ShopAddress(street = "Rua do Telhal 6"), ShopAddress.fromText("Rua do Telhal 6"))
    }

    @Test
    fun `more than two parts keep all but the last as the street`() {
        assertEquals(
            ShopAddress(street = "Rua X 3, Bairro Alto", postcode = "1100-225", city = "Lisboa"),
            ShopAddress.fromText("Rua X 3, Bairro Alto, 1100-225 Lisboa")
        )
    }

    @Test
    fun `a blank line is no address`() {
        assertEquals(true, ShopAddress.fromText("  ,  ").isEmpty)
        assertEquals(true, ShopAddress.fromText("").isEmpty)
    }

    @Test
    fun `a text address is laid out the way the country does it, in two lines`() {
        val address = ShopAddress.fromText("R. Raul Proença 56a, 2500 Caldas da Rainha")

        assertEquals(listOf("R. Raul Proença 56a", "2500 Caldas da Rainha"), address.lines(fallbackCountry = "PT"))
    }
}
