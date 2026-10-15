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

    /** One transaction for the whole list. Albums that already exist are left as they are. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfNotExist(albums: List<AlbumEntity>): List<Long>

    @Query("SELECT * FROM albums WHERE artistId = :artistId")
    suspend fun getAlbumsForArtist(artistId: String): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getById(albumId: String): AlbumEntity?

    /** An empty [formats] means the album was checked and no formats are known. */
    @Query("UPDATE albums SET formats = :formats, formatsCheckedAt = :checkedAt WHERE id = :albumId")
    suspend fun setFormats(albumId: String, formats: String, checkedAt: Long): Int

    @Query("UPDATE albums SET owned = :owned WHERE id = :albumId")
    suspend fun setOwned(albumId: String, owned: Boolean): Int

    @Query("UPDATE albums SET wanted = :wanted WHERE id = :albumId")
    suspend fun setWanted(albumId: String, wanted: Boolean): Int

    @Query("UPDATE albums SET wantPosition = :position WHERE id = :albumId")
    suspend fun setWantPosition(albumId: String, position: Int): Int

    /** Wants the album and puts it last in its artist's want list. An album already wanted keeps its place. */
    @Query(
        """
        UPDATE albums SET wanted = 1,
            wantPosition = CASE WHEN wanted = 1 THEN wantPosition ELSE
                (SELECT COALESCE(MAX(other.wantPosition), -1) + 1 FROM albums AS other
                 WHERE other.artistId = albums.artistId AND other.wanted = 1 AND other.owned = 0
                   AND other.id != albums.id) END
        WHERE id = :albumId
        """
    )
    suspend fun markWanted(albumId: String): Int

    @Query("UPDATE albums SET wanted = 0 WHERE artistId = :artistId")
    suspend fun clearWantedForArtist(artistId: String): Int

    @Query(
        """
        SELECT albums.id AS albumId,
               albums.artistId AS artistId,
               favourite_artists.name AS artistName,
               albums.title AS albumTitle,
               albums.firstReleaseDate AS firstReleaseDate,
               favourite_artists.position AS artistPosition,
               albums.wantPosition AS wantPosition
        FROM albums
        INNER JOIN favourite_artists ON albums.artistId = favourite_artists.id
        WHERE albums.wanted = 1 AND albums.owned = 0
        """
    )
    suspend fun getWantedAlbums(): List<WantedAlbumRow>
}