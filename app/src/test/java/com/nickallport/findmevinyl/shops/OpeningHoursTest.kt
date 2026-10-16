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

    // Public holidays. A moment is on a public holiday when the caller says so.

    private fun statusOnHoliday(hours: String, moment: LocalDateTime): OpeningStatus =
        OpeningHours.parse(hours)!!.statusAt(moment, isPublicHoliday = true)

    @Test
    fun `a PH off rule closes the shop on a public holiday and not on other days`() {
        val hours = "Mo-Fr 09:00-17:00; PH off"

        assertEquals(OpeningStatus.Closed, statusOnHoliday(hours, at(monday, "12:00")))
        assertEquals(OpeningStatus.Open(LocalTime.of(17, 0)), status(hours, at(monday, "12:00")))
    }

    @Test
    fun `a PH rule with hours replaces the usual hours on a public holiday`() {
        val hours = "Mo-Sa 10:00-18:00; PH 11:00-16:00"

        assertEquals(OpeningStatus.Closed, statusOnHoliday(hours, at(monday, "10:30")))
        assertEquals(OpeningStatus.Open(LocalTime.of(16, 0)), statusOnHoliday(hours, at(monday, "12:00")))
        assertEquals(OpeningStatus.Open(LocalTime.of(18, 0)), status(hours, at(monday, "10:30")))
    }

    @Test
    fun `a PH rule applies on a public holiday that falls on a day the shop is normally shut`() {
        val hours = "Mo-Sa 10:00-18:00; PH 11:00-16:00"

        assertEquals(OpeningStatus.Open(LocalTime.of(16, 0)), statusOnHoliday(hours, at(sunday, "12:00")))
        assertEquals(OpeningStatus.Closed, status(hours, at(sunday, "12:00")))
    }

    @Test
    fun `PH as a day in the list opens the shop on public holidays with those hours`() {
        val hours = "Mo-Sa,PH 10:00-18:00"

        assertEquals(OpeningStatus.Open(LocalTime.of(18, 0)), statusOnHoliday(hours, at(sunday, "12:00")))
        assertEquals(OpeningStatus.Closed, status(hours, at(sunday, "12:00")))
    }

    @Test
    fun `an additional PH rule adds its hours to the usual ones`() {
        val hours = "Mo-Sa 11:00-19:30, Su,PH 15:00-19:30"

        // A public holiday on a Monday: the usual 11:00 start still applies, and 15:00 adds nothing new.
        assertEquals(OpeningStatus.Open(LocalTime.of(19, 30)), statusOnHoliday(hours, at(monday, "12:00")))
        // A public holiday on a Sunday: only the PH hours exist.
        assertEquals(OpeningStatus.Closed, statusOnHoliday(hours, at(sunday, "12:00")))
        assertEquals(OpeningStatus.Open(LocalTime.of(19, 30)), statusOnHoliday(hours, at(sunday, "16:00")))
    }

    @Test
    fun `a rule that comes later wins on a public holiday that falls on a day it names`() {
        val hours = "PH off; Mo-Fr 09:00-17:00"

        assertEquals(OpeningStatus.Open(LocalTime.of(17, 0)), statusOnHoliday(hours, at(monday, "12:00")))
    }

    @Test
    fun `a PH off that comes after other rules wins on every public holiday`() {
        val hours = "Mo-Su 10:00-18:00; Mo 09:00-12:00; PH off"

        assertEquals(OpeningStatus.Closed, statusOnHoliday(hours, at(monday, "10:00")))
        assertEquals(OpeningStatus.Closed, statusOnHoliday(hours, at(tuesday, "10:00")))
    }

    @Test
    fun `a PH rule on its own opens the shop only on public holidays`() {
        val hours = "PH 10:00-14:00"

        assertEquals(OpeningStatus.Open(LocalTime.of(14, 0)), statusOnHoliday(hours, at(wednesday, "11:00")))
        assertEquals(OpeningStatus.Closed, status(hours, at(wednesday, "11:00")))
    }

    @Test
    fun `hours that do not mention public holidays are the same on a public holiday`() {
        assertEquals(
            OpeningStatus.Open(LocalTime.of(18, 0)),
            statusOnHoliday("Mo-Sa 10:00-18:00", at(monday, "12:00"))
        )
        assertEquals(OpeningStatus.Open(null), statusOnHoliday("24/7", at(monday, "12:00")))
    }

    @Test
    fun `the next opening skips a day that is a public holiday with the shop off`() {
        val hours = "Mo-Fr 09:00-17:00; PH off"
        val holidays = setOf(monday.plusDays(7))

        // Friday evening, and the Monday after is a public holiday.
        val friday = monday.plusDays(4)
        assertEquals(
            at(tuesday.plusDays(7), "09:00"),
            OpeningHours.parse(hours)!!.nextOpening(at(friday, "18:00")) { it in holidays }
        )
    }

    @Test
    fun `the next opening on a public holiday uses the public holiday hours`() {
        val hours = "Mo-Sa 10:00-18:00; PH 11:00-16:00"

        assertEquals(
            at(monday, "11:00"),
            OpeningHours.parse(hours)!!.nextOpening(at(monday, "08:00")) { it == monday }
        )
    }

    @Test
    fun `the next opening is the same as before when there are no public holidays`() {
        assertEquals(
            at(tuesday, "09:00"),
            OpeningHours.parse("Mo-Fr 09:00-17:00; PH off")!!.nextOpening(at(monday, "18:00"))
        )
    }

    @Test
    fun `the next opening finds nothing when every day is a public holiday with the shop off`() {
        assertNull(OpeningHours.parse("Mo-Fr 09:00-17:00; PH off")!!.nextOpening(at(monday, "18:00")) { true })
    }

    @Test
    fun `a PH off rule that is added to other hours is not allowed`() {
        assertNull(OpeningHours.parse("Mo-Fr 09:00-17:00, PH off"))
    }

    // The hours of each day, for the shop's page.

    private fun hours(text: String) = OpeningHours.parse(text)!!

    @Test
    fun `gives the hours of each day as text`() {
        val louie = hours("Mo-Sa 11:00-19:30; Su,PH 15:00-19:30")

        assertEquals("11:00-19:30", louie.hoursText(java.time.DayOfWeek.MONDAY))
        assertEquals("11:00-19:30", louie.hoursText(java.time.DayOfWeek.SATURDAY))
        assertEquals("15:00-19:30", louie.hoursText(java.time.DayOfWeek.SUNDAY))
    }

    @Test
    fun `a day with two spells of opening gives both`() {
        val carbono = hours("Mo-Sa 11:00-12:30, 13:30-19:00")

        assertEquals("11:00-12:30, 13:30-19:00", carbono.hoursText(java.time.DayOfWeek.TUESDAY))
    }

    @Test
    fun `a day with no hours is closed`() {
        assertEquals("Closed", hours("Mo-Sa 11:00-19:30").hoursText(java.time.DayOfWeek.SUNDAY))
    }

    @Test
    fun `open until the end of the day reads 24 00`() {
        assertEquals("00:00-24:00", hours("Mo-Su 00:00-24:00").hoursText(java.time.DayOfWeek.FRIDAY))
    }

    @Test
    fun `says what the public holidays are, when the hours name them`() {
        assertEquals("Closed", hours("Mo-Sa 10:00-18:00; PH off").publicHolidayText())
        assertEquals("15:00-19:30", hours("Mo-Sa 11:00-19:30; Su,PH 15:00-19:30").publicHolidayText())
    }

    @Test
    fun `says nothing about public holidays when the hours do not name them`() {
        assertNull(hours("Mo-Sa 10:00-18:00").publicHolidayText())
    }
}
