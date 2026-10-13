package com.nickallport.findmevinyl.shops

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningHoursDescriberTest {

    private val monday = LocalDate.of(2026, 10, 12)
    private val sunday = monday.plusDays(6)

    private fun at(date: LocalDate, time: String): LocalDateTime =
        date.atTime(LocalTime.parse(time))

    @Test
    fun `says when the shop is open and when it closes`() {
        assertEquals(
            "Open now, closes 18:00",
            OpeningHoursDescriber.describe("Mo-Sa 10:00-18:00", at(monday, "12:00"))
        )
    }

    @Test
    fun `says only open now when there is no closing time`() {
        assertEquals(
            "Open now",
            OpeningHoursDescriber.describe("24/7", at(monday, "12:00"))
        )
    }

    @Test
    fun `says when it opens later today`() {
        assertEquals(
            "Closed, opens 10:00",
            OpeningHoursDescriber.describe("Mo-Sa 10:00-18:00", at(monday, "09:00"))
        )
    }

    @Test
    fun `names the day when it opens on another day`() {
        assertEquals(
            "Closed, opens Tue 10:00",
            OpeningHoursDescriber.describe("Mo-Sa 10:00-18:00", at(monday, "19:00"))
        )
    }

    @Test
    fun `names the day when it is closed all day`() {
        assertEquals(
            "Closed, opens Mon 10:00",
            OpeningHoursDescriber.describe("Mo-Sa 10:00-18:00", at(sunday, "12:00"))
        )
    }

    @Test
    fun `says next when the opening is a week away`() {
        assertEquals(
            "Closed, opens next Mon 10:00",
            OpeningHoursDescriber.describe("Mo 10:00-18:00", at(monday, "19:00"))
        )
    }

    @Test
    fun `says only closed when the shop never opens`() {
        assertEquals(
            "Closed",
            OpeningHoursDescriber.describe("Mo-Su off", at(monday, "12:00"))
        )
    }

    @Test
    fun `says nothing when the hours cannot be read`() {
        assertNull(OpeningHoursDescriber.describe("by appointment", at(monday, "12:00")))
        assertNull(OpeningHoursDescriber.describe("Jan-Mar Mo 10:00-18:00", at(monday, "12:00")))
    }

    @Test
    fun `adds a public holiday note to an open status`() {
        assertEquals(
            "Open now, closes 19:30 (public holidays may differ)",
            OpeningHoursDescriber.describe(
                "Mo-Sa 11:00-19:30, Su,PH 15:00-19:30",
                at(monday, "12:00")
            )
        )
    }

    @Test
    fun `adds a public holiday note to a closed status`() {
        assertEquals(
            "Closed, opens Mon 11:00 (public holidays may differ)",
            OpeningHoursDescriber.describe(
                "Mo-Sa 11:00-19:30, Su,PH 15:00-19:30",
                at(sunday, "20:00")
            )
        )
    }

    @Test
    fun `adds the note when the hours only mention holidays in an off rule`() {
        assertEquals(
            "Open now, closes 17:00 (public holidays may differ)",
            OpeningHoursDescriber.describe("Mo-Fr 09:00-17:00; PH off", at(monday, "12:00"))
        )
    }
}
