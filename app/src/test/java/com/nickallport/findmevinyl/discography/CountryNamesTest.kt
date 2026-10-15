package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryNamesTest {

    @Test
    fun `names a country from its code`() {
        assertEquals("United Kingdom", CountryNames.of("GB"))
        assertEquals("Germany", CountryNames.of("DE"))
        assertEquals("United States", CountryNames.of("US"))
    }

    @Test
    fun `knows the codes MusicBrainz uses for places that are not countries`() {
        assertEquals("Worldwide", CountryNames.of("XW"))
        assertEquals("Europe", CountryNames.of("XE"))
    }

    @Test
    fun `reads a lower case code`() {
        assertEquals("France", CountryNames.of("fr"))
    }

    @Test
    fun `gives back what it was given when it does not know the code`() {
        assertEquals("ZZ", CountryNames.of("ZZ"))
        assertEquals("Atlantis", CountryNames.of("Atlantis"))
    }
}
