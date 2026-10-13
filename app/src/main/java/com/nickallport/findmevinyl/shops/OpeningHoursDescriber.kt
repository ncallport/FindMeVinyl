package com.nickallport.findmevinyl.shops

import java.time.DayOfWeek
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

    fun describe(openingHours: String, now: LocalDateTime): String? {
        val hours = OpeningHours.parse(openingHours) ?: return null

        val status = when (val current = hours.statusAt(now)) {
            is OpeningStatus.Open -> {
                current.closesAt?.let { "Open now, closes ${formatTime(it)}" } ?: "Open now"
            }
            OpeningStatus.Closed -> describeClosed(hours, now)
        }

        return if (hours.mentionsPublicHolidays) "$status (public holidays may differ)" else status
    }

    private fun describeClosed(hours: OpeningHours, now: LocalDateTime): String {
        val next = hours.nextOpening(now) ?: return "Closed"

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