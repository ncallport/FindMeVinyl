package com.nickallport.findmevinyl.storage

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Several MusicBrainz artists the user keeps together as one, under a name of their own. MusicBrainz is not
 * changed. The group stands in the favourites where its first member would be.
 */
@Entity(tableName = "artist_groups")
data class ArtistGroupEntity(
    /** `group/` and a random id, so it can never be mistaken for a MusicBrainz artist id. */
    @PrimaryKey val id: String,
    val name: String,
    /** Where the group sits in the user's own order of favourites, with the favourite artists. Lowest first. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
    /** The genre the user chose for the group, lower case. Null: none chosen. */
    val genre: String? = null,
    /** The genre with the most votes among the members on MusicBrainz, found by the app. A [genre] the user chose wins. */
    val autoGenre: String? = null
)

/** One MusicBrainz artist in a group. An artist is in at most one group. */
@Entity(tableName = "artist_group_members")
data class ArtistGroupMemberEntity(
    @PrimaryKey val artistId: String,
    val groupId: String,
    /** The artist's name on MusicBrainz, kept so the group can be shown without asking for it. */
    val name: String,
    /** The order of the members within the group. The first is the one whose picture the group uses. */
    @ColumnInfo(defaultValue = "0") val ordinal: Int = 0
)

@Dao
interface ArtistGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ArtistGroupEntity): Long

    /** An artist already in a group moves to the new one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ArtistGroupMemberEntity>): List<Long>

    @Query("SELECT * FROM artist_groups")
    suspend fun getGroups(): List<ArtistGroupEntity>

    @Query("SELECT * FROM artist_groups WHERE id = :groupId")
    suspend fun getGroup(groupId: String): ArtistGroupEntity?

    @Query("SELECT * FROM artist_group_members WHERE groupId = :groupId ORDER BY ordinal")
    suspend fun getMembers(groupId: String): List<ArtistGroupMemberEntity>

    @Query("SELECT * FROM artist_group_members")
    suspend fun getAllMembers(): List<ArtistGroupMemberEntity>

    @Query("SELECT groupId FROM artist_group_members WHERE artistId = :artistId")
    suspend fun groupIdOf(artistId: String): String?

    @Query("UPDATE artist_groups SET name = :name WHERE id = :groupId")
    suspend fun setName(groupId: String, name: String): Int

    @Query("UPDATE artist_groups SET position = :position WHERE id = :groupId")
    suspend fun setPosition(groupId: String, position: Int): Int

    @Query("UPDATE artist_groups SET genre = :genre WHERE id = :groupId")
    suspend fun setGenre(groupId: String, genre: String?): Int

    @Query("UPDATE artist_groups SET autoGenre = :genre WHERE id = :groupId")
    suspend fun setAutoGenre(groupId: String, genre: String?): Int

    @Query("DELETE FROM artist_group_members WHERE groupId = :groupId")
    suspend fun deleteMembersOf(groupId: String): Int

    @Query("DELETE FROM artist_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String): Int
}
