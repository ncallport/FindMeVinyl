package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseFormatsTest {

    private val album = DiscographyCategory.STUDIO_ALBUM

    private fun labels(vararg names: String, category: DiscographyCategory = album) =
        ReleaseFormats.labels(names.toList(), category)

    @Test
    fun `the formats MusicBrainz lists for Trespass become LP, CD, MC and Digital`() {
        val trespass = listOf(
            "CD", "12\" Vinyl", "Digital Media", "Cassette", "DVD-Video",
            "Hybrid SACD (CD layer)", "Hybrid SACD (SACD layer, 2 channels)",
            "Hybrid SACD (SACD layer, multichannel)", "SHM-CD"
        )

        assertEquals(listOf("LP", "CD", "MC", "Digital"), ReleaseFormats.labels(trespass, album))
    }

    @Test
    fun `12 inch vinyl is an LP on an album and a 12 inch on a single or an EP`() {
        assertEquals(listOf("LP"), labels("12\" Vinyl", category = DiscographyCategory.STUDIO_ALBUM))
        assertEquals(listOf("LP"), labels("12\" Vinyl", category = DiscographyCategory.LIVE_ALBUM))
        assertEquals(listOf("LP"), labels("12\" Vinyl", category = DiscographyCategory.COMPILATION))
        assertEquals(listOf("12\""), labels("12\" Vinyl", category = DiscographyCategory.SINGLE))
        assertEquals(listOf("12\""), labels("12\" Vinyl", category = DiscographyCategory.EP))
    }

    @Test
    fun `7 and 10 inch vinyl keep their size`() {
        assertEquals(listOf("7\""), labels("7\" Vinyl"))
        assertEquals(listOf("10\""), labels("10\" Vinyl"))
    }

    @Test
    fun `vinyl of no stated size is treated as 12 inch`() {
        assertEquals(listOf("LP"), labels("Vinyl"))
        assertEquals(listOf("12\""), labels("Vinyl", category = DiscographyCategory.SINGLE))
    }

    @Test
    fun `every kind of CD counts as a CD`() {
        listOf("CD", "CD-R", "SHM-CD", "HDCD", "Blu-spec CD", "8cm CD", "Hybrid SACD (CD layer)").forEach {
            assertEquals("for $it", listOf("CD"), labels(it))
        }
    }

    @Test
    fun `an SACD layer on its own is not a CD`() {
        assertEquals(
            emptyList<String>(),
            labels("SACD", "Hybrid SACD (SACD layer)", "Hybrid SACD (SACD layer, 2 channels)")
        )
    }

    @Test
    fun `cassette is MC and digital media is Digital`() {
        assertEquals(listOf("MC", "Digital"), labels("Digital Media", "Cassette"))
    }

    @Test
    fun `formats that are not shown are ignored`() {
        assertEquals(emptyList<String>(), labels("DVD-Video", "Blu-ray", "8-Track Cartridge", "VCD", "Wax Cylinder"))
    }

    @Test
    fun `labels come in a fixed order and each appears once`() {
        assertEquals(
            listOf("LP", "10\"", "7\"", "CD", "MC", "Digital"),
            labels("Digital Media", "CD", "7\" Vinyl", "CD", "Cassette", "10\" Vinyl", "12\" Vinyl", "CD")
        )
    }

    @Test
    fun `no formats gives no labels`() {
        assertEquals(emptyList<String>(), ReleaseFormats.labels(emptyList(), album))
    }

    @Test
    fun `stored names keep names that contain commas`() {
        val names = listOf("Hybrid SACD (SACD layer, 2 channels)", "CD", "CD", "12\" Vinyl")

        val stored = ReleaseFormats.encode(names)

        assertEquals(
            listOf("12\" Vinyl", "CD", "Hybrid SACD (SACD layer, 2 channels)"),
            ReleaseFormats.decode(stored)
        )
    }

    @Test
    fun `an empty list is stored as an empty string and decodes to nothing`() {
        assertEquals("", ReleaseFormats.encode(emptyList()))
        assertEquals(emptyList<String>(), ReleaseFormats.decode(""))
    }

    // Labels as older versions stored the user's formats.

    @Test
    fun `stored labels are read back in the usual order, a label that is not a format is dropped and each is kept once`() {
        assertEquals(listOf("LP", "CD"), ReleaseFormats.decodeUser("CD|LP"))
        assertEquals(listOf("LP"), ReleaseFormats.decodeUser("LP|nonsense|LP"))
    }

    @Test
    fun `nothing stored means no formats`() {
        assertEquals(emptyList<String>(), ReleaseFormats.decodeUser(null))
        assertEquals(emptyList<String>(), ReleaseFormats.decodeUser(""))
    }
}
