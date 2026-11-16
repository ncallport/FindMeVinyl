package com.nickallport.findmevinyl.export

import org.junit.Assert.assertEquals
import org.junit.Test

class OwnedListFormatterTest {

    @Test
    fun `one numbered line for each album with the formats of its copies`() {
        val text = OwnedListFormatter.format(
            listOf(
                OwnedAlbumExportItem("Big Country", "The Crossing", listOf("LP")),
                OwnedAlbumExportItem("Bob Dylan", "Desire", listOf("LP", "CD"))
            )
        )

        assertEquals("My record collection (2 albums):\n\n1. Big Country - The Crossing (LP)\n2. Bob Dylan - Desire (LP, CD)", text)
    }

    @Test
    fun `an album with no copies listed has no brackets`() {
        val text = OwnedListFormatter.format(listOf(OwnedAlbumExportItem("Sleep", "Dopesmoker", emptyList())))

        assertEquals("My record collection (1 album):\n\n1. Sleep - Dopesmoker", text)
    }

    @Test
    fun `a format on two copies is named once`() {
        val text = OwnedListFormatter.format(listOf(OwnedAlbumExportItem("Sleep", "Dopesmoker", listOf("LP", "LP", "CD"))))

        assertEquals("My record collection (1 album):\n\n1. Sleep - Dopesmoker (LP, CD)", text)
    }

    @Test
    fun `an empty collection is only the header`() {
        assertEquals("My record collection (0 albums):", OwnedListFormatter.format(emptyList()))
    }
}
