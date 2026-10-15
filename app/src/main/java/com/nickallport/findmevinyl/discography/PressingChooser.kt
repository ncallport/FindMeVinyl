package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDto

/**
 * An album can have dozens of releases. The detail page lists the pressings that matter for a vinyl
 * collector, the [candidates]: the official vinyl releases, earliest first. Without any vinyl, the
 * official releases of any format. Unofficial releases (bootlegs, promos) are only used when there is
 * nothing official. Releases with no date come last, a date to the month before a bare year of the same
 * year (see [ReleaseDates]), and ties keep the order MusicBrainz listed them in.
 * The page opens on the first one, which is the one [choose] gives.
 */
object PressingChooser {

    fun choose(releases: List<MusicBrainzReleaseDto>): MusicBrainzReleaseDto? = candidates(releases).firstOrNull()

    fun candidates(releases: List<MusicBrainzReleaseDto>): List<MusicBrainzReleaseDto> {
        val usable = releases.filter { it.id != null }
        val official = usable.filter { it.status == "Official" }
        val pool = official.ifEmpty { usable }
        val vinyl = pool.filter { release -> release.media.orEmpty().any { it.format.orEmpty().contains("Vinyl") } }

        return vinyl.ifEmpty { pool }
            // sortedWith is stable, so equal dates stay in the listed order.
            .sortedWith(compareBy<MusicBrainzReleaseDto, String?>(nullsLast()) { ReleaseDates.sortKey(it.date) })
    }
}
