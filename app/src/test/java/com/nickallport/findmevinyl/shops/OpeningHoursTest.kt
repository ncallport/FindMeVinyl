package com.nickallport.findmevinyl.shops

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertNull

class OpeningHoursTest {

    private val monday = LocalDate.of(2026, 10, 12)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val saturday = monday.plusDays(5)
    private val sunday = monday.plusDays(6)

    private fun at(date: LocalDate, time: String): LocalDateTime =
        date.atTime(LocalTime.parse(time))

    private fun status(hours: String, moment: LocalDateTime): OpeningStatus =
        OpeningHours.parse(hours)!!.statusAt(moment)

    private fun next(hours: String, moment: LocalDateTime): LocalDateTime? =
        OpeningHours.parse(hours)!!.nextOpening(moment)

    @Test
    fun `is open inside a weekday range and reports the closing time`() {
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            status("Mo-Sa 10:00-18:00", at(monday, "12:00"))
        )
    }

    @Test
    fun `is open at the opening time`() {
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            status("Mo-Sa 10:00-18:00", at(monday, "10:00"))
        )
    }

    @Test
    fun `is closed before opening`() {
        assertEquals(OpeningStatus.Closed, status("Mo-Sa 10:00-18:00", at(monday, "09:59")))
    }

    @Test
    fun `is closed at the closing time`() {
        assertEquals(OpeningStatus.Closed, status("Mo-Sa 10:00-18:00", at(monday, "18:00")))
    }

    @Test
    fun `is closed on a day that is not listed`() {
        assertEquals(OpeningStatus.Closed, status("Mo-Sa 10:00-18:00", at(sunday, "12:00")))
    }

    @Test
    fun `handles several ranges in one day`() {
        val hours = "Mo-Fr 09:00-12:30,13:30-17:00"

        assertEquals(OpeningStatus.Closed, status(hours, at(monday, "13:00")))
        assertEquals(
            OpeningStatus.Open(LocalTime.of(17, 0)),
            status(hours, at(monday, "14:00"))
        )
    }

    @Test
    fun `handles a list of days`() {
        val hours = "Mo,We,Fr 10:00-16:00"

        assertEquals(OpeningStatus.Closed, status(hours, at(tuesday, "12:00")))
        assertEquals(
            OpeningStatus.Open(LocalTime.of(16, 0)),
            status(hours, at(wednesday, "12:00"))
        )
    }

    @Test
    fun `times with no days apply every day`() {
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            status("10:00-18:00", at(sunday, "12:00"))
        )
    }

    @Test
    fun `24 7 is always open with no closing time`() {
        assertEquals(OpeningStatus.Open(null), status("24/7", at(sunday, "03:00")))
    }

    @Test
    fun `a range ending at 24 00 is open to the end of the day`() {
        assertEquals(
            OpeningStatus.Open(null),
            status("Mo-Su 00:00-24:00", at(monday, "23:30"))
        )
    }

    @Test
    fun `an off rule closes a day that an earlier rule opened`() {
        val hours = "Mo-Sa 10:00-18:00; We off"

        assertEquals(OpeningStatus.Closed, status(hours, at(wednesday, "12:00")))
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            status(hours, at(tuesday, "12:00"))
        )
    }

    @Test
    fun `a later rule replaces the hours for the days it covers`() {
        val hours = "Mo-Su 10:00-18:00; Sa 10:00-14:00"

        assertEquals(OpeningStatus.Closed, status(hours, at(saturday, "15:00")))
        assertEquals(
            OpeningStatus.Open(LocalTime.of(14, 0)),
            status(hours, at(saturday, "12:00"))
        )
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            status(hours, at(monday, "15:00"))
        )
    }

    @Test
    fun `rules for different days are combined`() {
        val hours = "Mo-Fr 09:00-17:00; Sa 10:00-13:00"

        assertEquals(
            OpeningStatus.Open(LocalTime.of(13, 0)),
            status(hours, at(saturday, "12:00"))
        )
        assertEquals(
            OpeningStatus.Open(LocalTime.of(17, 0)),
            status(hours, at(monday, "12:00"))
        )
        assertEquals(OpeningStatus.Closed, status(hours, at(sunday, "12:00")))
    }

    @Test
    fun `ignores a trailing semicolon and extra spaces`() {
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            status("Mo-Sa 10:00-18:00;  ", at(monday, "12:00"))
        )
    }

    @Test
    fun `a day range can wrap past the end of the week`() {
        val hours = "Sa-Mo 10:00-14:00"

        assertEquals(
            OpeningStatus.Open(LocalTime.of(14, 0)),
            status(hours, at(sunday, "12:00"))
        )
        assertEquals(OpeningStatus.Closed, status(hours, at(tuesday, "12:00")))
    }

    @Test
    fun `24 7 can be combined with an off rule`() {
        val hours = "24/7; Su off"

        assertEquals(OpeningStatus.Closed, status(hours, at(sunday, "12:00")))
        assertEquals(OpeningStatus.Open(null), status(hours, at(monday, "12:00")))
    }

    @Test
    fun `refuses text it cannot read`() {
        val unreadable = listOf(
            "",
            "by appointment",
            "sunrise-sunset",
            "Jan-Mar Mo 10:00-18:00",
            "Mo-Fr 09:00-17:00, Xx 10:00-13:00",
            "Mo-Sa 22:00-02:00",
            "Mo-Sa 25:00-26:00",
            "Mo-Fr 09:00-17:00, We off",
            "Mo-Sa 10:00-18:00 open"
        )

        for (text in unreadable) {
            assertNull("Expected no result for: $text", OpeningHours.parse(text))
        }
    }

    @Test
    fun `next opening is later today when the shop has not opened yet`() {
        assertEquals(
            at(monday, "10:00"),
            next("Mo-Sa 10:00-18:00", at(monday, "09:00"))
        )
    }

    @Test
    fun `next opening is tomorrow after closing time`() {
        assertEquals(
            at(tuesday, "10:00"),
            next("Mo-Sa 10:00-18:00", at(monday, "19:00"))
        )
    }

    @Test
    fun `next opening is the second range when closed between two ranges`() {
        assertEquals(
            at(monday, "13:30"),
            next("Mo-Fr 09:00-12:30,13:30-17:00", at(monday, "13:00"))
        )
    }

    @Test
    fun `next opening skips days that are not listed`() {
        assertEquals(
            at(monday.plusDays(7), "10:00"),
            next("Mo-Sa 10:00-18:00", at(saturday, "19:00"))
        )
    }

    @Test
    fun `next opening can be the same weekday next week`() {
        assertEquals(
            at(monday.plusDays(7), "10:00"),
            next("Mo 10:00-18:00", at(monday, "19:00"))
        )
    }

    @Test
    fun `next opening is nothing when the shop never opens`() {
        assertNull(next("Mo-Su off", at(monday, "12:00")))
    }

    @Test
    fun `next opening while open is the following start`() {
        assertEquals(
            at(monday, "13:30"),
            next("Mo-Fr 09:00-12:30,13:30-17:00", at(monday, "11:00"))
        )
    }

    @Test
    fun `an opening at exactly the given minute is not the next one`() {
        assertEquals(
            at(tuesday, "10:00"),
            next("Mo-Sa 10:00-18:00", at(monday, "10:00"))
        )
    }

    @Test
    fun `a comma adds a rule for other days`() {
        val hours = "Mo-Sa 11:00-12:30, Su 15:00-19:30"

        assertEquals(
            OpeningStatus.Open(LocalTime.of(12, 30)),
            status(hours, at(monday, "12:00"))
        )
        assertEquals(
            OpeningStatus.Open(LocalTime.of(19, 30)),
            status(hours, at(sunday, "16:00"))
        )
        assertEquals(OpeningStatus.Closed, status(hours, at(sunday, "12:00")))
    }

    @Test
    fun `a comma rule adds to the hours of days it repeats`() {
        val hours = "Mo-Fr 09:00-12:00, Mo 14:00-17:00"

        assertEquals(
            OpeningStatus.Open(LocalTime.of(12, 0)),
            status(hours, at(monday, "10:00"))
        )
        assertEquals(
            OpeningStatus.Open(LocalTime.of(17, 0)),
            status(hours, at(monday, "15:00"))
        )
        assertEquals(OpeningStatus.Closed, status(hours, at(tuesday, "15:00")))
    }

    @Test
    fun `public holiday labels are accepted and ignored for the weekday check`() {
        val hours = "Mo-Sa 11:00-19:30, Su,PH 15:00-19:30"

        assertEquals(OpeningStatus.Closed, status(hours, at(sunday, "12:00")))
        assertEquals(
            OpeningStatus.Open(LocalTime.of(19, 30)),
            status(hours, at(sunday, "16:00"))
        )
        assertEquals(
            OpeningStatus.Open(LocalTime.of(19, 30)),
            status(hours, at(monday, "12:00"))
        )
    }

    @Test
    fun `a public holiday off rule is accepted and ignored`() {
        assertEquals(
            OpeningStatus.Open(LocalTime.of(17, 0)),
            status("Mo-Fr 09:00-17:00; PH off", at(monday, "12:00"))
        )
    }

    @Test
    fun `knows when the text mentions public holidays`() {
        assertEquals(
            true,
            OpeningHours.parse("Mo-Sa 11:00-19:30, Su,PH 15:00-19:30")!!.mentionsPublicHolidays
        )
        assertEquals(
            true,
            OpeningHours.parse("Mo-Fr 09:00-17:00; PH off")!!.mentionsPublicHolidays
        )
        assertEquals(
            false,
            OpeningHours.parse("Mo-Sa 10:00-18:00")!!.mentionsPublicHolidays
        )
    }
}
