package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDetailResponse

data class Track(val number: String?, val title: String, val lengthMillis: Long?) {
    /** "7:04", or "1:01:05" for an hour or more. Null when MusicBrainz does not know the length. */
    fun lengthText(): String? {
        val millis = lengthMillis?.takeIf { it > 0 } ?: return null
        val totalSeconds = (millis + 500) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
    }
}

data class Disc(val title: String?, val format: String?, val tracks: List<Track>)

/**
 * What the detail page shows about an album: one pressing's date, label and tracks, and the genres of
 * the album. It is the response from MusicBrainz boiled down, and is what gets stored.
 */
data class AlbumDetails(
    val releaseDate: String?,
    val country: String?,
    val label: String?,
    val catalogNumber: String?,
    val firstReleaseDate: String?,
    val format: String?,
    val genres: List<String>,
    val discs: List<Disc>
) {
    /** False when MusicBrainz had nothing at all about the pressing, so the page can say so. */
    val hasContent: Boolean
        get() = releaseDate != null || country != null || label != null || genres.isNotEmpty() || discs.isNotEmpty()

    companion object {
        private const val MAX_GENRES = 6

        fun from(release: MusicBrainzReleaseDetailResponse): AlbumDetails {
            val labelInfo = release.labelInfo.orEmpty().firstOrNull { !it.label?.name.isNullOrBlank() }
            val media = release.media.orEmpty().sortedBy { it.position ?: Int.MAX_VALUE }

            return AlbumDetails(
                releaseDate = release.date.clean(),
                country = release.country.clean(),
                label = labelInfo?.label?.name.clean(),
                catalogNumber = labelInfo?.catalogNumber.clean(),
                firstReleaseDate = release.releaseGroup?.firstReleaseDate.clean(),
                format = media.mapNotNull { it.format.clean() }.distinct().joinToString(", ").ifEmpty { null },
                genres = release.releaseGroup?.genres.orEmpty()
                    .filter { !it.name.isNullOrBlank() }
                    .sortedByDescending { it.count ?: 0 }
                    .take(MAX_GENRES)
                    .map { it.name!!.trim() },
                discs = media.map { medium ->
                    Disc(
                        title = medium.title.clean(),
                        format = medium.format.clean(),
                        tracks = medium.tracks.orEmpty().map {
                            Track(it.number.clean(), it.title.clean() ?: "Untitled", it.length)
                        }
                    )
                }
            )
        }

        private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
