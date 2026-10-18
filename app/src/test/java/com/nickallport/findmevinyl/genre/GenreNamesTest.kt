package com.nickallport.findmevinyl.genre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenreNamesTest {

    @Test
    fun `a genre is kept trimmed, in lower case, with one space between words`() {
        assertEquals("post punk", GenreNames.normalise("  Post   Punk "))
        assertEquals("post-punk", GenreNames.normalise("Post-Punk"))
    }

    @Test
    fun `nothing, or only spaces, is no genre`() {
        assertNull(GenreNames.normalise(null))
        assertNull(GenreNames.normalise(""))
        assertNull(GenreNames.normalise("   "))
    }

    @Test
    fun `a genre is cut to forty letters`() {
        assertEquals(40, GenreNames.normalise("a".repeat(60))!!.length)
    }

    @Test
    fun `a genre is shown with a capital letter to begin with`() {
        assertEquals("Post-punk", GenreNames.display("post-punk"))
        assertEquals("Alternative rock", GenreNames.display("alternative rock"))
        assertEquals("", GenreNames.display(""))
    }

    @Test
    fun `the heading for artists with no genre says so`() {
        assertEquals("No genre yet", GenreNames.display(null))
    }
}
