package com.nickallport.findmevinyl.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String?,
    val country: String?,
    /** Where the artist sits in the user's own order, lowest first. Equal values fall back to the name. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0
)