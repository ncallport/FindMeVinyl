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
    val bootleg: Boolean,
    /** A few words about the album, such as where it is from. Optional. */
    val description: String = "",
    /** The tracklist the user typed, in order. Optional. */
    val tracks: List<String> = emptyList()
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

/** The most tracks an album added by hand keeps. */
const val MAX_USER_TRACKS = 100

/** The longest track title that is kept. */
const val MAX_TRACK_TITLE_LENGTH = 200

/** The tracklist of an album added by hand, stored in one column as a JSON list of titles. */
object UserTracks {
    private val gson = com.google.gson.Gson()
    private val type = object : com.google.gson.reflect.TypeToken<List<String?>>() {}.type

    /** The titles trimmed and cut, blank ones dropped, at most [MAX_USER_TRACKS], as stored; null when there are none. */
    fun encode(titles: List<String>): String? {
        val kept = clean(titles)
        return if (kept.isEmpty()) null else gson.toJson(kept)
    }

    /** What [encode] stored, or nothing for no value or text that is not a list. */
    fun decode(stored: String?): List<String> {
        if (stored.isNullOrBlank()) return emptyList()
        val read: List<String?> = try {
            gson.fromJson<List<String?>>(stored, type) ?: return emptyList()
        } catch (e: com.google.gson.JsonParseException) {
            return emptyList()
        } catch (e: ClassCastException) {
            return emptyList()
        }
        return clean(read.filterNotNull())
    }

    fun clean(titles: List<String>): List<String> =
        titles.map { it.trim().take(MAX_TRACK_TITLE_LENGTH).trim() }.filter { it.isNotEmpty() }.take(MAX_USER_TRACKS)

    /** The description trimmed and cut, or null when it is blank. */
    fun cleanDescription(text: String): String? = text.trim().take(MAX_NOTE_LENGTH).trim().ifEmpty { null }
}
