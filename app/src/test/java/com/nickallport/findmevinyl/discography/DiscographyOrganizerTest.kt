package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscographyOrganizerTest {

    private fun album(id: String, title: String, date: String?) = ReleaseGroupDto(
        id = id,
        title = title,
        primaryType = "Album",
        secondaryTypes = emptyList(),
        firstReleaseDate = date
    )

    @Test
    fun `studio albums are sorted by release date, earliest first`() {
        val releaseGroups = listOf(
            album("2", "Wish You Were Here", "1975-09-12"),
            album("1", "The Dark Side of the Moon", "1973-03-01"),
            album("3", "Animals", "1977-01-23")
        )

        val organized = DiscographyOrganizer.organize(releaseGroups)

        val studioAlbums = organized[DiscographyCategory.STUDIO_ALBUM]
        assertEquals(
            listOf("The Dark Side of the Moon", "Wish You Were Here", "Animals"),
            studioAlbums?.map { it.title }
        )
    }

    @Test
    fun `releases with no date are sorted to the end of their category`() {
        val releaseGroups = listOf(
            album("2", "Undated Reissue", null),
            album("1", "The Dark Side of the Moon", "1973-03-01")
        )

        val organized = DiscographyOrganizer.organize(releaseGroups)

        val studioAlbums = organized[DiscographyCategory.STUDIO_ALBUM]
        assertEquals(
            listOf("The Dark Side of the Moon", "Undated Reissue"),
            studioAlbums?.map { it.title }
        )
    }
}