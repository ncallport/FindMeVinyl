package com.nickallport.findmevinyl.artist

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistDescriptionFormatterTest {

    @Test
    fun `a group with a country and no end date is described as still active`() {
        val artist = ArtistSearchResultDto(
            id = "1", name = "Pink Floyd", type = "Group", country = "GB",
            lifeSpanBegin = "1965", lifeSpanEnd = null, disambiguation = null
        )
        assertEquals(
            "Band from GB, active since 1965",
            ArtistDescriptionFormatter.describe(artist)
        )
    }

    @Test
    fun `a group with an end date shows the full active range`() {
        val artist = ArtistSearchResultDto(
            id = "2", name = "The Beatles", type = "Group", country = "GB",
            lifeSpanBegin = "1960", lifeSpanEnd = "1970", disambiguation = null
        )
        assertEquals(
            "Band from GB, active 1960–1970",
            ArtistDescriptionFormatter.describe(artist)
        )
    }

    @Test
    fun `a disambiguation note is appended in parentheses when present`() {
        val artist = ArtistSearchResultDto(
            id = "3", name = "Genesis", type = "Group", country = "GB",
            lifeSpanBegin = "1967", lifeSpanEnd = null, disambiguation = "English rock band"
        )
        assertEquals(
            "Band from GB, active since 1967 (English rock band)",
            ArtistDescriptionFormatter.describe(artist)
        )
    }

    @Test
    fun `a missing country omits the country phrase`() {
        val artist = ArtistSearchResultDto(
            id = "4", name = "Solo Person", type = "Person", country = null,
            lifeSpanBegin = "1980", lifeSpanEnd = null, disambiguation = null
        )
        assertEquals(
            "Solo artist, active since 1980",
            ArtistDescriptionFormatter.describe(artist)
        )
    }

    private fun artist(
        type: String? = "Group", country: String? = "GB", begin: String? = null, end: String? = null, note: String? = null
    ) = ArtistSearchResultDto("9", "Name", type, country, begin, end, note)

    @Test
    fun `an artist with no dates at all has no active phrase, and never says null`() {
        assertEquals("Band from GB", ArtistDescriptionFormatter.describe(artist()))
        assertEquals("Artist", ArtistDescriptionFormatter.describe(artist(type = null, country = null)))
    }

    @Test
    fun `an artist with no dates still shows the disambiguation note`() {
        assertEquals(
            "Solo artist from CA (Toronto based singer-songwriter)",
            ArtistDescriptionFormatter.describe(artist(type = "Person", country = "CA", note = "Toronto based singer-songwriter"))
        )
    }

    @Test
    fun `an end date with no start date says until`() {
        assertEquals("Band from GB, active until 1970", ArtistDescriptionFormatter.describe(artist(end = "1970")))
    }

    @Test
    fun `blank dates count as no dates`() {
        assertEquals("Band from GB", ArtistDescriptionFormatter.describe(artist(begin = "", end = " ")))
        assertEquals("Band from GB, active since 1965", ArtistDescriptionFormatter.describe(artist(begin = "1965", end = "")))
    }
}

