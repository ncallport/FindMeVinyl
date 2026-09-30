package com.nickallport.findmevinyl.musicbrainz

import com.nickallport.findmevinyl.artist.ArtistSearchResultDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicBrainzArtistMapperTest {

    @Test
    fun `parses a well-formed artist search response into domain objects`() {
        val json = """
            {
              "artists": [
                {
                  "id": "1c70a3fc-fe03-4b23-ba9c-873c33dcd814",
                  "name": "Genesis",
                  "type": "Group",
                  "country": "GB",
                  "life-span": { "begin": "1967", "end": "1998" },
                  "disambiguation": "English rock band"
                }
              ]
            }
        """.trimIndent()

        val result = MusicBrainzArtistMapper.parseSearchResponse(json)

        assertEquals(
            listOf(
                ArtistSearchResultDto(
                    id = "1c70a3fc-fe03-4b23-ba9c-873c33dcd814",
                    name = "Genesis",
                    type = "Group",
                    country = "GB",
                    lifeSpanBegin = "1967",
                    lifeSpanEnd = "1998",
                    disambiguation = "English rock band"
                )
            ),
            result
        )
    }

    @Test
    fun `handles an artist with missing optional fields`() {
        val json = """
            {
              "artists": [
                {
                  "id": "abc",
                  "name": "Unknown Artist",
                  "type": null,
                  "country": null,
                  "life-span": null,
                  "disambiguation": null
                }
              ]
            }
        """.trimIndent()

        val result = MusicBrainzArtistMapper.parseSearchResponse(json)

        assertEquals(
            listOf(
                ArtistSearchResultDto(
                    id = "abc",
                    name = "Unknown Artist",
                    type = null,
                    country = null,
                    lifeSpanBegin = null,
                    lifeSpanEnd = null,
                    disambiguation = null
                )
            ),
            result
        )
    }
}