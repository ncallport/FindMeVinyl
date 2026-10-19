package com.nickallport.findmevinyl.export

import org.junit.Assert.assertEquals
import org.junit.Test

class WantListFormatterTest {

    @Test
    fun `formats a want list as one numbered line for each album, in the order given, with a header`() {
        val items = listOf(
            WantedAlbumExportItem("Genesis", "Foxtrot"),
            WantedAlbumExportItem("Pink Floyd", "Animals"),
            WantedAlbumExportItem("Genesis", "Selling England by the Pound")
        )

        val result = WantListFormatter.format(items)

        val expected = """
            My vinyl want list:

            1. Genesis - Foxtrot
            2. Pink Floyd - Animals
            3. Genesis - Selling England by the Pound
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `an empty list produces just the header`() {
        val result = WantListFormatter.format(emptyList())

        assertEquals("My vinyl want list:", result)
    }

    private val five = listOf(
        WantedAlbumExportItem("Genesis", "Foxtrot"),
        WantedAlbumExportItem("Genesis", "Trespass"),
        WantedAlbumExportItem("Pink Floyd", "Animals"),
        WantedAlbumExportItem("Pink Floyd", "Meddle"),
        WantedAlbumExportItem("Roxy Music", "Avalon")
    )

    @Test
    fun `a message goes above the list, with a blank line between`() {
        val result = WantListFormatter.format(
            listOf(WantedAlbumExportItem("Genesis", "Foxtrot")),
            intro = "It's my birthday soon, here are some ideas."
        )

        assertEquals(
            "It's my birthday soon, here are some ideas.\n\nMy vinyl want list:\n\n1. Genesis - Foxtrot",
            result
        )
    }

    @Test
    fun `a message is trimmed, and a blank one is left out`() {
        val items = listOf(WantedAlbumExportItem("Genesis", "Foxtrot"))

        assertEquals("Hello\n\nMy vinyl want list:\n\n1. Genesis - Foxtrot", WantListFormatter.format(items, intro = "  Hello  \n"))
        assertEquals("My vinyl want list:\n\n1. Genesis - Foxtrot", WantListFormatter.format(items, intro = "   \n "))
    }

    @Test
    fun `a message of several lines is kept as it was written`() {
        val result = WantListFormatter.format(emptyList(), intro = "Line one\nLine two")

        assertEquals("Line one\nLine two\n\nMy vinyl want list:", result)
    }

    @Test
    fun `a limit takes the first albums in order and says how many of how many`() {
        val result = WantListFormatter.format(five, limit = 3)

        assertEquals(
            "My vinyl want list (top 3 of 5):\n\n1. Genesis - Foxtrot\n2. Genesis - Trespass\n3. Pink Floyd - Animals",
            result
        )
    }

    @Test
    fun `a limit of one takes just the first`() {
        val result = WantListFormatter.format(five, limit = 1)

        assertEquals("My vinyl want list (top 1 of 5):\n\n1. Genesis - Foxtrot", result)
    }

    @Test
    fun `a limit as big as the list, or bigger, changes nothing`() {
        assertEquals(WantListFormatter.format(five), WantListFormatter.format(five, limit = 5))
        assertEquals(WantListFormatter.format(five), WantListFormatter.format(five, limit = 50))
    }

    @Test
    fun `no limit, or one that is not a positive number, shares everything`() {
        assertEquals(WantListFormatter.format(five), WantListFormatter.format(five, limit = null))
        assertEquals(WantListFormatter.format(five), WantListFormatter.format(five, limit = 0))
        assertEquals(WantListFormatter.format(five), WantListFormatter.format(five, limit = -3))
    }

    @Test
    fun `a message and a limit go together`() {
        val result = WantListFormatter.format(five, intro = "Ideas:", limit = 2)

        assertEquals("Ideas:\n\nMy vinyl want list (top 2 of 5):\n\n1. Genesis - Foxtrot\n2. Genesis - Trespass", result)
    }

    @Test
    fun `an album wanted on a format says so after its title`() {
        val items = listOf(
            WantedAlbumExportItem("Bob Dylan", "Desire", wantFormat = "LP"),
            WantedAlbumExportItem("Big Country", "The Crossing", wantFormat = "CD"),
            WantedAlbumExportItem("Bob Dylan", "Infidels")
        )

        assertEquals(
            "My vinyl want list:\n\n1. Bob Dylan - Desire (on LP)\n2. Big Country - The Crossing (on CD)\n3. Bob Dylan - Infidels",
            WantListFormatter.format(items)
        )
    }
}
