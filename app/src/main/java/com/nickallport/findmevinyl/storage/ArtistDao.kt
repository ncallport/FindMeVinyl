package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artist: ArtistEntity): Long

    @Query("SELECT * FROM favourite_artists")
    suspend fun getAllFavourites(): List<ArtistEntity>

    @Query("DELETE FROM favourite_artists WHERE id = :artistId")
    suspend fun deleteById(artistId: String): Int

    @Query("SELECT * FROM favourite_artists WHERE id = :artistId")
    suspend fun getById(artistId: String): ArtistEntity?
}