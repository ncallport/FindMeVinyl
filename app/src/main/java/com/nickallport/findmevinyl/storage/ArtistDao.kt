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

    @Query("UPDATE favourite_artists SET position = :position WHERE id = :artistId")
    suspend fun setPosition(artistId: String, position: Int): Int

    /** The lowest position in use by a favourite artist or a group of artists, or null when there are none. */
    @Query("SELECT MIN(p) FROM (SELECT position AS p FROM favourite_artists UNION ALL SELECT position AS p FROM artist_groups)")
    suspend fun minPosition(): Int?

    /** Keeps the genre the user chose, or takes it away with null. Nothing else about the artist changes. */
    @Query("UPDATE favourite_artists SET genre = :genre WHERE id = :artistId")
    suspend fun setGenre(artistId: String, genre: String?): Int

    @Query("DELETE FROM favourite_artists WHERE id = :artistId")
    suspend fun deleteById(artistId: String): Int

    @Query("SELECT * FROM favourite_artists WHERE id = :artistId")
    suspend fun getById(artistId: String): ArtistEntity?
}