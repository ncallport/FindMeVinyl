package com.nickallport.findmevinyl.shops

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

sealed class OpeningStatus {
    data class Open(val closesAt: LocalTime?) : OpeningStatus()
    object Closed : OpeningStatus()
}

class OpeningHours private constructor(
    private val schedule: Map<DayOfWeek, List<MinuteRange>>,
    val mentionsPublicHolidays: Boolean
) {
    private data class MinuteRange(val start: Int, val end: Int)

    fun statusAt(moment: LocalDateTime): OpeningStatus {
        val ranges = schedule[moment.dayOfWeek] ?: return OpeningStatus.Closed

        val minutes = moment.hour * 60 + moment.minute
        val openRange = ranges.firstOrNull { minutes >= it.start && minutes < it.end }
            ?: return OpeningStatus.Closed

        val closesAt = if (openRange.end >= END_OF_DAY) {
            null
        } else {
            LocalTime.of(openRange.end / 60, openRange.end % 60)
        }
        return OpeningStatus.Open(closesAt)
    }

    fun nextOpening(moment: LocalDateTime): LocalDateTime? {
        val minutesNow = moment.hour * 60 + moment.minute

        for (offset in 0..7) {
            val date = moment.toLocalDate().plusDays(offset.toLong())
            val ranges = schedule[date.dayOfWeek] ?: continue

            val start = ranges
                .map { it.start }
                .filter { offset > 0 || it > minutesNow }
                .minOrNull() ?: continue

            return date.atTime(start / 60, start % 60)
        }
        return null
    }

    companion object {
        private const val END_OF_DAY = 24 * 60
        private val DAY_CODES = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        private val TIME_RANGE = Regex("""(\d{1,2}):(\d{2})-(\d{1,2}):(\d{2})""")
        private val ADDITIONAL_RULE_SPLIT = Regex(""",\s+(?=[A-Za-z])""")
        private val PUBLIC_HOLIDAY = Regex("""\bPH\b""")

        fun parse(text: String): OpeningHours? {
            val rules = text.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            if (rules.isEmpty()) return null

            val schedule = mutableMapOf<DayOfWeek, List<MinuteRange>>()
            for (rule in rules) {
                val parts = rule.split(ADDITIONAL_RULE_SPLIT)
                if (!applyRule(parts.first(), schedule, additive = false)) return null
                for (extra in parts.drop(1)) {
                    if (!applyRule(extra, schedule, additive = true)) return null
                }
            }
            return OpeningHours(schedule, PUBLIC_HOLIDAY.containsMatchIn(text))
        }

        private fun applyRule(
            rule: String,
            schedule: MutableMap<DayOfWeek, List<MinuteRange>>,
            additive: Boolean
        ): Boolean {
            val (days, rest) = if (rule == "24/7") {
                DayOfWeek.values().toSet() to "00:00-24:00"
            } else {
                splitDaysAndRest(rule) ?: return false
            }

            if (rest == "off") {
                if (additive) return false
                days.forEach { schedule.remove(it) }
                return true
            }

            val ranges = parseRanges(rest) ?: return false
            days.forEach { day ->
                schedule[day] = if (additive) (schedule[day] ?: emptyList()) + ranges else ranges
            }
            return true
        }

        private fun splitDaysAndRest(rule: String): Pair<Set<DayOfWeek>, String>? {
            val parts = rule.split(" ", limit = 2)
            val hasDays = parts.size == 2 && parts[0].first().isLetter()
            if (!hasDays) return DayOfWeek.values().toSet() to rule

            val days = parseDays(parts[0]) ?: return null
            return days to parts[1].trim()
        }

        private fun parseDays(text: String): Set<DayOfWeek>? {
            val result = mutableSetOf<DayOfWeek>()
            for (part in text.split(",")) {
                if (part == "PH") continue

                val bounds = part.split("-")
                when (bounds.size) {
                    1 -> {
                        val index = dayIndex(bounds[0]) ?: return null
                        result.add(DayOfWeek.of(index + 1))
                    }
                    2 -> {
                        val start = dayIndex(bounds[0]) ?: return null
                        val end = dayIndex(bounds[1]) ?: return null
                        var index = start
                        while (true) {
                            result.add(DayOfWeek.of(index + 1))
                            if (index == end) break
                            index = (index + 1) % 7
                        }
                    }
                    else -> return null
                }
            }
            return result
        }

        private fun dayIndex(code: String): Int? {
            return DAY_CODES.indexOf(code).takeIf { it >= 0 }
        }

        private fun parseRanges(text: String): List<MinuteRange>? {
            val ranges = mutableListOf<MinuteRange>()
            for (part in text.split(",")) {
                val match = TIME_RANGE.matchEntire(part.trim()) ?: return null
                val (startHour, startMinute, endHour, endMinute) = match.destructured
                val start = toMinutes(startHour.toInt(), startMinute.toInt()) ?: return null
                val end = toMinutes(endHour.toInt(), endMinute.toInt()) ?: return null
                if (end <= start) return null
                ranges.add(MinuteRange(start, end))
            }
            return ranges
        }

        private fun toMinutes(hour: Int, minute: Int): Int? {
            if (minute !in 0..59) return null
            if (hour == 24 && minute == 0) return END_OF_DAY
            if (hour !in 0..23) return null
            return hour * 60 + minute
        }
    }
}