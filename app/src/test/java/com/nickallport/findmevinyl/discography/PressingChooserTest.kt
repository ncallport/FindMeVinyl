package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzMediumDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PressingChooserTest {

    private fun release(
        id: String,
        date: String? = null,
        status: String? = "Official",
        vararg formats: String
    ) = MusicBrainzReleaseDto(
        id = id, date = date, status = status, media = formats.map { MusicBrainzMediumDto(format = it) }
    )

    private fun choose(vararg releases: MusicBrainzReleaseDto) = PressingChooser.choose(releases.toList())?.id

    @Test
    fun `prefers the earliest vinyl release`() {
        assertEquals(
            "vinyl-1971",
            choose(
                release("cd-1987", "1987", formats = arrayOf("CD")),
                release("vinyl-1972", "1972-05", formats = arrayOf("12\" Vinyl")),
                release("vinyl-1971", "1971-10-01", formats = arrayOf("12\" Vinyl"))
            )
        )
    }

    @Test
    fun `an earlier CD does not beat a later vinyl`() {
        assertEquals(
            "vinyl",
            choose(release("cd", "1985", formats = arrayOf("CD")), release("vinyl", "1990", formats = arrayOf("12\" Vinyl")))
        )
    }

    @Test
    fun `every kind of vinyl counts`() {
        assertEquals("seven", choose(release("seven", "1970", formats = arrayOf("7\" Vinyl")), release("cd", "1969", formats = arrayOf("CD"))))
        assertEquals("ten", choose(release("ten", "1970", formats = arrayOf("10\" Vinyl")), release("cd", "1969", formats = arrayOf("CD"))))
        assertEquals("plain", choose(release("plain", "1970", formats = arrayOf("Vinyl")), release("cd", "1969", formats = arrayOf("CD"))))
    }

    @Test
    fun `a release with a vinyl disc among others counts as vinyl`() {
        assertEquals(
            "box",
            choose(release("cd", "1995", formats = arrayOf("CD")), release("box", "1999", formats = arrayOf("CD", "12\" Vinyl")))
        )
    }

    @Test
    fun `releases with no date come last`() {
        assertEquals(
            "dated",
            choose(release("undated", null, formats = arrayOf("12\" Vinyl")), release("dated", "1980", formats = arrayOf("12\" Vinyl")))
        )
        assertEquals(
            "dated",
            choose(release("empty", "", formats = arrayOf("12\" Vinyl")), release("dated", "1980", formats = arrayOf("12\" Vinyl")))
        )
    }

    @Test
    fun `a release with only a year comes before one from later that year`() {
        assertEquals(
            "year",
            choose(release("month", "1970-10", formats = arrayOf("12\" Vinyl")), release("year", "1970", formats = arrayOf("12\" Vinyl")))
        )
    }

    @Test
    fun `keeps the listed order between releases with the same date`() {
        assertEquals(
            "first",
            choose(release("first", "1970", formats = arrayOf("12\" Vinyl")), release("second", "1970", formats = arrayOf("12\" Vinyl")))
        )
    }

    @Test
    fun `prefers official releases to bootlegs and promos`() {
        assertEquals(
            "official",
            choose(
                release("bootleg", "1969", status = "Bootleg", formats = arrayOf("12\" Vinyl")),
                release("official", "1970", status = "Official", formats = arrayOf("12\" Vinyl"))
            )
        )
    }

    @Test
    fun `uses unofficial releases when there is no official one`() {
        assertEquals("bootleg", choose(release("bootleg", "1969", status = "Bootleg", formats = arrayOf("12\" Vinyl"))))
    }

    @Test
    fun `without any vinyl it takes the earliest release of any format`() {
        assertEquals(
            "cassette",
            choose(release("cd", "1987", formats = arrayOf("CD")), release("cassette", "1970", formats = arrayOf("Cassette")))
        )
    }

    @Test
    fun `an official CD beats an unofficial vinyl`() {
        assertEquals(
            "cd",
            choose(release("vinyl", "1970", status = "Bootleg", formats = arrayOf("12\" Vinyl")), release("cd", "1987", formats = arrayOf("CD")))
        )
    }

    @Test
    fun `gives nothing when there are no releases`() {
        assertNull(PressingChooser.choose(emptyList()))
    }

    @Test
    fun `a release with no id is never chosen`() {
        assertNull(PressingChooser.choose(listOf(MusicBrainzReleaseDto(id = null, date = "1970"))))
    }
}
