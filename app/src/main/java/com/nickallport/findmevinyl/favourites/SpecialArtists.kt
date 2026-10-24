package com.nickallport.findmevinyl.favourites

/**
 * The two pages under "Various Artists and Soundtracks" on the Artists page. They stand in for an artist's id, as a group's id
 * does, but they are not favourites, and nothing is asked of MusicBrainz for them: each is only what is saved.
 *
 * Various Artists is MusicBrainz's own artist of that name, so an album credited to it files itself there. Soundtracks is no
 * artist: its page lists every soundtrack the user owns or wants, whoever it is by, and an album added from it is filed
 * under Various Artists.
 */
object SpecialArtists {
    /** MusicBrainz's artist "Various Artists", the one marked for compilations (checked 2026-10-24). */
    const val VARIOUS_ID = "89ad4ac3-39f7-470e-963a-56509c546377"
    const val SOUNDTRACKS_ID = "special/soundtracks"

    const val VARIOUS_NAME = "Various Artists"
    const val SOUNDTRACKS_NAME = "Soundtracks"

    fun isSpecial(id: String): Boolean = id == VARIOUS_ID || id == SOUNDTRACKS_ID

    fun nameOf(id: String): String? = when (id) {
        VARIOUS_ID -> VARIOUS_NAME
        SOUNDTRACKS_ID -> SOUNDTRACKS_NAME
        else -> null
    }

    /** The artist an album added on the page [id] is filed under. */
    fun ownerOf(id: String): String = if (id == SOUNDTRACKS_ID) VARIOUS_ID else id
}
