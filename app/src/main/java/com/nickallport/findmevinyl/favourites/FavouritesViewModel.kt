package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.ordering.Reorder
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistGroupDao
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel

/**
 * The favourite artists in the user's own order. Artists the user has not ordered yet (equal
 * positions, as after the upgrade that added them) stay in name order.
 */
class FavouritesViewModel(private val artistDao: ArtistDao, private val groupDao: ArtistGroupDao) {
    suspend fun loadFavourites(): List<FavouriteArtistUiModel> {
        return ordered().map {
            val members = it.memberIds
            if (members == null) {
                FavouriteArtistUiModel(id = it.id, name = it.name, genre = it.genre)
            } else {
                FavouriteArtistUiModel(id = it.id, name = it.name, isGroup = true, memberIds = members, memberNames = it.memberNames, genre = it.genre)
            }
        }
    }

    suspend fun moveUp(artistId: String) = move(artistId, -1)

    suspend fun moveDown(artistId: String) = move(artistId, 1)

    /**
     * Puts the favourites in the order of [ids], as after dragging a picture to a new place. Ids that are not
     * favourites are ignored, and favourites not named keep their order after the named ones.
     */
    suspend fun reorder(ids: List<String>) {
        val current = ordered()
        val known = current.map { it.id }.toSet()
        val named = ids.filter { it in known }.distinct()
        val order = named + current.map { it.id }.filter { it !in named }
        val positions = current.associate { it.id to it.position }
        order.forEachIndexed { index, id ->
            if (positions[id] != index) writePosition(id, index)
        }
    }

    /** One favourite in the list: an artist, or a group (with its member ids). */
    private class Entry(
        val id: String,
        val name: String,
        val position: Int,
        val memberIds: List<String>? = null,
        val memberNames: List<String> = emptyList(),
        val genre: String? = null
    )

    /**
     * The favourites in the user's order: the artists that are not in a group, and the groups, as one list. An
     * artist in a group is not listed, but keeps its own row, so it comes back where it was if the group is split.
     */
    private suspend fun ordered(): List<Entry> {
        val members = groupDao.getAllMembers()
        val inGroup = members.map { it.artistId }.toSet()
        val membersByGroup = members.groupBy { it.groupId }.mapValues { (_, list) -> list.sortedBy { it.ordinal } }
        val artists = artistDao.getAllFavourites().filter { it.id !in inGroup }.map { Entry(it.id, it.name, it.position, genre = it.genre ?: it.autoGenre) }
        val groups = groupDao.getGroups().mapNotNull { group ->
            membersByGroup[group.id]?.takeIf { it.isNotEmpty() }?.let { list ->
                Entry(group.id, group.name, group.position, list.map { it.artistId }, list.map { it.name }, group.genre ?: group.autoGenre)
            }
        }
        return (artists + groups).sortedWith(compareBy<Entry> { it.position }.thenBy { it.name.lowercase() })
    }

    private suspend fun writePosition(id: String, position: Int) {
        if (ArtistGroupViewModel.isGroupId(id)) groupDao.setPosition(id, position) else artistDao.setPosition(id, position)
    }

    // Every artist is given its own position, so artists that were never ordered stop being ties.
    private suspend fun move(artistId: String, delta: Int) {
        val current = ordered()
        val moved = Reorder.swapped(current.map { it.id }, artistId, delta) ?: return
        val positions = current.associate { it.id to it.position }
        moved.forEachIndexed { index, id ->
            if (positions[id] != index) writePosition(id, index)
        }
    }
}
