package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscographyCategorizerTest {

    @Test
    fun `an album with no secondary types is categorised as a studio album`() {
        val releaseGroup = ReleaseGroupDto(
            id = "abc-123",
            title = "The Dark Side of the Moon",
            primaryType = "Album",
            secondaryTypes = emptyList(),
            firstReleaseDate = "1973-03-01"
        )
        assertEquals(DiscographyCategory.STUDIO_ALBUM, DiscographyCategorizer.categorize(releaseGroup))
    }

    @Test
    fun `an album with secondary type Live is categorised as a live album`() {
        val releaseGroup = ReleaseGroupDto(
            id = "live-1",
            title = "Live at Pompeii",
            primaryType = "Album",
            secondaryTypes = listOf("Live"),
            firstReleaseDate = "1972-10-01"
        )
        assertEquals(DiscographyCategory.LIVE_ALBUM, DiscographyCategorizer.categorize(releaseGroup))
    }

    @Test
    fun `an album with secondary type Compilation is categorised as a compilation`() {
        val releaseGroup = ReleaseGroupDto(
            id = "comp-1",
            title = "Greatest Hits",
            primaryType = "Album",
            secondaryTypes = listOf("Compilation"),
            firstReleaseDate = "1981-11-16"
        )
        assertEquals(DiscographyCategory.COMPILATION, DiscographyCategorizer.categorize(releaseGroup))
    }

    @Test
    fun `a release with primary type EP is categorised as an EP`() {
        val releaseGroup = ReleaseGroupDto(
            id = "ep-1",
            title = "Live 1985",
            primaryType = "EP",
            secondaryTypes = emptyList(),
            firstReleaseDate = "1985-06-01"
        )
        assertEquals(DiscographyCategory.EP, DiscographyCategorizer.categorize(releaseGroup))
    }

    @Test
    fun `a release with primary type Single is categorised as a single`() {
        val releaseGroup = ReleaseGroupDto(
            id = "single-1",
            title = "Another Brick in the Wall",
            primaryType = "Single",
            secondaryTypes = emptyList(),
            firstReleaseDate = "1979-11-16"
        )
        assertEquals(DiscographyCategory.SINGLE, DiscographyCategorizer.categorize(releaseGroup))
    }

    @Test
    fun `a release tagged as both live and compilation is categorised as a live album`() {
        val releaseGroup = ReleaseGroupDto(
            id = "live-comp-1",
            title = "The Best of Live",
            primaryType = "Album",
            secondaryTypes = listOf("Live", "Compilation"),
            firstReleaseDate = "1990-01-01"
        )
        assertEquals(DiscographyCategory.LIVE_ALBUM, DiscographyCategorizer.categorize(releaseGroup))
    }
}