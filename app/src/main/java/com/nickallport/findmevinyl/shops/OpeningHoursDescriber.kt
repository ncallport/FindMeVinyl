package com.nickallport.findmevinyl.shops

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object OpeningHoursDescriber {
    private val DAY_NAMES = mapOf(
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun"
    )

    /**
     * [isPublicHoliday] says whether a date is a public holiday where the shop is. Without it the
     * holidays are not known, so hours that mention them say that holidays may differ. With it, public
     * holiday rules are followed, and a shop whose hours mention them says so on a public holiday.
     */
    fun describe(
        openingHours: String,
        now: LocalDateTime,
        isPublicHoliday: ((LocalDate) -> Boolean)? = null
    ): String? {
        val hours = OpeningHours.parse(openingHours) ?: return null
        val holidayToday = isPublicHoliday?.invoke(now.toLocalDate()) ?: false

        val status = when (val current = hours.statusAt(now, holidayToday)) {
            is OpeningStatus.Open -> {
                current.closesAt?.let { "Open now, closes ${formatTime(it)}" } ?: "Open now"
            }
            OpeningStatus.Closed -> describeClosed(hours, now, isPublicHoliday ?: { false })
        }

        return when {
            !hours.mentionsPublicHolidays -> status
            isPublicHoliday == null -> "$status (public holidays may differ)"
            holidayToday -> "$status (public holiday today)"
            else -> status
        }
    }

    private fun describeClosed(hours: OpeningHours, now: LocalDateTime, isPublicHoliday: (LocalDate) -> Boolean): String {
        val next = hours.nextOpening(now, isPublicHoliday) ?: return "Closed"

        val daysAway = ChronoUnit.DAYS.between(now.toLocalDate(), next.toLocalDate())
        val time = formatTime(next.toLocalTime())
        val day = DAY_NAMES.getValue(next.dayOfWeek)

        return when {
            daysAway == 0L -> "Closed, opens $time"
            daysAway >= 7L -> "Closed, opens next $day $time"
            else -> "Closed, opens $day $time"
        }
    }

    private fun formatTime(time: LocalTime): String {
        val hour = time.hour.toString().padStart(2, '0')
        val minute = time.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }
}