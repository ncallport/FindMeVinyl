package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The picture of an artist, found once and kept. A null [imageUrl] means the artist was checked and
 * has no picture, which is kept too, so it is not asked for again. [checkedAt] is in epoch milliseconds.
 */
@Entity(tableName = "artist_images")
data class ArtistImageEntity(
    @PrimaryKey val artistId: String,
    val imageUrl: String?,
    val checkedAt: Long
)
