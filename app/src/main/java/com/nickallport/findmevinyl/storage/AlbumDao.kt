package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfNotExists(album: AlbumEntity)

    @Query("SELECT * FROM albums WHERE artistId = :artistId")
    fun getAlbumsForArtist(artistId: String): List<AlbumEntity>

    @Query("UPDATE albums SET owned = :owned WHERE id = :albumId")
    fun setOwned(albumId: String, owned: Boolean)
}
