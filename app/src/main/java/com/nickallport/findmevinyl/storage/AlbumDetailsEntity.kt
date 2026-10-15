package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The details page of an album, found once and kept: tracks, label and genres as JSON (see
 * `AlbumDetailsJson`), and when they were fetched, in epoch milliseconds.
 */
@Entity(tableName = "album_details")
data class AlbumDetailsEntity(
    @PrimaryKey val albumId: String,
    val json: String,
    val fetchedAt: Long
)
