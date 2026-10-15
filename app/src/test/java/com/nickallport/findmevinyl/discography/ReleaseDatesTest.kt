package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseDatesTest {

    private fun sorted(vararg dates: String?) = dates.toList().sortedWith(compareBy<String?, String?>(nullsLast()) { ReleaseDates.sortKey(it) })

    @Test
    fun `no date, an empty date and a blank date have no sort key`() {
        assertNull(ReleaseDates.sortKey(null))
        assertNull(ReleaseDates.sortKey(""))
        assertNull(ReleaseDates.sortKey("   "))
    }

    @Test
    fun `a full date sorts by day`() {
        assertEquals(listOf("1970-10-01", "1970-10-23", "1970-11-02"), sorted("1970-11-02", "1970-10-23", "1970-10-01"))
    }

    @Test
    fun `a date with only a month sorts after the days of that month`() {
        assertEquals(listOf("1970-10-23", "1970-10"), sorted("1970-10", "1970-10-23"))
    }

    @Test
    fun `a year alone sorts after every date in that year, and before the next year`() {
        assertEquals(
            listOf("1970-10", "1970-12-31", "1970", "1971-01-01", "1971"),
            sorted("1971", "1970", "1971-01-01", "1970-12-31", "1970-10")
        )
    }

    @Test
    fun `dates with no date come last`() {
        assertEquals(listOf("1970", null, ""), sorted(null, "1970", ""))
    }

    @Test
    fun `equal dates have equal keys`() {
        assertEquals(ReleaseDates.sortKey("1970-10"), ReleaseDates.sortKey("1970-10"))
    }

    @Test
    fun `takes no notice of spaces around a date`() {
        assertEquals(ReleaseDates.sortKey("1970-10"), ReleaseDates.sortKey(" 1970-10 "))
    }
}
