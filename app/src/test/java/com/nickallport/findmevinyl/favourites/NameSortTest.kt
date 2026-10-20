package com.nickallport.findmevinyl.favourites

import org.junit.Assert.assertEquals
import org.junit.Test

class NameSortTest {

    @Test
    fun `capitals do not count`() {
        assertEquals(listOf("abc", "Big Country", "Joy Division"), listOf("Joy Division", "abc", "Big Country").sortedWith(NameSort))
    }

    @Test
    fun `a leading The is left off`() {
        assertEquals(
            listOf("the cure", "Simple Minds", "The Smiths", "Talk Talk"),
            listOf("Talk Talk", "The Smiths", "Simple Minds", "the cure").sortedWith(NameSort)
        )
    }

    @Test
    fun `only the word The is left off, not a name that starts with those letters, nor The on its own`() {
        assertEquals(
            listOf("Nick Cave", "Sparks", "The", "Theatre of Hate"),
            listOf("Theatre of Hate", "The", "Sparks", "Nick Cave").sortedWith(NameSort)
        )
    }

    @Test
    fun `names that sort the same stay in the order they were given`() {
        assertEquals(listOf("The Smiths", "Smiths"), listOf("The Smiths", "Smiths").sortedWith(NameSort))
    }

    @Test
    fun `the key of a name with spaces round it is the trimmed name without The`() {
        assertEquals("Smiths", NameSort.key("  The   Smiths "))
    }
}
