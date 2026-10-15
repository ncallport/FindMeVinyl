package com.nickallport.findmevinyl.discography

/**
 * The status of a release on MusicBrainz, cut down to what the settings can hide. An album stores the
 * statuses of all its releases, as these names joined with `|`.
 */
object ReleaseStatuses {
    const val OFFICIAL = "Official"
    const val PROMOTION = "Promotion"
    const val BOOTLEG = "Bootleg"

    /** Withdrawn, cancelled, pseudo-release, and a release with no status at all. */
    const val OTHER = "Other"

    fun token(status: String?): String = when (status) {
        "Official" -> OFFICIAL
        "Promotion" -> PROMOTION
        "Bootleg" -> BOOTLEG
        else -> OTHER
    }

    fun encode(tokens: Collection<String>): String = tokens.toSortedSet().joinToString("|")

    fun decode(stored: String): List<String> = if (stored.isEmpty()) emptyList() else stored.split("|")
}
