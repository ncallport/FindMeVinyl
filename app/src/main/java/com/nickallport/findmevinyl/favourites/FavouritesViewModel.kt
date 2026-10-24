package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistGroupDao
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel

/**
 * The favourite artists A to Z ([NameSort]), each with how many of its albums are owned and wanted. An artist in a
 * group is not listed on its own, and the group is, by its own name, with its members' albums counted together.
 */
class FavouritesViewModel(
    private val artistDao: ArtistDao,
    private val groupDao: ArtistGroupDao,
    private val albumDao: AlbumDao
) {
    /**
     * The two blocks under the favourites, Various Artists and Soundtracks, with how many albums are owned and wanted. They
     * are always there, and are never in the favourites list.
     */
    suspend fun loadSpecials(): List<FavouriteArtistUiModel> {
        val various = albumDao.getAlbumCounts().firstOrNull { it.artistId == SpecialArtists.VARIOUS_ID }
        val soundtracks = albumDao.getSoundtrackCounts()
        return listOf(
            FavouriteArtistUiModel(
                SpecialArtists.VARIOUS_ID, SpecialArtists.VARIOUS_NAME, ownedCount = various?.owned ?: 0, wantedCount = various?.wanted ?: 0
            ),
            FavouriteArtistUiModel(
                SpecialArtists.SOUNDTRACKS_ID, SpecialArtists.SOUNDTRACKS_NAME, ownedCount = soundtracks.owned, wantedCount = soundtracks.wanted
            )
        )
    }

    suspend fun loadFavourites(): List<FavouriteArtistUiModel> {
        val members = groupDao.getAllMembers()
        val inGroup = members.map { it.artistId }.toSet()
        val membersByGroup = members.groupBy { it.groupId }.mapValues { (_, list) -> list.sortedBy { it.ordinal } }
        val counts = albumDao.getAlbumCounts().associateBy { it.artistId }

        val artists = artistDao.getAllFavourites().filter { it.id !in inGroup }.map {
            FavouriteArtistUiModel(
                id = it.id, name = it.name,
                ownedCount = counts[it.id]?.owned ?: 0, wantedCount = counts[it.id]?.wanted ?: 0
            )
        }
        val groups = groupDao.getGroups().mapNotNull { group ->
            membersByGroup[group.id]?.takeIf { it.isNotEmpty() }?.let { list ->
                val ids = list.map { it.artistId }
                FavouriteArtistUiModel(
                    id = group.id, name = group.name, isGroup = true, memberIds = ids, memberNames = list.map { it.name },
                    ownedCount = ids.sumOf { counts[it]?.owned ?: 0 }, wantedCount = ids.sumOf { counts[it]?.wanted ?: 0 }
                )
            }
        }
        return (artists + groups).sortedWith(compareBy(NameSort) { it.name })
    }
}
