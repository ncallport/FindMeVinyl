package com.nickallport.findmevinyl.discography

data class TrackSection(val title: String?, val tracks: List<Track>) {
    /** The running time, when every track has a length. */
    fun totalText(): String? {
        if (tracks.isEmpty() || tracks.any { (it.lengthMillis ?: 0) <= 0 }) return null
        return Track(null, "", tracks.sumOf { it.lengthMillis ?: 0 }).lengthText()
    }
}

/**
 * Splits a disc's tracks into the sections the page shows. A vinyl disc is numbered by side (A1, A2,
 * B1, ...) and becomes "Side A", "Side B". A disc in a set says which disc it is. Anything else is
 * one section.
 */
object TrackSections {
    private val sideNumber = Regex("""^([A-Za-z]+)\d*$""")

    fun of(disc: Disc, multipleDiscs: Boolean, discNumber: Int = 1): List<TrackSection> {
        if (disc.tracks.isEmpty()) return emptyList()

        val sides = disc.tracks.map { track -> track.number?.let { sideNumber.find(it.trim())?.groupValues?.get(1)?.uppercase() } }
        val discLabel = "Disc $discNumber"

        if (sides.any { it == null }) {
            val title = when {
                multipleDiscs && disc.title != null -> "$discLabel: ${disc.title}"
                multipleDiscs -> discLabel
                else -> disc.title
            }
            return listOf(TrackSection(title, disc.tracks))
        }

        val bySide = LinkedHashMap<String, MutableList<Track>>()
        disc.tracks.zip(sides).forEach { (track, side) -> bySide.getOrPut(side!!) { mutableListOf() }.add(track) }
        return bySide.map { (side, tracks) ->
            TrackSection(if (multipleDiscs) "$discLabel, Side $side" else "Side $side", tracks)
        }
    }
}
