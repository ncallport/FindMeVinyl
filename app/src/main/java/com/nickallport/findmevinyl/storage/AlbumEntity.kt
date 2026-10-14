package com.nickallport.findmevinyl.storage

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
    val formatsCheckedAt: Long? = null
)
