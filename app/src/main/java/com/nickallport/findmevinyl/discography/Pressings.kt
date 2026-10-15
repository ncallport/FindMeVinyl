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
