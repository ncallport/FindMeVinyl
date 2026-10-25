package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzArtistCreditDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzCreditedArtistDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzGenreDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzLabelDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzLabelInfoDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzMediumDetailDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDetailResponse
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupDetailDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzTrackDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumDetailsMapperTest {

    private fun track(number: String?, title: String?, length: Long? = null) =
        MusicBrainzTrackDto(number = number, title = title, length = length)

    private val full = MusicBrainzReleaseDetailResponse(
        title = "Trespass", date = "1970-10", country = "GB",
        labelInfo = listOf(MusicBrainzLabelInfoDto("CAS 1020", MusicBrainzLabelDto("Charisma"))),
        releaseGroup = MusicBrainzReleaseGroupDetailDto(
            primaryType = "Album", firstReleaseDate = "1970-10-23",
            genres = listOf(
                MusicBrainzGenreDto("art rock", 1), MusicBrainzGenreDto("rock", 4), MusicBrainzGenreDto("progressive rock", 3)
            )
        ),
        media = listOf(
            MusicBrainzMediumDetailDto(
                position = 1, format = "12\" Vinyl", title = "",
                tracks = listOf(track("A1", "Looking for Someone", 424000), track("B1", "Stagnation", null))
            )
        )
    )

    @Test
    fun `maps the pressing's details`() {
        val details = AlbumDetails.from(full)

        assertEquals("1970-10", details.releaseDate)
        assertEquals("GB", details.country)
        assertEquals("Charisma", details.label)
        assertEquals("CAS 1020", details.catalogNumber)
        assertEquals("1970-10-23", details.firstReleaseDate)
        assertEquals("12\" Vinyl", details.format)
    }

    @Test
    fun `a track keeps who is credited on it, with the words joining the names`() {
        val credited = MusicBrainzTrackDto(
            number = "1", title = "Easy Lover",
            artistCredit = listOf(
                MusicBrainzArtistCreditDto("Philip Bailey", MusicBrainzCreditedArtistDto("pb", "Philip Bailey"), " & "),
                MusicBrainzArtistCreditDto("Phil Collins", MusicBrainzCreditedArtistDto("pc", "Phil Collins"), "")
            )
        )
        val release = full.copy(media = listOf(MusicBrainzMediumDetailDto(position = 1, tracks = listOf(credited))))

        val track = AlbumDetails.from(release).discs.single().tracks.single()

        assertEquals("Philip Bailey & Phil Collins", track.artistName)
        assertEquals(listOf("pb", "pc"), track.artistIds)
    }

    @Test
    fun `a track with no credit has no artist`() {
        val track = AlbumDetails.from(full).discs.single().tracks.first()

        assertNull(track.artistName)
        assertEquals(emptyList<String>(), track.artistIds)
    }

    @Test
    fun `puts the genres with the most votes first`() {
        assertEquals(listOf("rock", "progressive rock", "art rock"), AlbumDetails.from(full).genres)
    }

    @Test
    fun `keeps at most six genres`() {
        val many = full.copy(
            releaseGroup = full.releaseGroup!!.copy(genres = (1..9).map { MusicBrainzGenreDto("genre $it", 10 - it) })
        )

        assertEquals((1..6).map { "genre $it" }, AlbumDetails.from(many).genres)
    }

    @Test
    fun `maps each disc and its tracks with the lengths`() {
        val disc = AlbumDetails.from(full).discs.single()

        assertEquals("12\" Vinyl", disc.format)
        assertNull(disc.title)
        assertEquals(
            listOf(Track("A1", "Looking for Someone", 424000), Track("B1", "Stagnation", null)),
            disc.tracks
        )
    }

    @Test
    fun `puts the discs in order`() {
        val two = full.copy(
            media = listOf(
                MusicBrainzMediumDetailDto(position = 2, format = "CD", title = "Live", tracks = listOf(track("1", "Two"))),
                MusicBrainzMediumDetailDto(position = 1, format = "CD", title = null, tracks = listOf(track("1", "One")))
            )
        )

        assertEquals(listOf("One", "Two"), AlbumDetails.from(two).discs.map { it.tracks.single().title })
        assertEquals(listOf(null, "Live"), AlbumDetails.from(two).discs.map { it.title })
    }

    @Test
    fun `a track with no title is called Untitled`() {
        val blank = full.copy(media = listOf(MusicBrainzMediumDetailDto(1, "CD", null, listOf(track("1", null), track("2", "  ")))))

        assertEquals(listOf("Untitled", "Untitled"), AlbumDetails.from(blank).discs.single().tracks.map { it.title })
    }

    @Test
    fun `blank values become nothing`() {
        val blank = MusicBrainzReleaseDetailResponse(
            date = "", country = " ",
            labelInfo = listOf(MusicBrainzLabelInfoDto("", MusicBrainzLabelDto("")))
        )

        val details = AlbumDetails.from(blank)

        assertNull(details.releaseDate)
        assertNull(details.country)
        assertNull(details.label)
        assertNull(details.catalogNumber)
        assertEquals(emptyList<String>(), details.genres)
        assertEquals(emptyList<Disc>(), details.discs)
    }

    @Test
    fun `uses the first label that has a name`() {
        val labels = full.copy(
            labelInfo = listOf(
                MusicBrainzLabelInfoDto("none", null),
                MusicBrainzLabelInfoDto("CAS 1020", MusicBrainzLabelDto("Charisma")),
                MusicBrainzLabelInfoDto("X", MusicBrainzLabelDto("Other"))
            )
        )

        assertEquals("Charisma", AlbumDetails.from(labels).label)
        assertEquals("CAS 1020", AlbumDetails.from(labels).catalogNumber)
    }

    @Test
    fun `a label without a catalogue number still shows`() {
        val noNumber = full.copy(labelInfo = listOf(MusicBrainzLabelInfoDto(null, MusicBrainzLabelDto("Charisma"))))

        assertEquals("Charisma", AlbumDetails.from(noNumber).label)
        assertNull(AlbumDetails.from(noNumber).catalogNumber)
    }

    @Test
    fun `the format names every kind of disc once`() {
        val mixed = full.copy(
            media = listOf(
                MusicBrainzMediumDetailDto(1, "CD", null, emptyList()),
                MusicBrainzMediumDetailDto(2, "CD", null, emptyList()),
                MusicBrainzMediumDetailDto(3, "DVD-Video", null, emptyList())
            )
        )

        assertEquals("CD, DVD-Video", AlbumDetails.from(mixed).format)
    }

    @Test
    fun `knows whether there is anything to show`() {
        assertEquals(true, AlbumDetails.from(full).hasContent)
        assertEquals(false, AlbumDetails.from(MusicBrainzReleaseDetailResponse()).hasContent)
    }

    @Test
    fun `formats a track length as minutes and seconds`() {
        assertEquals("7:04", Track("A1", "x", 424000).lengthText())
        assertEquals("0:05", Track("1", "x", 5000).lengthText())
        assertEquals("10:00", Track("1", "x", 600000).lengthText())
        assertEquals("1:01:05", Track("1", "x", 3665000).lengthText())
        assertNull(Track("1", "x", null).lengthText())
        assertNull(Track("1", "x", 0).lengthText())
    }
}
