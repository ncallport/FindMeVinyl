package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import java.text.Normalizer

/**
 * Narrows the favourites to the artists whose name begins with what was typed, so a long list can be cut down
 * letter by letter. Capitals and accents do not count, and a leading "The" may be typed or left off: "smi" and
 * "the s" both find The Smiths. A group is found by its own name or by the start of one of its artists' names.
 * Nothing typed keeps everyone, in the order given.
 */
object FavouriteFilter {

    fun apply(artists: List<FavouriteArtistUiModel>, text: String): List<FavouriteArtistUiModel> {
        val wanted = simplify(text)
        if (wanted.isEmpty()) return artists
        return artists.filter { matches(it, wanted) }
    }

    private fun matches(artist: FavouriteArtistUiModel, wanted: String): Boolean =
        (listOf(artist.name) + artist.memberNames).any { name ->
            simplify(name).startsWith(wanted) || simplify(NameSort.key(name)).startsWith(wanted)
        }

    private fun simplify(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
}
