package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDto

/**
 * An album can have dozens of releases. The detail page shows one pressing: the earliest official
 * vinyl release, since this is an app for vinyl. Without any vinyl, the earliest release of any
 * format. Releases with no date come last, and ties keep the order MusicBrainz listed them in.
 * Unofficial releases (bootlegs, promos) are only used when there is nothing official.
 */
object PressingChooser {

    fun choose(releases: List<MusicBrainzReleaseDto>): MusicBrainzReleaseDto? {
        val usable = releases.filter { it.id != null }
        val official = usable.filter { it.status == "Official" }
        val pool = official.ifEmpty { usable }
        val vinyl = pool.filter { release -> release.media.orEmpty().any { it.format.orEmpty().contains("Vinyl") } }

        return vinyl.ifEmpty { pool }
            // sortedWith is stable, so equal dates stay in the listed order.
            .sortedWith(compareBy<MusicBrainzReleaseDto>({ it.date.isNullOrEmpty() }, { it.date.orEmpty() }))
            .firstOrNull()
    }
}
