package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(album: AlbumEntity): Long

    @Query("SELECT * FROM albums WHERE artistId = :artistId")
    suspend fun getAlbumsForArtist(artistId: String): List<AlbumEntity>

    @Query("UPDATE albums SET owned = :owned WHERE id = :albumId")
    suspend fun setOwned(albumId: String, owned: Boolean): Int

    @Query("UPDATE albums SET wanted = :wanted WHERE id = :albumId")
    suspend fun setWanted(albumId: String, wanted: Boolean): Int

    @Query("UPDATE albums SET wanted = 0 WHERE artistId = :artistId")
    suspend fun clearWantedForArtist(artistId: String): Int

    @Query(
        """
        SELECT albums.id AS albumId,
               albums.artistId AS artistId,
               favourite_artists.name AS artistName,
               albums.title AS albumTitle,
               albums.firstReleaseDate AS firstReleaseDate
        FROM albums
        INNER JOIN favourite_artists ON albums.artistId = favourite_artists.id
        WHERE albums.wanted = 1 AND albums.owned = 0
        """
    )
    suspend fun getWantedAlbums(): List<WantedAlbumRow>
}