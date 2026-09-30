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
    val wanted: Boolean
)
