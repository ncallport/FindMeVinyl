package com.nickallport.findmevinyl.backup

import com.nickallport.findmevinyl.settings.Settings

/**
 * What a backup file holds, as JSON (`backup.json` in the zip). Every field can be missing, because the
 * file is read from outside the app, so nothing here is trusted until [BackupViewModel] has checked it.
 * [version] is the version of this format. Raise it when a field changes meaning, not when one is added.
 */
data class BackupFile(
    val app: String? = null,
    val version: Int? = null,
    val exportedAt: Long? = null,
    val favourites: List<BackupArtist>? = null,
    val albums: List<BackupAlbum>? = null,
    val photos: List<BackupPhoto>? = null,
    val shops: List<BackupShop>? = null,
    val groups: List<BackupGroup>? = null,
    val settings: Settings? = null
) {
    companion object {
        const val APP = "FindMeVinyl"
        const val VERSION = 1
    }
}

data class BackupArtist(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val country: String? = null,
    val position: Int? = null,
    /** The genre the user chose. */
    val genre: String? = null
)

/**
 * An album the user owns or wants, or has recorded details of a copy for. Other albums are not saved: the
 * discography brings them back.
 */
data class BackupAlbum(
    val id: String? = null,
    val artistId: String? = null,
    val title: String? = null,
    val category: String? = null,
    val firstReleaseDate: String? = null,
    val owned: Boolean? = null,
    val wanted: Boolean? = null,
    /** Written by older versions: a place among the artist's wants. No longer used. */
    val wantPosition: Int? = null,
    /** The place the want was pinned to by moving it, counting from 0 at the top. Null: not moved. */
    val wantPriority: Int? = null,
    /** Written by older versions: formats the user added for their copy, as labels joined with `|`. */
    val userFormats: String? = null,
    val userNote: String? = null,
    /** The MusicBrainz release the user chose as their copy, and a line that describes it. */
    val myPressingId: String? = null,
    val myPressingText: String? = null,
    /** The format the album is wanted on, a label such as LP. */
    val wantFormat: String? = null,
    /** Written by older versions: the formats of the user's copy joined with `|`, and a note for each as JSON. */
    val userFormatNotes: String? = null,
    /** The copies the user owns, in order. Older files have `userFormats` instead. */
    val copies: List<BackupCopy>? = null,
    /** Only for an album added by hand: Official or Bootleg. An album from MusicBrainz has its statuses looked up again. */
    val statuses: String? = null,
    /** Only for an album added by hand: what the user typed about it, and its tracklist. */
    val description: String? = null,
    val tracks: List<String>? = null,
    /** Only for an album added by hand: the name of its cover image, which is in the zip as `covers/<name>`. */
    val cover: String? = null,
    /** The artists the user chose for tracks, over MusicBrainz's credits. */
    val trackCredits: List<BackupTrackCredit>? = null
)

/** The artist chosen for one track: its disc and place on the disc, its title, and the artist (no id for a typed name). */
data class BackupTrackCredit(
    val discIndex: Int? = null,
    val trackIndex: Int? = null,
    val title: String? = null,
    val artistId: String? = null,
    val artistName: String? = null
)

/** One copy of an album: a format label such as LP, and a note. */
data class BackupCopy(val format: String? = null, val note: String? = null)

/** Artists the user keeps together under a name of their own. The first member is first. */
data class BackupGroup(
    val id: String? = null,
    val name: String? = null,
    val position: Int? = null,
    val members: List<BackupGroupMember>? = null,
    val genre: String? = null
)

data class BackupGroupMember(val artistId: String? = null, val name: String? = null)

/** A photo the user chose. The bytes are in the zip as `photos/{fileName}`. */
data class BackupPhoto(
    val artistId: String? = null,
    val fileName: String? = null
)

/** A favourite shop, as it was when it was saved. */
data class BackupShop(
    val id: String? = null,
    val name: String? = null,
    val category: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val openingHours: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val addressCountry: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val email: String? = null,
    val secondHandOnly: Boolean? = null,
    val regionCountry: String? = null,
    val regionSubdivision: String? = null,
    val savedAt: Long? = null,
    val approximatePosition: Boolean? = null,
    val mapQuery: String? = null,
    val mapsLink: String? = null
)

data class BackupSummary(val artists: Int, val albums: Int, val photos: Int, val shops: Int = 0, val groups: Int = 0)

sealed class RestoreResult {
    /** What was added to the phone. Things it already had are not counted. */
    data class Done(
        val artists: Int,
        val albums: Int,
        val photos: Int,
        val settingsRestored: Boolean,
        val shops: Int = 0,
        val groups: Int = 0
    ) : RestoreResult()

    /** The file was not used, and nothing on the phone was changed. */
    data class Failed(val reason: String) : RestoreResult()
}
