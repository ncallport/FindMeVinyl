package com.nickallport.findmevinyl.shops

/**
 * Opening hours as people write them ("Mon-Sat 10-7; Sun closed", "Monday to Friday 10am-6pm") turned into the form
 * OpenStreetMap uses ("Mo-Sa 10:00-19:00; Su off"), which is what the app reads. Hours that are already in that form are
 * returned as they are. If any rule cannot be understood, the whole text is, and the result is null.
 */
object FriendlyHours {
    private val DAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    private val DAY_NAMES: Map<String, Int> = mapOf(
        "mon" to 0, "monday" to 0, "mo" to 0,
        "tue" to 1, "tues" to 1, "tuesday" to 1, "tu" to 1,
        "wed" to 2, "weds" to 2, "wednesday" to 2, "we" to 2,
        "thu" to 3, "thur" to 3, "thurs" to 3, "thursday" to 3, "th" to 3,
        "fri" to 4, "friday" to 4, "fr" to 4,
        "sat" to 5, "saturday" to 5, "sa" to 5,
        "sun" to 6, "sunday" to 6, "su" to 6
    )
    private val EVERY_DAY = setOf("daily", "everyday", "every day", "all week", "7 days", "seven days")
    private val RULE = Regex("""^([a-z,\- ]*?)\s*(\d.*|closed|off)$""")
    private val TIME = Regex("""^(\d{1,2})(?:[:.](\d{2}))?(am|pm)?$""")

    fun toOsm(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (OpeningHours.parse(trimmed) != null) return trimmed

        val rules = trimmed.split(';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (rules.isEmpty()) return null
        val converted = rules.map { rule(it) }
        if (converted.any { it == null }) return null
        val result = converted.joinToString("; ")
        return result.takeIf { OpeningHours.parse(it) != null }
    }

    private fun rule(raw: String): String? {
        val text = raw.lowercase()
            .replace('\u2013', '-').replace('\u2014', '-')
            .replace(Regex("""\b(from|open|opens|opening)\b"""), " ")
            .replace(" to ", "-").replace(" until ", "-").replace(" till ", "-")
            .replace("&", ",").replace(" and ", ",")
            .replace(Regex("""\s*-\s*"""), "-")
            .replace(Regex("""\s*,\s*"""), ",")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val match = RULE.matchEntire(text) ?: return null
        val days = days(match.groupValues[1].trim()) ?: return null
        val rest = match.groupValues[2]
        val hours = if (rest == "closed" || rest == "off") "off" else ranges(rest) ?: return null
        return "$days $hours"
    }

    // "mon-fri", "mon,wed,fri", "weekdays", "daily", or nothing at all for every day. In the form OpenStreetMap uses.
    private fun days(text: String): String? {
        if (text.isEmpty() || text in EVERY_DAY) return "Mo-Su"
        val parts = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val out = linkedSetOf<String>()
        for (part in parts) {
            when {
                part in EVERY_DAY -> out.add("Mo-Su")
                part == "weekdays" || part == "weekday" -> out.add("Mo-Fr")
                part == "weekends" || part == "weekend" -> out.add("Sa-Su")
                "-" in part -> {
                    val bounds = part.split('-')
                    if (bounds.size != 2) return null
                    val from = DAY_NAMES[bounds[0].trim()] ?: return null
                    val to = DAY_NAMES[bounds[1].trim()] ?: return null
                    out.add(if (from == to) DAYS[from] else "${DAYS[from]}-${DAYS[to]}")
                }
                else -> out.add(DAYS[DAY_NAMES[part] ?: return null])
            }
        }
        return out.joinToString(",")
    }

    private fun ranges(text: String): String? {
        val out = mutableListOf<String>()
        for (part in text.split(',')) {
            val bounds = part.replace(" ", "").split('-')
            if (bounds.size != 2) return null
            val start = time(bounds[0]) ?: return null
            var end = time(bounds[1]) ?: return null
            // 10-7 means ten to seven in the evening, when neither says am or pm and the end is not after the start.
            if (end.minutes <= start.minutes && end.hour in 1..12 && !end.marked && end.minutes + 12 * 60 <= 24 * 60 &&
                end.minutes + 12 * 60 > start.minutes
            ) {
                end = Time(end.hour + 12, end.minute, marked = false)
            }
            if (end.minutes <= start.minutes || end.minutes > 24 * 60) return null
            out.add("${clock(start)}-${clock(end)}")
        }
        return out.joinToString(", ")
    }

    private data class Time(val hour: Int, val minute: Int, val marked: Boolean) {
        val minutes: Int get() = hour * 60 + minute
    }

    private fun time(text: String): Time? {
        val match = TIME.matchEntire(text) ?: return null
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
        val suffix = match.groupValues[3]
        if (minute > 59) return null
        when (suffix) {
            "am" -> { if (hour !in 1..12) return null; if (hour == 12) hour = 0 }
            "pm" -> { if (hour !in 1..12) return null; if (hour != 12) hour += 12 }
            else -> if (hour > 24 || (hour == 24 && minute != 0)) return null
        }
        return Time(hour, minute, marked = suffix.isNotEmpty())
    }

    private fun clock(time: Time): String = "%02d:%02d".format(time.hour, time.minute)
}
