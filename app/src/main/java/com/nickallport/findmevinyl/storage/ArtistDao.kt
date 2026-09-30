package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artist: ArtistEntity)

    @Query("SELECT * FROM favourite_artists")
    fun getAllFavourites(): List<ArtistEntity>

    @Query("DELETE FROM favourite_artists WHERE id = :artistId")
    fun deleteById(artistId: String)
}

