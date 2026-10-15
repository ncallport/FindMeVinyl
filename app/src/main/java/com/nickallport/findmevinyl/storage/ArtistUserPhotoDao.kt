package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArtistUserPhotoDao {
    /** Replaces the photo stored for the artist. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(photo: ArtistUserPhotoEntity): Long

    @Query("SELECT * FROM artist_user_photos WHERE artistId = :artistId")
    suspend fun get(artistId: String): ArtistUserPhotoEntity?

    @Query("DELETE FROM artist_user_photos WHERE artistId = :artistId")
    suspend fun delete(artistId: String): Int
}
