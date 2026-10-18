package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Test

/** Notes as an earlier version stored them, still read by the migration and when an older backup is restored. */
class FormatNotesTest {

    @Test
    fun `notes are read back in the usual order of the labels`() {
        val notes = FormatNotes.decode("""{"CD":"from Mum","LP":"scratched"}""")

        assertEquals(mapOf("LP" to "scratched", "CD" to "from Mum"), notes)
        assertEquals(listOf("LP", "CD"), notes.keys.toList())
    }

    @Test
    fun `blank notes and labels that are not formats are left out`() {
        val notes = FormatNotes.decode("""{"LP":"   ","Betamax":"x","CD":"ok"}""")

        assertEquals(mapOf("CD" to "ok"), notes)
    }

    @Test
    fun `nothing, or text that is not notes, reads as no notes`() {
        assertEquals(emptyMap<String, String>(), FormatNotes.decode(null))
        assertEquals(emptyMap<String, String>(), FormatNotes.decode(""))
        assertEquals(emptyMap<String, String>(), FormatNotes.decode("not json"))
        assertEquals(emptyMap<String, String>(), FormatNotes.decode("[1, 2]"))
    }
}
