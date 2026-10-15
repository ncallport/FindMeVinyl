package com.nickallport.findmevinyl.discography

/**
 * MusicBrainz dates can be a year ("1970"), a month ("1970-10") or a day ("1970-10-23"). Compared as
 * plain text, "1970" comes before "1970-10", which puts the less exact date first. The sort key
 * fills the missing parts with 99, so a date known to the month sorts before a bare year of the same
 * year, and a bare year still sorts before the next year.
 */
object ReleaseDates {
    private const val UNKNOWN_PART = "99"

    /** Null for no date, an empty one or a blank one. Dates with no key sort last. */
    fun sortKey(date: String?): String? {
        val trimmed = date?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val parts = trimmed.split("-")
        return (parts + List((3 - parts.size).coerceAtLeast(0)) { UNKNOWN_PART }).joinToString("-")
    }
}
