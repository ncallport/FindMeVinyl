package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArtistImageDao {
    /** Replaces what was stored for the artist, so a picture checked again after a while takes its place. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(image: ArtistImageEntity): Long

    @Query("SELECT * FROM artist_images WHERE artistId = :artistId")
    suspend fun get(artistId: String): ArtistImageEntity?
}
