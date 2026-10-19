package com.nickallport.findmevinyl.discography

import java.util.UUID

/**
 * Albums the user adds by hand because MusicBrainz does not list them (most bootlegs, say). They are rows in `albums`
 * like any other, with an id that starts with [PREFIX], so nothing asks MusicBrainz about them and the whole-artist
 * check leaves them alone.
 */
object CustomAlbums {
    const val PREFIX = "custom/"

    fun isCustom(albumId: String): Boolean = albumId.startsWith(PREFIX)

    fun newId(): String = PREFIX + UUID.randomUUID()
}

/** What the user types to add or change an album by hand. [year] is empty or four digits. */
data class AlbumDraft(
    val title: String,
    val year: String,
    val category: DiscographyCategory,
    val bootleg: Boolean
)

/** The longest title that is kept. */
const val MAX_ALBUM_TITLE_LENGTH = 200

/**
 * The trimmed, cut title and the date (the year, or null when there is none), or null when the draft is not valid: the
 * title is blank, or the year is not four digits between 1000 and 2100.
 */
fun AlbumDraft.cleaned(): Pair<String, String?>? {
    val cleanTitle = title.trim().take(MAX_ALBUM_TITLE_LENGTH).trim()
    if (cleanTitle.isEmpty()) return null
    val cleanYear = year.trim()
    if (cleanYear.isEmpty()) return cleanTitle to null
    if (!Regex("""\d{4}""").matches(cleanYear) || cleanYear.toInt() !in 1000..2100) return null
    return cleanTitle to cleanYear
}
