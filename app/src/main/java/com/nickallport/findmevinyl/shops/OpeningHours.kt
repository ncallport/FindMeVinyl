package com.nickallport.findmevinyl.shops

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

sealed class OpeningStatus {
    data class Open(val closesAt: LocalTime?) : OpeningStatus()
    object Closed : OpeningStatus()
}

/**
 * A reading of the OpenStreetMap opening hours syntax (a subset, see [parse]).
 *
 * [schedule] is the usual hours of each weekday. [holidayHours] is what applies when a public holiday
 * falls on that weekday, where the rules name PH. A weekday with no entry has no public holiday rule
 * that is still in force, so its usual hours apply. An empty list means shut. Rules are read in order,
 * and a later rule overrides an earlier one on the days it matches, public holidays included.
 */
class OpeningHours private constructor(
    private val schedule: Map<DayOfWeek, List<MinuteRange>>,
    private val holidayHours: Map<DayOfWeek, List<MinuteRange>>,
    val mentionsPublicHolidays: Boolean
) {
    private data class MinuteRange(val start: Int, val end: Int)

    private fun rangesOn(day: DayOfWeek, isPublicHoliday: Boolean): List<MinuteRange>? =
        (if (isPublicHoliday) holidayHours[day] else null) ?: schedule[day]

    /** The usual hours of [day] as text, such as "11:00-12:30, 13:30-19:00", or "Closed". */
    fun hoursText(day: DayOfWeek): String = format(schedule[day].orEmpty())

    /**
     * What the hours say about public holidays, as one text ("Closed", "15:00-19:30"), or null when they do not
     * name public holidays or give different hours depending on the weekday.
     */
    fun publicHolidayText(): String? =
        holidayHours.values.map { format(it) }.distinct().singleOrNull()

    private fun format(ranges: List<MinuteRange>): String =
        if (ranges.isEmpty()) "Closed" else ranges.joinToString(", ") { "${clock(it.start)}-${clock(it.end)}" }

    private fun clock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    /** [isPublicHoliday]: the moment is on a public holiday. Hours that do not name PH are the same on one. */
    fun statusAt(moment: LocalDateTime, isPublicHoliday: Boolean = false): OpeningStatus {
        val ranges = rangesOn(moment.dayOfWeek, isPublicHoliday) ?: return OpeningStatus.Closed

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

    /** [isPublicHoliday] says whether a date is a public holiday. With none given, none are. */
    fun nextOpening(moment: LocalDateTime, isPublicHoliday: (LocalDate) -> Boolean = { false }): LocalDateTime? {
        val minutesNow = moment.hour * 60 + moment.minute

        for (offset in 0..7) {
            val date = moment.toLocalDate().plusDays(offset.toLong())
            val ranges = rangesOn(date.dayOfWeek, isPublicHoliday(date)) ?: continue

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
            val holidayHours = mutableMapOf<DayOfWeek, List<MinuteRange>>()
            for (rule in rules) {
                val parts = rule.split(ADDITIONAL_RULE_SPLIT)
                if (!applyRule(parts.first(), schedule, holidayHours, additive = false)) return null
                for (extra in parts.drop(1)) {
                    if (!applyRule(extra, schedule, holidayHours, additive = true)) return null
                }
            }
            return OpeningHours(schedule, holidayHours, PUBLIC_HOLIDAY.containsMatchIn(text))
        }

        // The days a rule names, and whether it also names public holidays (PH).
        private data class Selector(val days: Set<DayOfWeek>, val publicHolidays: Boolean)

        private fun applyRule(
            rule: String,
            schedule: MutableMap<DayOfWeek, List<MinuteRange>>,
            holidayHours: MutableMap<DayOfWeek, List<MinuteRange>>,
            additive: Boolean
        ): Boolean {
            val (selector, rest) = if (rule == "24/7") {
                Selector(DayOfWeek.values().toSet(), publicHolidays = false) to "00:00-24:00"
            } else {
                splitDaysAndRest(rule) ?: return false
            }

            val ranges: List<MinuteRange> = if (rest == "off") {
                if (additive) return false
                emptyList()
            } else {
                parseRanges(rest) ?: return false
            }

            // What a public holiday gets from this rule is worked out from the hours before the rule.
            val before = DayOfWeek.values().associateWith { holidayHours[it] ?: schedule[it] ?: emptyList() }

            selector.days.forEach { day ->
                if (additive) {
                    schedule[day] = (schedule[day] ?: emptyList()) + ranges
                    // A public holiday on this weekday is a day the rule names too.
                    holidayHours[day]?.let { holidayHours[day] = it + ranges }
                } else {
                    if (ranges.isEmpty()) schedule.remove(day) else schedule[day] = ranges
                    // A rule that names the weekday overrides an earlier rule for its public holidays.
                    holidayHours.remove(day)
                }
            }

            if (selector.publicHolidays) {
                DayOfWeek.values().forEach { day ->
                    holidayHours[day] = if (additive) before.getValue(day) + ranges else ranges
                }
            }
            return true
        }

        private fun splitDaysAndRest(rule: String): Pair<Selector, String>? {
            val parts = rule.split(" ", limit = 2)
            val hasDays = parts.size == 2 && parts[0].first().isLetter()
            if (!hasDays) return Selector(DayOfWeek.values().toSet(), publicHolidays = false) to rule

            val selector = parseDays(parts[0]) ?: return null
            return selector to parts[1].trim()
        }

        private fun parseDays(text: String): Selector? {
            val result = mutableSetOf<DayOfWeek>()
            var publicHolidays = false
            for (part in text.split(",")) {
                if (part == "PH") {
                    publicHolidays = true
                    continue
                }

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
            return Selector(result, publicHolidays)
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