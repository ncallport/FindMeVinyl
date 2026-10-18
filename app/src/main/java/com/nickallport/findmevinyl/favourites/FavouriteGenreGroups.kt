package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.genre.GenreNames
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel

/**
 * The favourites split into one section for each genre, for the Artists page when it is grouped by genre. The
 * sections are in alphabetical order of the genre, with the artists that have none (a [Section.genre] of null)
 * last, and inside a section the artists keep the order they were given in, the user's own. A genre that
 * differs only in capitals, or in spaces at the ends, is the same section.
 */
object FavouriteGenreGroups {
    class Section(val genre: String?, val artists: List<FavouriteArtistUiModel>)

    fun group(artists: List<FavouriteArtistUiModel>): List<Section> {
        val byGenre = linkedMapOf<String?, MutableList<FavouriteArtistUiModel>>()
        for (artist in artists) {
            byGenre.getOrPut(GenreNames.normalise(artist.genre)) { mutableListOf() }.add(artist)
        }
        return byGenre.entries
            .sortedWith(compareBy<Map.Entry<String?, List<FavouriteArtistUiModel>>> { it.key == null }.thenBy { it.key })
            .map { Section(it.key, it.value) }
    }
}
