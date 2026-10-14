package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AlbumDetailsDao {
    /** Replaces what was stored for the album, so details fetched again take the old ones' place. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(details: AlbumDetailsEntity): Long

    @Query("SELECT * FROM album_details WHERE albumId = :albumId")
    suspend fun get(albumId: String): AlbumDetailsEntity?
}
