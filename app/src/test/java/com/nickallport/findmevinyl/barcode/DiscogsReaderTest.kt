package com.nickallport.findmevinyl.barcode

import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.discogs.DiscogsFormatDto
import com.nickallport.findmevinyl.discogs.DiscogsResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscogsReaderTest {

    private fun result(title: String, vararg formats: DiscogsFormatDto, year: String? = "2026", label: List<String>? = null) =
        DiscogsResultDto(title = title, year = year, formats = formats.toList(), label = label)

    private fun vinyl(vararg descriptions: String) = DiscogsFormatDto("Vinyl", "2", descriptions.toList())

    @Test
    fun `splits the artist and the album, dropping the star Discogs puts on a name`() {
        val find = DiscogsReader.parse(listOf(result("Adam & The Ants* - The Singles", vinyl("LP", "Compilation", "Stereo")))).single()

        assertEquals("Adam & The Ants", find.artist)
        assertEquals("The Singles", find.album)
    }

    @Test
    fun `drops the number Discogs adds to tell two artists of one name apart`() {
        val find = DiscogsReader.parse(listOf(result("Genesis (2) - Trespass", vinyl("LP")))).single()

        assertEquals("Genesis", find.artist)
    }

    @Test
    fun `an album with a dash in its title keeps it`() {
        val find = DiscogsReader.parse(listOf(result("Roxy Music - Avalon - Remastered", vinyl("LP")))).single()

        assertEquals("Roxy Music", find.artist)
        assertEquals("Avalon - Remastered", find.album)
    }

    @Test
    fun `a title with no artist and album is left out`() {
        assertEquals(emptyList<DiscogsFind>(), DiscogsReader.parse(listOf(result("Just a title", vinyl("LP")))))
    }

    @Test
    fun `a compilation by Various is marked so it is not matched to an artist`() {
        val find = DiscogsReader.parse(listOf(result("Various - Now That's What I Call Music", vinyl("LP", "Compilation")))).single()

        assertTrue(find.variousArtists)
        assertFalse(DiscogsReader.parse(listOf(result("Roxy Music - Avalon", vinyl("LP")))).single().variousArtists)
    }

    @Test
    fun `reads the year, and none when it is missing or not a year`() {
        assertEquals("2026", DiscogsReader.parse(listOf(result("A - B", vinyl("LP"), year = "2026"))).single().year)
        assertNull(DiscogsReader.parse(listOf(result("A - B", vinyl("LP"), year = null))).single().year)
        assertNull(DiscogsReader.parse(listOf(result("A - B", vinyl("LP"), year = "0"))).single().year)
    }

    @Test
    fun `reads the format as the app's label`() {
        fun format(vararg f: DiscogsFormatDto) = DiscogsReader.parse(listOf(result("A - B", *f))).single().format
        assertEquals("LP", format(vinyl("LP", "Album")))
        assertEquals("7\"", format(vinyl("7\"", "Single", "45 RPM")))
        assertEquals("12\"", format(vinyl("12\"", "EP")))
        assertEquals("10\"", format(vinyl("10\"")))
        assertEquals("LP", format(DiscogsFormatDto("Vinyl", "1", null)))
        assertEquals("CD", format(DiscogsFormatDto("CD", "1", listOf("Album"))))
        assertEquals("MC", format(DiscogsFormatDto("Cassette", "1", listOf("Album"))))
        assertEquals("Digital", format(DiscogsFormatDto("File", "1", listOf("MP3"))))
        assertEquals("CD", format(DiscogsFormatDto("DVD", "1", null), DiscogsFormatDto("CD", "1", null)))
        assertNull(format(DiscogsFormatDto("DVD", "1", null)))
    }

    @Test
    fun `reads the kind of release from the descriptions`() {
        fun category(vararg d: String) = DiscogsReader.parse(listOf(result("A - B", vinyl(*d)))).single().category
        assertEquals(DiscographyCategory.COMPILATION, category("LP", "Compilation"))
        assertEquals(DiscographyCategory.EP, category("12\"", "EP"))
        assertEquals(DiscographyCategory.SINGLE, category("7\"", "Single"))
        assertEquals(DiscographyCategory.SINGLE, category("12\"", "Maxi-Single"))
        assertEquals(DiscographyCategory.STUDIO_ALBUM, category("LP", "Album"))
        assertEquals(DiscographyCategory.STUDIO_ALBUM, category("LP"))
    }

    @Test
    fun `several pressings of one album are one find`() {
        val finds = DiscogsReader.parse(
            listOf(result("Roxy Music - Avalon", vinyl("LP")), result("Roxy Music* - Avalon", DiscogsFormatDto("CD", "1", null)), result("A - Other", vinyl("LP")))
        )

        assertEquals(listOf("Avalon", "Other"), finds.map { it.album })
        // The first pressing's format is the one kept.
        assertEquals("LP", finds.first().format)
    }

    // Matching names between Discogs and MusicBrainz.

    @Test
    fun `names match whatever their capitals, ampersands and punctuation`() {
        assertTrue(MatchNames.same("Adam & The Ants", "Adam and the Ants"))
        assertTrue(MatchNames.same("AC/DC", "AC-DC"))
        assertTrue(MatchNames.same("The Smiths", "Smiths"))
        assertTrue(MatchNames.same("Avalon (Remastered)", "Avalon"))
        assertTrue(MatchNames.same("Sigur Rós", "Sigur Rós"))
    }

    @Test
    fun `different names do not match`() {
        assertFalse(MatchNames.same("Adam & The Ants", "Adam Ant"))
        assertFalse(MatchNames.same("Avalon", "Avalon Sunset"))
        assertFalse(MatchNames.same("", ""))
    }
}
