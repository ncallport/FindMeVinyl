package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzLabelDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzLabelInfoDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzMediumDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzRatingDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PressingsTest {

    private fun release(
        id: String?,
        date: String? = null,
        status: String? = "Official",
        country: String? = null,
        labelInfo: List<MusicBrainzLabelInfoDto>? = null,
        vararg formats: String
    ) = MusicBrainzReleaseDto(
        id = id, date = date, status = status, country = country, labelInfo = labelInfo,
        media = formats.map { MusicBrainzMediumDto(format = it) }
    )

    private fun label(name: String?, catalogNumber: String?) =
        MusicBrainzLabelInfoDto(catalogNumber = catalogNumber, label = name?.let { MusicBrainzLabelDto(it) })

    private fun ids(releases: List<MusicBrainzReleaseDto>) = releases.map { it.id }

    // The list of pressings the page can show.

    @Test
    fun `candidates are the official vinyl releases, earliest first`() {
        val releases = listOf(
            release("cd", "1987", formats = arrayOf("CD")),
            release("vinyl-1972", "1972-05", formats = arrayOf("12\" Vinyl")),
            release("vinyl-1971", "1971-10-01", formats = arrayOf("12\" Vinyl")),
            release("bootleg", "1970", status = "Bootleg", formats = arrayOf("12\" Vinyl"))
        )

        assertEquals(listOf("vinyl-1971", "vinyl-1972"), ids(PressingChooser.candidates(releases)))
    }

    @Test
    fun `candidates put undated releases last and keep the listed order for equal dates`() {
        val releases = listOf(
            release("undated", null, formats = arrayOf("12\" Vinyl")),
            release("b", "1972", formats = arrayOf("12\" Vinyl")),
            release("a", "1972", formats = arrayOf("12\" Vinyl")),
            release("blank", "", formats = arrayOf("12\" Vinyl"))
        )

        assertEquals(listOf("b", "a", "undated", "blank"), ids(PressingChooser.candidates(releases)))
    }

    @Test
    fun `without vinyl the candidates are the official releases of any format`() {
        val releases = listOf(
            release("cd-1990", "1990", formats = arrayOf("CD")),
            release("cd-1985", "1985", formats = arrayOf("CD")),
            release("promo", "1980", status = "Promotion", formats = arrayOf("CD"))
        )

        assertEquals(listOf("cd-1985", "cd-1990"), ids(PressingChooser.candidates(releases)))
    }

    @Test
    fun `without anything official the candidates come from every release, and are only the vinyl ones if there is any`() {
        val releases = listOf(
            release("cd", "1990", status = "Bootleg", formats = arrayOf("CD")),
            release("lp", "1991", status = "Bootleg", formats = arrayOf("12\" Vinyl"))
        )

        assertEquals(listOf("lp"), ids(PressingChooser.candidates(releases)))
    }

    @Test
    fun `candidates leave out releases that have no id`() {
        assertEquals(listOf("a"), ids(PressingChooser.candidates(listOf(release(null, "1970"), release("a", "1971")))))
    }

    @Test
    fun `the chosen pressing is the first candidate`() {
        val releases = listOf(
            release("later", "1990", formats = arrayOf("12\" Vinyl")),
            release("first", "1971", formats = arrayOf("12\" Vinyl"))
        )

        assertEquals("first", PressingChooser.choose(releases)?.id)
    }

    // One pressing as the page lists it.

    @Test
    fun `a pressing has its date, country, label, catalogue number and format`() {
        val pressing = Pressing.from(
            release(
                "r1", "1970", country = "DE",
                labelInfo = listOf(label("Charisma Records Ltd.", "6369 905")),
                formats = arrayOf("12\" Vinyl")
            )
        )!!

        assertEquals(Pressing("r1", "1970", "DE", "Charisma Records Ltd.", "6369 905", "12\" Vinyl"), pressing)
    }

    @Test
    fun `a release with nothing but an id is a pressing with nothing known`() {
        assertEquals(Pressing("r2", null, null, null, null, null), Pressing.from(release("r2")))
    }

    @Test
    fun `a release without an id is not a pressing`() {
        assertNull(Pressing.from(release(null, "1970")))
    }

    @Test
    fun `blank text counts as not known`() {
        val pressing = Pressing.from(
            release("r", "  ", country = "", labelInfo = listOf(label(" ", " ")), formats = arrayOf(""))
        )!!

        assertEquals(Pressing("r", null, null, null, null, null), pressing)
    }

    @Test
    fun `takes the first label that has a name`() {
        val pressing = Pressing.from(
            release("r", labelInfo = listOf(label(null, "X 1"), label("Charisma", "CAS 1020")))
        )!!

        assertEquals("Charisma", pressing.label)
        assertEquals("CAS 1020", pressing.catalogNumber)
    }

    @Test
    fun `a label with no catalogue number is still a label`() {
        assertEquals("Charisma", Pressing.from(release("r", labelInfo = listOf(label("Charisma", null))))?.label)
    }

    @Test
    fun `lists each format of a set once`() {
        val pressing = Pressing.from(release("r", formats = arrayOf("12\" Vinyl", "12\" Vinyl", "CD")))!!

        assertEquals("12\" Vinyl, CD", pressing.format)
    }

    // The rating.

    @Test
    fun `reads a rating with votes`() {
        assertEquals(Rating(4.3, 27), Rating.from(MusicBrainzRatingDto(4.3, 27)))
    }

    @Test
    fun `no rating when there is no value or no votes`() {
        assertNull(Rating.from(null))
        assertNull(Rating.from(MusicBrainzRatingDto(null, 5)))
        assertNull(Rating.from(MusicBrainzRatingDto(4.0, 0)))
        assertNull(Rating.from(MusicBrainzRatingDto(4.0, null)))
        assertNull(Rating.from(MusicBrainzRatingDto(0.0, 3)))
    }

    @Test
    fun `says the rating out of 5 with one decimal and the number of votes`() {
        assertEquals("4.3 / 5 (27 votes)", Rating(4.3, 27).text())
        assertEquals("4.0 / 5 (3 votes)", Rating(4.0, 3).text())
        assertEquals("3.7 / 5 (1 vote)", Rating(3.666, 1).text())
    }

    // The details of an album with its pressings.

    private val first = AlbumDetails(
        releaseDate = "1970", country = "DE", label = "Charisma", catalogNumber = "6369 905",
        firstReleaseDate = "1970-10-23", format = "12\" Vinyl", genres = listOf("rock"),
        discs = listOf(Disc(null, "12\" Vinyl", listOf(Track("A1", "One", null)))),
        pressings = listOf(Pressing("r1", "1970", "DE", "Charisma", "6369 905", "12\" Vinyl")),
        pressingId = "r1", rating = Rating(4.3, 27)
    )

    private val other = AlbumDetails(
        releaseDate = "1985", country = "GB", label = "Virgin", catalogNumber = "V 1",
        firstReleaseDate = "1985-01-01", format = "CD", genres = listOf("pop"),
        discs = listOf(Disc(null, "CD", listOf(Track("1", "Two", 1000)))),
        pressingId = "r2"
    )

    @Test
    fun `showing another pressing replaces what belongs to the pressing and keeps what belongs to the album`() {
        val switched = first.withPressing(other)

        assertEquals("1985", switched.releaseDate)
        assertEquals("GB", switched.country)
        assertEquals("Virgin", switched.label)
        assertEquals("V 1", switched.catalogNumber)
        assertEquals("CD", switched.format)
        assertEquals("Two", switched.discs.single().tracks.single().title)
        assertEquals("r2", switched.pressingId)
        // The album's own facts stay.
        assertEquals("1970-10-23", switched.firstReleaseDate)
        assertEquals(listOf("rock"), switched.genres)
        assertEquals(first.pressings, switched.pressings)
        assertEquals(Rating(4.3, 27), switched.rating)
    }

    @Test
    fun `an album with only a rating or only pressings has content to show`() {
        val none = AlbumDetails(null, null, null, null, null, null, emptyList(), emptyList())

        assertFalse(none.hasContent)
        assertTrue(none.copy(rating = Rating(4.0, 2)).hasContent)
        assertTrue(none.copy(pressings = listOf(Pressing("r", null, null, null, null, null))).hasContent)
    }

    @Test
    fun `a pressing dated to the month comes before one dated only to the year`() {
        val releases = listOf(
            release("de-1970", "1970", formats = arrayOf("12\" Vinyl")),
            release("uk-1970-10", "1970-10", formats = arrayOf("12\" Vinyl")),
            release("us-1971-02", "1971-02", formats = arrayOf("12\" Vinyl"))
        )

        assertEquals(listOf("uk-1970-10", "de-1970", "us-1971-02"), ids(PressingChooser.candidates(releases)))
        assertEquals("uk-1970-10", PressingChooser.choose(releases)?.id)
    }
}
