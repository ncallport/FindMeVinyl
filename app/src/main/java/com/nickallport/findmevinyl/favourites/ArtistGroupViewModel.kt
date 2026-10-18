package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistGroupDao
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import java.util.UUID

/** One MusicBrainz artist in a group. [name] is the artist's own name on MusicBrainz. */
data class GroupMember(val artistId: String, val name: String)

/** Artists the user keeps together as one, under a [name] of their own. The first member comes first. */
data class ArtistGroup(val id: String, val name: String, val members: List<GroupMember>)

sealed class GroupResult {
    data class Saved(val group: ArtistGroup) : GroupResult()

    /** A group needs two different artists at least. */
    object TooFewMembers : GroupResult()

    /** [artistName] is already in the group called [groupName], and an artist is in one group at most. */
    data class AlreadyInGroup(val artistName: String, val groupName: String) : GroupResult()
}

/**
 * Joining artists into a group, changing a group and splitting it up. Nothing here touches MusicBrainz or the albums:
 * a group is only a name and a list of artist ids, so splitting it up puts everything back as it was. The group
 * takes the place in the favourites of its first member, and members that were favourites keep their own rows,
 * hidden while they are in a group, so they come back where they were.
 *
 * Each change is checked before anything is written, and written in [inTransaction], so a failure half way does
 * not leave a group with no members.
 */
class ArtistGroupViewModel(
    private val artistDao: ArtistDao,
    private val groupDao: ArtistGroupDao,
    private val newId: () -> String = { GROUP_PREFIX + UUID.randomUUID() },
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { it() }
) {
    suspend fun load(groupId: String): ArtistGroup? {
        val group = groupDao.getGroup(groupId) ?: return null
        return ArtistGroup(group.id, group.name, groupDao.getMembers(groupId).map { GroupMember(it.artistId, it.name) })
    }

    suspend fun groupOf(artistId: String): ArtistGroup? = groupDao.groupIdOf(artistId)?.let { load(it) }

    suspend fun create(name: String, members: List<GroupMember>): GroupResult {
        val distinct = members.distinctBy { it.artistId }
        if (distinct.size < 2) return GroupResult.TooFewMembers
        conflict(distinct, ownGroupId = null)?.let { return it }

        val id = newId()
        val finalName = name.trim().ifEmpty { distinct.first().name }
        // In the place of the first member, or first of all when it was not a favourite.
        val position = artistDao.getById(distinct.first().artistId)?.position ?: (artistDao.minPosition()?.minus(1) ?: 0)
        inTransaction {
            groupDao.insertGroup(ArtistGroupEntity(id, finalName, position))
            groupDao.insertMembers(distinct.mapIndexed { i, m -> ArtistGroupMemberEntity(m.artistId, id, m.name, i) })
        }
        return GroupResult.Saved(ArtistGroup(id, finalName, distinct))
    }

    suspend fun update(groupId: String, name: String, members: List<GroupMember>): GroupResult {
        if (groupDao.getGroup(groupId) == null) return GroupResult.TooFewMembers
        val distinct = members.distinctBy { it.artistId }
        if (distinct.size < 2) return GroupResult.TooFewMembers
        conflict(distinct, ownGroupId = groupId)?.let { return it }

        val finalName = name.trim().ifEmpty { distinct.first().name }
        inTransaction {
            groupDao.deleteMembersOf(groupId)
            groupDao.insertMembers(distinct.mapIndexed { i, m -> ArtistGroupMemberEntity(m.artistId, groupId, m.name, i) })
            groupDao.setName(groupId, finalName)
        }
        return GroupResult.Saved(ArtistGroup(groupId, finalName, distinct))
    }

    suspend fun ungroup(groupId: String) {
        inTransaction {
            groupDao.deleteMembersOf(groupId)
            groupDao.deleteGroup(groupId)
        }
    }

    /** The first member that is in some other group than [ownGroupId], if there is one. */
    private suspend fun conflict(members: List<GroupMember>, ownGroupId: String?): GroupResult.AlreadyInGroup? {
        for (member in members) {
            val other = groupDao.groupIdOf(member.artistId) ?: continue
            if (other == ownGroupId) continue
            return GroupResult.AlreadyInGroup(member.name, groupDao.getGroup(other)?.name.orEmpty())
        }
        return null
    }

    companion object {
        const val GROUP_PREFIX = "group/"

        /** Group ids start with `group/`, so they are never taken for MusicBrainz artist ids. */
        fun isGroupId(id: String): Boolean = id.startsWith(GROUP_PREFIX)
    }
}
