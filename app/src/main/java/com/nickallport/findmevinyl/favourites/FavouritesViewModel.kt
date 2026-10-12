package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel

class FavouritesViewModel(private val artistDao: ArtistDao) {
    suspend fun loadFavourites(): List<FavouriteArtistUiModel> {
        return artistDao.getAllFavourites()
            .sortedBy { it.name.lowercase() }
            .map { FavouriteArtistUiModel(id = it.id, name = it.name) }
    }
}