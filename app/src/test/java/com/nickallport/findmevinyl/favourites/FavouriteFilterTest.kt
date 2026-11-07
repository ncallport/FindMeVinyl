package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteFilterTest {

    private fun artists(vararg names: String) = names.mapIndexed { i, name -> FavouriteArtistUiModel("id$i", name) }

    private fun names(list: List<FavouriteArtistUiModel>) = list.map { it.name }

    private val all = artists("Bob Dylan", "Slade", "Sleep", "Slowdive", "Simple Minds", "The Smiths", "Genesis")

    @Test
    fun `nothing typed keeps every artist in the order given`() {
        assertEquals(names(all), names(FavouriteFilter.apply(all, "")))
        assertEquals(names(all), names(FavouriteFilter.apply(all, "   ")))
    }

    @Test
    fun `one letter keeps the artists that begin with it`() {
        assertEquals(
            listOf("Slade", "Sleep", "Slowdive", "Simple Minds", "The Smiths"),
            names(FavouriteFilter.apply(all, "S"))
        )
    }

    @Test
    fun `each letter narrows it further`() {
        assertEquals(listOf("Slade", "Sleep", "Slowdive"), names(FavouriteFilter.apply(all, "Sl")))
        assertEquals(listOf("Sleep"), names(FavouriteFilter.apply(all, "Sle")))
    }

    @Test
    fun `capitals do not matter`() {
        assertEquals(listOf("Slade", "Sleep", "Slowdive"), names(FavouriteFilter.apply(all, "sL")))
    }

    @Test
    fun `an artist is found by the start of its name, not by letters in the middle`() {
        assertEquals(emptyList<String>(), names(FavouriteFilter.apply(all, "ade")))
        assertEquals(emptyList<String>(), names(FavouriteFilter.apply(all, "dylan")))
    }

    @Test
    fun `a leading The does not have to be typed, and may be`() {
        assertEquals(listOf("The Smiths"), names(FavouriteFilter.apply(all, "smi")))
        assertEquals(listOf("The Smiths"), names(FavouriteFilter.apply(all, "the s")))
        assertEquals(listOf("The Smiths"), names(FavouriteFilter.apply(all, "The")))
    }

    @Test
    fun `a space after the words typed so far is fine`() {
        assertEquals(listOf("Simple Minds"), names(FavouriteFilter.apply(all, "simple ")))
        assertEquals(listOf("Simple Minds"), names(FavouriteFilter.apply(all, "Simple M")))
    }

    @Test
    fun `accents do not matter`() {
        val list = artists("Beyoncé", "Björk", "Blur")
        assertEquals(listOf("Beyoncé"), names(FavouriteFilter.apply(list, "beyonce")))
        assertEquals(listOf("Björk"), names(FavouriteFilter.apply(list, "bjor")))
        assertEquals(listOf("Björk"), names(FavouriteFilter.apply(list, "Björ")))
    }

    @Test
    fun `a group is found by its own name or by the start of one of its artists`() {
        val group = FavouriteArtistUiModel(
            "group/1", "Nick Cave", isGroup = true,
            memberIds = listOf("a", "b"), memberNames = listOf("Nick Cave", "Nick Cave & The Bad Seeds")
        )
        val list = listOf(group, FavouriteArtistUiModel("c", "Genesis"))

        assertEquals(listOf("Nick Cave"), names(FavouriteFilter.apply(list, "nick")))
        assertEquals(emptyList<String>(), names(FavouriteFilter.apply(list, "bad")))
        assertEquals(listOf("Nick Cave"), names(FavouriteFilter.apply(list, "nick cave &")))
    }

    @Test
    fun `an artist in a group is not found by letters in the middle of a member's name`() {
        val group = FavouriteArtistUiModel(
            "group/1", "Roxy Music", isGroup = true,
            memberIds = listOf("a", "b"), memberNames = listOf("Roxy Music", "Bryan Ferry")
        )

        assertEquals(listOf("Roxy Music"), names(FavouriteFilter.apply(listOf(group), "bry")))
        assertEquals(emptyList<String>(), names(FavouriteFilter.apply(listOf(group), "ferry")))
    }
}
