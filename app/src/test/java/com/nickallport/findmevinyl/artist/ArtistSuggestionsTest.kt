package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistSuggestionsTest {

    private val favourites = listOf(
        FavouriteArtistUiModel("1", "Joy Division"),
        FavouriteArtistUiModel("2", "New Order"),
        FavouriteArtistUiModel("3", "The Joy Formidable"),
        FavouriteArtistUiModel("4", "Talk Talk")
    )

    @Test
    fun `favourites whose name has every word typed are suggested, in their order`() {
        val result = ArtistSuggestions.favouritesMatching("joy", favourites)

        assertEquals(listOf("1", "3"), result.map { it.id })
        assertEquals(true, result.all { it.favourite })
    }

    @Test
    fun `every word has to be in the name`() {
        assertEquals(listOf("1"), ArtistSuggestions.favouritesMatching("joy div", favourites).map { it.id })
        assertEquals(emptyList<String>(), ArtistSuggestions.favouritesMatching("joy order", favourites).map { it.id })
    }

    @Test
    fun `a word can be the start or the middle of a word, whatever the case`() {
        assertEquals(listOf("4"), ArtistSuggestions.favouritesMatching("ALK", favourites).map { it.id })
    }

    @Test
    fun `one letter suggests no favourites, two do`() {
        assertEquals(emptyList<String>(), ArtistSuggestions.favouritesMatching("j", favourites).map { it.id })
        assertEquals(listOf("1", "3"), ArtistSuggestions.favouritesMatching("jo", favourites).map { it.id })
    }

    @Test
    fun `at most three favourites are suggested`() {
        val many = (1..6).map { FavouriteArtistUiModel("$it", "Band $it") }

        assertEquals(3, ArtistSuggestions.favouritesMatching("band", many).size)
    }

    private fun remote(id: String) = ArtistSuggestion(id, "Artist $id", "Band", favourite = false)

    @Test
    fun `the favourites come first and an artist already among them is not listed twice`() {
        val local = ArtistSuggestions.favouritesMatching("joy", favourites)

        val merged = ArtistSuggestions.merge(local, listOf(remote("1"), remote("9"), remote("8")))

        assertEquals(listOf("1", "3", "9", "8"), merged.map { it.id })
        assertEquals(true, merged.first().favourite)
        assertEquals(false, merged.last().favourite)
    }

    @Test
    fun `no more than six are listed in all`() {
        val local = ArtistSuggestions.favouritesMatching("band", (1..6).map { FavouriteArtistUiModel("f$it", "Band $it") })

        val merged = ArtistSuggestions.merge(local, (1..5).map { remote("r$it") })

        assertEquals(6, merged.size)
        assertEquals(listOf("f1", "f2", "f3", "r1", "r2", "r3"), merged.map { it.id })
    }
}
