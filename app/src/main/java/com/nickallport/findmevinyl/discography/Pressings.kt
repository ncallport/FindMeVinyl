package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzRatingDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDto

/** One release of an album as the page lists it: enough to tell the pressings apart. */
data class Pressing(
    val releaseId: String,
    val date: String?,
    val country: String?,
    val label: String?,
    val catalogNumber: String?,
    val format: String?
) {
    companion object {
        /** Null for a release with no id, because nothing could be fetched for it. Blank text counts as not known. */
        fun from(release: MusicBrainzReleaseDto): Pressing? {
            val id = release.id ?: return null
            val labelInfo = release.labelInfo.orEmpty().firstOrNull { !it.label?.name.isNullOrBlank() }
            return Pressing(
                releaseId = id,
                date = release.date.clean(),
                country = release.country.clean(),
                label = labelInfo?.label?.name.clean(),
                catalogNumber = labelInfo?.catalogNumber.clean(),
                format = release.media.orEmpty().mapNotNull { it.format.clean() }.distinct().joinToString(", ").ifEmpty { null }
            )
        }

        private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}

/** The average MusicBrainz rating of an album, out of 5, and how many people voted. */
data class Rating(val value: Double, val votes: Int) {
    /** "4.3 / 5 (27 votes)". */
    fun text(): String = "%.1f / 5 (%d %s)".format(java.util.Locale.ROOT, value, votes, if (votes == 1) "vote" else "votes")

    companion object {
        /** Null when nobody has voted, which MusicBrainz sends as a rating with no value. */
        fun from(rating: MusicBrainzRatingDto?): Rating? = of(rating?.value, rating?.votesCount)

        fun of(value: Double?, votes: Int?): Rating? =
            if (value != null && votes != null && value > 0 && votes > 0) Rating(value, votes) else null
    }
}

/** How a pressing reads: on the page's list, and as the line kept when the user chooses it as theirs. */
object PressingSummary {
    /** The date and the country by name, or "Date unknown" when neither is known. */
    fun title(pressing: Pressing): String =
        listOfNotNull(pressing.date, pressing.country?.let { CountryNames.of(it) })
            .joinToString(", ").ifEmpty { "Date unknown" }

    /** The label, the catalogue number and the format, or null when none is known. */
    fun detail(pressing: Pressing): String? = listOfNotNull(
        listOfNotNull(pressing.label, pressing.catalogNumber).joinToString(", ").ifEmpty { null },
        pressing.format
    ).joinToString(", ").ifEmpty { null }

    /** [title] and [detail] on one line. */
    fun oneLine(pressing: Pressing): String = listOfNotNull(title(pressing), detail(pressing)).joinToString(", ")
}

