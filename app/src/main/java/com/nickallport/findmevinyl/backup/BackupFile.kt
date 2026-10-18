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
    val wantPosition: Int? = null,
    /** Formats the user added for their copy, as labels joined with `|`. */
    val userFormats: String? = null,
    val userNote: String? = null,
    /** The MusicBrainz release the user chose as their copy, and a line that describes it. */
    val myPressingId: String? = null,
    val myPressingText: String? = null,
    /** The format the album is wanted on, a label such as LP. */
    val wantFormat: String? = null,
    /** The user's note for each format of their copy, as JSON with the label as the key. */
    val userFormatNotes: String? = null
)

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
