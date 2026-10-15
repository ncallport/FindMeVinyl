package com.nickallport.findmevinyl.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val title: String,
    val category: String,
    val firstReleaseDate: String?,
    val owned: Boolean,
    val wanted: Boolean,
    /** MusicBrainz's format names for the album's releases, joined with `|`. Null: never fetched. */
    val formats: String? = null,
    /** When [formats] was fetched, in epoch milliseconds. Null: never fetched. */
    val formatsCheckedAt: Long? = null,
    /** Where the album sits in its artist's want list, lowest first. Equal values fall back to release date. */
    @ColumnInfo(defaultValue = "0") val wantPosition: Int = 0,
    /**
     * The statuses of the album's releases (`Official`, `Promotion`, `Bootleg`, `Other`), each once, sorted,
     * joined with `|`. Null: never fetched. Empty: fetched and no releases were listed.
     */
    val statuses: String? = null
)
