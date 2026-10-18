package com.nickallport.findmevinyl.favourites

import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteGenreGroupsTest {

    private fun artist(id: String, genre: String?) = FavouriteArtistUiModel(id, "Artist $id", genre = genre)

    @Test
    fun `the sections are in alphabetical order of the genre`() {
        val sections = FavouriteGenreGroups.group(
            listOf(artist("1", "post-punk"), artist("2", "folk"), artist("3", "progressive rock"))
        )

        assertEquals(listOf("folk", "post-punk", "progressive rock"), sections.map { it.genre })
    }

    @Test
    fun `artists with no genre come last, in a section of their own`() {
        val sections = FavouriteGenreGroups.group(listOf(artist("1", null), artist("2", "rock"), artist("3", null)))

        assertEquals(listOf<String?>("rock", null), sections.map { it.genre })
        assertEquals(listOf("1", "3"), sections.last().artists.map { it.id })
    }

    @Test
    fun `inside a section the order of the list is kept`() {
        val sections = FavouriteGenreGroups.group(
            listOf(artist("1", "rock"), artist("2", "folk"), artist("3", "rock"), artist("4", "folk"), artist("5", "rock"))
        )

        assertEquals(listOf("1", "3", "5"), sections.single { it.genre == "rock" }.artists.map { it.id })
        assertEquals(listOf("2", "4"), sections.single { it.genre == "folk" }.artists.map { it.id })
    }

    @Test
    fun `only genres that an artist has get a section`() {
        val sections = FavouriteGenreGroups.group(listOf(artist("1", "rock")))

        assertEquals(1, sections.size)
    }

    @Test
    fun `nothing gives no sections`() {
        assertEquals(emptyList<FavouriteGenreGroups.Section>(), FavouriteGenreGroups.group(emptyList()))
    }

    @Test
    fun `a genre that differs only in capitals or spaces at the ends is the same section`() {
        val sections = FavouriteGenreGroups.group(listOf(artist("1", "Post-Punk"), artist("2", " post-punk ")))

        assertEquals(1, sections.size)
        assertEquals(listOf("1", "2"), sections.single().artists.map { it.id })
        assertEquals("post-punk", sections.single().genre)
    }

    @Test
    fun `a blank genre counts as none`() {
        val sections = FavouriteGenreGroups.group(listOf(artist("1", "  "), artist("2", null)))

        assertEquals(listOf<String?>(null), sections.map { it.genre })
    }
}
