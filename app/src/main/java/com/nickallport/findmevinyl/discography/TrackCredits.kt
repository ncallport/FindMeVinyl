package com.nickallport.findmevinyl.discography

/**
 * An artist the user chose for one track, found by its disc and place on it. [artistId] is null for a name typed by hand.
 * The title is kept so a credit is not put on another track when the pressing shown has a different tracklist.
 */
data class TrackCredit(
    val discIndex: Int,
    val trackIndex: Int,
    val title: String,
    val artistId: String?,
    val artistName: String
)

object TrackCredits {
    /** The details with the user's artists on the tracks they were chosen for, and MusicBrainz's on the rest. */
    fun apply(details: AlbumDetails, credits: List<TrackCredit>): AlbumDetails {
        if (credits.isEmpty()) return details
        val byPlace = credits.associateBy { it.discIndex to it.trackIndex }
        return details.copy(
            discs = details.discs.mapIndexed { d, disc ->
                disc.copy(
                    tracks = disc.tracks.mapIndexed { t, track ->
                        val credit = byPlace[d to t]?.takeIf { it.title == track.title }
                        if (credit == null) track
                        else track.copy(artistName = credit.artistName, artistIds = listOfNotNull(credit.artistId))
                    }
                )
            }
        )
    }
}
