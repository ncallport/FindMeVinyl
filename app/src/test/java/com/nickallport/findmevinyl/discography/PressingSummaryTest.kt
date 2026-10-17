package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PressingSummaryTest {

    private val columbia = Pressing("r1", "1962-03-19", "US", "Columbia", "CS 8579", "12\" Vinyl")

    @Test
    fun `the title is the date and the country by name`() {
        assertEquals("1962-03-19, United States", PressingSummary.title(columbia))
    }

    @Test
    fun `the title says when nothing about the date or country is known`() {
        assertEquals("Date unknown", PressingSummary.title(Pressing("r", null, null, null, null, null)))
        assertEquals("Portugal", PressingSummary.title(Pressing("r", null, "PT", null, null, null)))
    }

    @Test
    fun `the detail is the label, the catalogue number and the format`() {
        assertEquals("Columbia, CS 8579, 12\" Vinyl", PressingSummary.detail(columbia))
        assertEquals("Charisma", PressingSummary.detail(Pressing("r", null, null, "Charisma", null, null)))
        assertEquals("CD", PressingSummary.detail(Pressing("r", null, null, null, null, "CD")))
    }

    @Test
    fun `there is no detail when nothing is known`() {
        assertNull(PressingSummary.detail(Pressing("r", "1970", "GB", null, null, null)))
    }

    @Test
    fun `one line puts the title and the detail together`() {
        assertEquals("1962-03-19, United States, Columbia, CS 8579, 12\" Vinyl", PressingSummary.oneLine(columbia))
        assertEquals("1970, United Kingdom", PressingSummary.oneLine(Pressing("r", "1970", "GB", null, null, null)))
    }
}
