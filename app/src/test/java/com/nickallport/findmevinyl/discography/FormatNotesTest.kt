package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatNotesTest {

    @Test
    fun `notes for the formats given are kept, trimmed, and read back in the usual order`() {
        val stored = FormatNotes.encode(mapOf("CD" to "  from Mum ", "LP" to "scratched"), listOf("LP", "CD"))

        assertEquals(mapOf("LP" to "scratched", "CD" to "from Mum"), FormatNotes.decode(stored))
        assertEquals(listOf("LP", "CD"), FormatNotes.decode(stored).keys.toList())
    }

    @Test
    fun `a note for a format that is not among those given is dropped`() {
        val stored = FormatNotes.encode(mapOf("LP" to "scratched", "CD" to "from Mum"), listOf("LP"))

        assertEquals(mapOf("LP" to "scratched"), FormatNotes.decode(stored))
    }

    @Test
    fun `blank notes and labels that are not formats are dropped`() {
        val stored = FormatNotes.encode(mapOf("LP" to "   ", "Betamax" to "x", "CD" to "ok"), listOf("LP", "CD", "Betamax"))

        assertEquals(mapOf("CD" to "ok"), FormatNotes.decode(stored))
    }

    @Test
    fun `a note is cut to the length of the album note`() {
        val stored = FormatNotes.encode(mapOf("LP" to "x".repeat(MAX_NOTE_LENGTH + 50)), listOf("LP"))

        assertEquals(MAX_NOTE_LENGTH, FormatNotes.decode(stored).getValue("LP").length)
    }

    @Test
    fun `with nothing to keep there is nothing stored`() {
        assertNull(FormatNotes.encode(emptyMap(), listOf("LP")))
        assertNull(FormatNotes.encode(mapOf("LP" to " "), listOf("LP")))
    }

    @Test
    fun `nothing, or text that is not notes, reads as no notes`() {
        assertEquals(emptyMap<String, String>(), FormatNotes.decode(null))
        assertEquals(emptyMap<String, String>(), FormatNotes.decode(""))
        assertEquals(emptyMap<String, String>(), FormatNotes.decode("not json"))
        assertEquals(emptyMap<String, String>(), FormatNotes.decode("[1, 2]"))
    }
}
