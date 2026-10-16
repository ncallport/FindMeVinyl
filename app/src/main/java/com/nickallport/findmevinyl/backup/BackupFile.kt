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
    val position: Int? = null
)

/** An album the user owns or wants. Albums that are neither are not saved: the discography brings them back. */
data class BackupAlbum(
    val id: String? = null,
    val artistId: String? = null,
    val title: String? = null,
    val category: String? = null,
    val firstReleaseDate: String? = null,
    val owned: Boolean? = null,
    val wanted: Boolean? = null,
    val wantPosition: Int? = null
)

/** A photo the user chose. The bytes are in the zip as `photos/{fileName}`. */
data class BackupPhoto(
    val artistId: String? = null,
    val fileName: String? = null
)

data class BackupSummary(val artists: Int, val albums: Int, val photos: Int)

sealed class RestoreResult {
    /** What was added to the phone. Things it already had are not counted. */
    data class Done(val artists: Int, val albums: Int, val photos: Int, val settingsRestored: Boolean) : RestoreResult()

    /** The file was not used, and nothing on the phone was changed. */
    data class Failed(val reason: String) : RestoreResult()
}
