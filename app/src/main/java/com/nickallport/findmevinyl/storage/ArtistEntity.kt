package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String?,
    val country: String?
)