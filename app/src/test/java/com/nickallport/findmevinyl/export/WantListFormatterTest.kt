package com.nickallport.findmevinyl.export

import org.junit.Assert.assertEquals
import org.junit.Test

class WantListFormatterTest {

    @Test
    fun `formats a want list grouped by artist, with a header`() {
        val items = listOf(
            WantedAlbumExportItem("Genesis", "Foxtrot"),
            WantedAlbumExportItem("Pink Floyd", "Animals"),
            WantedAlbumExportItem("Genesis", "Selling England by the Pound")
        )

        val result = WantListFormatter.format(items)

        val expected = """
            My vinyl want list:

            Genesis
            - Foxtrot
            - Selling England by the Pound

            Pink Floyd
            - Animals
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
            "It's my birthday soon, here are some ideas.\n\nMy vinyl want list:\n\nGenesis\n- Foxtrot",
            result
        )
    }

    @Test
    fun `a message is trimmed, and a blank one is left out`() {
        val items = listOf(WantedAlbumExportItem("Genesis", "Foxtrot"))

        assertEquals("Hello\n\nMy vinyl want list:\n\nGenesis\n- Foxtrot", WantListFormatter.format(items, intro = "  Hello  \n"))
        assertEquals("My vinyl want list:\n\nGenesis\n- Foxtrot", WantListFormatter.format(items, intro = "   \n "))
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
            "My vinyl want list (top 3 of 5):\n\nGenesis\n- Foxtrot\n- Trespass\n\nPink Floyd\n- Animals",
            result
        )
    }

    @Test
    fun `a limit can stop in the middle of an artist`() {
        val result = WantListFormatter.format(five, limit = 1)

        assertEquals("My vinyl want list (top 1 of 5):\n\nGenesis\n- Foxtrot", result)
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

        assertEquals("Ideas:\n\nMy vinyl want list (top 2 of 5):\n\nGenesis\n- Foxtrot\n- Trespass", result)
    }

    @Test
    fun `an album wanted on a format says so after its title`() {
        val items = listOf(
            WantedAlbumExportItem("Bob Dylan", "Desire", wantFormat = "LP"),
            WantedAlbumExportItem("Bob Dylan", "Infidels")
        )

        assertEquals("My vinyl want list:\n\nBob Dylan\n- Desire (on LP)\n- Infidels", WantListFormatter.format(items))
    }
}
