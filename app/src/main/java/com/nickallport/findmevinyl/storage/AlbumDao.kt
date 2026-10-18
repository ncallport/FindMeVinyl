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

    /** Saves what a look at the album's releases found: its formats and its statuses, together. */
    @Query("UPDATE albums SET formats = :formats, statuses = :statuses, formatsCheckedAt = :checkedAt WHERE id = :albumId")
    suspend fun setChecked(albumId: String, formats: String, statuses: String, checkedAt: Long): Int

    /** Saves the user's own copy details, or clears them with nulls. Nothing else about the album changes. */
    @Query("UPDATE albums SET userFormats = :userFormats, userNote = :userNote WHERE id = :albumId")
    suspend fun setCopy(albumId: String, userFormats: String?, userNote: String?): Int

    /** Saves the pressing the user chose as their copy and a line that describes it, or takes it away with nulls. */
    @Query("UPDATE albums SET myPressingId = :releaseId, myPressingText = :text WHERE id = :albumId")
    suspend fun setMyPressing(albumId: String, releaseId: String?, text: String?): Int

    /** Every album the user owns or wants, or has recorded details of a copy for, for a backup. */
    @Query(
        "SELECT * FROM albums WHERE owned = 1 OR wanted = 1 OR userFormats IS NOT NULL OR userNote IS NOT NULL " +
            "OR myPressingId IS NOT NULL"
    )
    suspend fun getOwnedOrWanted(): List<AlbumEntity>

    /** Wants the album at a given place in its artist's want list. */
    @Query("UPDATE albums SET wanted = 1, wantPosition = :position WHERE id = :albumId")
    suspend fun setWantedAt(albumId: String, position: Int): Int

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
                 WHERE other.artistId = albums.artistId AND other.wanted = 1
                   AND other.id != albums.id) END
        WHERE id = :albumId
        """
    )
    suspend fun markWanted(albumId: String): Int

    @Query("UPDATE albums SET wanted = 0 WHERE artistId = :artistId")
    suspend fun clearWantedForArtist(artistId: String): Int

    /**
     * The albums on the want list, each with the name and place of whom it is listed under: its artist, or
     * the group of artists the artist is in, so a group's wanted albums come together under its name. An
     * album of an artist that is neither a favourite nor in a group is not listed. [WantedAlbumRow.artistId]
     * is the id of the group, or of the artist.
     */
    @Query(
        """
        SELECT albums.id AS albumId,
               COALESCE(artist_groups.id, albums.artistId) AS artistId,
               COALESCE(artist_groups.name, favourite_artists.name) AS artistName,
               albums.title AS albumTitle,
               albums.firstReleaseDate AS firstReleaseDate,
               COALESCE(artist_groups.position, favourite_artists.position) AS artistPosition,
               albums.wantPosition AS wantPosition
        FROM albums
        LEFT JOIN artist_group_members ON albums.artistId = artist_group_members.artistId
        LEFT JOIN artist_groups ON artist_groups.id = artist_group_members.groupId
        LEFT JOIN favourite_artists ON albums.artistId = favourite_artists.id
        WHERE albums.wanted = 1
          AND (artist_groups.id IS NOT NULL OR favourite_artists.id IS NOT NULL)
        """
    )
    suspend fun getWantedAlbums(): List<WantedAlbumRow>
}