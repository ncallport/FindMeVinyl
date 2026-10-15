package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.ordering.Reorder
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel

/**
 * The favourite artists in the user's own order. Artists the user has not ordered yet (equal
 * positions, as after the upgrade that added them) stay in name order.
 */
class FavouritesViewModel(private val artistDao: ArtistDao) {
    suspend fun loadFavourites(): List<FavouriteArtistUiModel> {
        return ordered().map { FavouriteArtistUiModel(id = it.id, name = it.name) }
    }

    suspend fun moveUp(artistId: String) = move(artistId, -1)

    suspend fun moveDown(artistId: String) = move(artistId, 1)

    private suspend fun ordered(): List<ArtistEntity> =
        artistDao.getAllFavourites().sortedWith(compareBy<ArtistEntity> { it.position }.thenBy { it.name.lowercase() })

    // Every artist is given its own position, so artists that were never ordered stop being ties.
    private suspend fun move(artistId: String, delta: Int) {
        val current = ordered()
        val moved = Reorder.swapped(current.map { it.id }, artistId, delta) ?: return
        val positions = current.associate { it.id to it.position }
        moved.forEachIndexed { index, id ->
            if (positions[id] != index) artistDao.setPosition(id, index)
        }
    }
}
