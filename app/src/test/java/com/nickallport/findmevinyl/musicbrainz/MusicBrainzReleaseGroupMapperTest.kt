package com.nickallport.findmevinyl.musicbrainz

import com.nickallport.findmevinyl.discography.ReleaseGroupDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicBrainzReleaseGroupMapperTest {

    @Test
    fun `parses a release group with secondary types into a domain object`() {
        val json = """
            {
              "release-groups": [
                {
                  "id": "abc-123",
                  "title": "Live at Pompeii",
                  "primary-type": "Album",
                  "secondary-types": ["Live"],
                  "first-release-date": "1972-10-01"
                }
              ]
            }
        """.trimIndent()

        val result = MusicBrainzReleaseGroupMapper.parseSearchResponse(json)

        assertEquals(
            listOf(
                ReleaseGroupDto(
                    id = "abc-123",
                    title = "Live at Pompeii",
                    primaryType = "Album",
                    secondaryTypes = listOf("Live"),
                    firstReleaseDate = "1972-10-01"
                )
            ),
            result
        )
    }

    @Test
    fun `treats a missing secondary-types field as an empty list, not null`() {
        val json = """
            {
              "release-groups": [
                {
                  "id": "def-456",
                  "title": "The Dark Side of the Moon",
                  "primary-type": "Album",
                  "first-release-date": "1973-03-01"
                }
              ]
            }
        """.trimIndent()

        val result = MusicBrainzReleaseGroupMapper.parseSearchResponse(json)

        assertEquals(emptyList<String>(), result[0].secondaryTypes)
    }

    @Test
    fun `an empty first release date, which MusicBrainz sends for undated albums, becomes no date`() {
        val json = """
            {
              "release-groups": [
                { "id": "a", "title": "Undated", "primary-type": "Album", "first-release-date": "" },
                { "id": "b", "title": "Dated", "primary-type": "Album", "first-release-date": "1973-03-01" },
                { "id": "c", "title": "Missing", "primary-type": "Album" }
              ]
            }
        """.trimIndent()

        val result = MusicBrainzReleaseGroupMapper.parseSearchResponse(json)

        assertNull(result[0].firstReleaseDate)
        assertEquals("1973-03-01", result[1].firstReleaseDate)
        assertNull(result[2].firstReleaseDate)
    }
}