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
}