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

    /** Changes an album added by hand. An album from MusicBrainz is never changed this way. */
    @Query("UPDATE albums SET title = :title, firstReleaseDate = :date, category = :category, statuses = :statuses, userDescription = :description, userTracks = :tracks WHERE id = :albumId AND id LIKE 'custom/%'")
    suspend fun updateCustomAlbum(
        albumId: String, title: String, date: String?, category: String, statuses: String,
        description: String?, tracks: String?
    ): Int

    /** Records the cover image file of an album added by hand, or none with null. An album from MusicBrainz takes none. */
    @Query("UPDATE albums SET userCover = :fileName WHERE id = :albumId AND id LIKE 'custom/%'")
    suspend fun setUserCover(albumId: String, fileName: String?): Int

    /** Removes an album added by hand. An album from MusicBrainz is never removed this way. */
    @Query("DELETE FROM albums WHERE id = :albumId AND id LIKE 'custom/%'")
    suspend fun deleteCustomAlbum(albumId: String): Int

    /** The copies of the album the user owns, in the order they were listed. */
    @Query("SELECT * FROM album_copies WHERE albumId = :albumId ORDER BY position, id")
    suspend fun getCopies(albumId: String): List<AlbumCopyEntity>

    /** Every copy of every album, for the discography list and for a backup. */
    @Query("SELECT * FROM album_copies ORDER BY albumId, position, id")
    suspend fun getAllCopies(): List<AlbumCopyEntity>

    @Insert
    suspend fun insertCopies(copies: List<AlbumCopyEntity>): List<Long>

    /** Takes away the copies of the album other than [keep], which are the ones just saved. */
    @Query("DELETE FROM album_copies WHERE albumId = :albumId AND id NOT IN (:keep)")
    suspend fun deleteCopiesExcept(albumId: String, keep: List<Long>): Int

    /** Saves the pressing the user chose as their copy and a line that describes it, or takes it away with nulls. */
    @Query("UPDATE albums SET myPressingId = :releaseId, myPressingText = :text WHERE id = :albumId")
    suspend fun setMyPressing(albumId: String, releaseId: String?, text: String?): Int

    /** Every album the user owns or wants, or has recorded details of a copy for, for a backup. */
    @Query(
        "SELECT * FROM albums WHERE owned = 1 OR wanted = 1 OR myPressingId IS NOT NULL OR id LIKE 'custom/%' " +
            "OR EXISTS (SELECT 1 FROM album_copies WHERE album_copies.albumId = albums.id)"
    )
    suspend fun getOwnedOrWanted(): List<AlbumEntity>

    /** Wants the album at a given place in its artist's want list. */
    @Query("UPDATE albums SET wanted = 1, wantPosition = :position WHERE id = :albumId")
    suspend fun setWantedAt(albumId: String, position: Int): Int

    @Query("UPDATE albums SET owned = :owned WHERE id = :albumId")
    suspend fun setOwned(albumId: String, owned: Boolean): Int

    /** Wants or unwants the album. Taking the want off also forgets the format it was wanted on. */
    @Query("UPDATE albums SET wanted = :wanted, wantFormat = CASE WHEN :wanted = 1 THEN wantFormat ELSE NULL END WHERE id = :albumId")
    suspend fun setWanted(albumId: String, wanted: Boolean): Int

    /** Saves the format the album is wanted on, or null for any. */
    @Query("UPDATE albums SET wantFormat = :format WHERE id = :albumId")
    suspend fun setWantFormat(albumId: String, format: String?): Int

    @Query("UPDATE albums SET wantPosition = :position WHERE id = :albumId")
    suspend fun setWantPosition(albumId: String, position: Int): Int

    /** Wants the album and puts it last in the want list. An album already wanted keeps its place. */
    @Query(
        """
        UPDATE albums SET wanted = 1,
            wantPosition = CASE WHEN wanted = 1 THEN wantPosition ELSE
                (SELECT COALESCE(MAX(other.wantPosition), -1) + 1 FROM albums AS other
                 WHERE other.wanted = 1
                   AND other.id != albums.id) END
        WHERE id = :albumId
        """
    )
    suspend fun markWanted(albumId: String): Int

    @Query("UPDATE albums SET wanted = 0, wantFormat = NULL WHERE artistId = :artistId")
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
               albums.wantPosition AS wantPosition,
               albums.wantFormat AS wantFormat
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