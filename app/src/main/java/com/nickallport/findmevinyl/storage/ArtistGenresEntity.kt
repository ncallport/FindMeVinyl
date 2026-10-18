package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * The genres MusicBrainz lists for an artist, with their votes, kept so that choosing a genre does not ask again
 * every time. [genres] is JSON, a list of name and count. [checkedAt] is when MusicBrainz was asked, in epoch
 * milliseconds. An artist with no genres on MusicBrainz is kept too, as an empty list.
 */
@Entity(tableName = "artist_genres")
data class ArtistGenresEntity(
    @PrimaryKey val artistId: String,
    val genres: String,
    val checkedAt: Long
)

@Dao
interface ArtistGenresDao {
    @Query("SELECT * FROM artist_genres WHERE artistId = :artistId")
    suspend fun get(artistId: String): ArtistGenresEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(genres: ArtistGenresEntity): Long
}
