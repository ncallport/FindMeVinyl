package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A photo the user chose for an artist. [fileName] is the file in the app's own photo folder, which
 * changes with every new photo, so a picture shown from the old file is not mistaken for the new one.
 * [savedAt] is in epoch milliseconds. The artist's own picture from Wikimedia Commons is not touched,
 * so removing this brings it back.
 */
@Entity(tableName = "artist_user_photos")
data class ArtistUserPhotoEntity(
    @PrimaryKey val artistId: String,
    val fileName: String,
    val savedAt: Long
)
