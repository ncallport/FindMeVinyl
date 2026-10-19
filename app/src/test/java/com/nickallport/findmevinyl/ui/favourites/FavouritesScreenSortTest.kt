package com.nickallport.findmevinyl.ui.favourites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The favourites in alphabetical order, to find an artist quickly. */
@RunWith(RobolectricTestRunner::class)
class FavouritesScreenSortTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // In the user's own order, which is not alphabetical.
    private val artists = listOf(
        FavouriteArtistUiModel("1", "Roxy Music", genre = "art rock"),
        FavouriteArtistUiModel("2", "abc", genre = "art rock"),
        FavouriteArtistUiModel("3", "Joy Division", genre = "post-punk"),
        FavouriteArtistUiModel("4", "Big Country", genre = "post-punk")
    )

    private fun y(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    private fun assertOrder(vararg ids: String) {
        ids.toList().zipWithNext().forEach { (a, b) ->
            assertTrue("$a should be above $b", y("favourite_artist_$a") < y("favourite_artist_$b"))
        }
    }

    @Test
    fun `sorted, the list is in alphabetical order, ignoring capitals`() {
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}, sortAz = true) }

        assertOrder("2", "4", "3", "1")
    }

    @Test
    fun `not sorted, the list keeps the user's own order`() {
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}) }

        assertOrder("1", "2", "3", "4")
    }

    @Test
    fun `sorted, the grid is in alphabetical order too`() {
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}, grid = true, sortAz = true) }

        val tops = listOf("2", "4", "3", "1").map { composeTestRule.onNodeWithTag("favourite_artist_$it").fetchSemanticsNode().positionInRoot }
        // Four pictures fit in a row or two, and each comes after the one before: further down, or the same row further right.
        tops.zipWithNext().forEach { (a, b) -> assertTrue(b.y > a.y || (b.y == a.y && b.x > a.x)) }
    }

    @Test
    fun `sorted, there are no move buttons, and a line says how to change the order`() {
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}, sortAz = true) }

        composeTestRule.onNodeWithTag("favourite_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourite_down_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourites_az_hint").assertTextEquals("Sorted A to Z. Turn it off to change the order.")
    }

    @Test
    fun `sorted, the grid does not say a picture can be dragged`() {
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}, grid = true, sortAz = true) }

        composeTestRule.onNodeWithTag("favourites_grid_hint").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourites_az_hint").assertExists()
    }

    @Test
    fun `sorted, an artist can still be opened`() {
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = { selected = it }, sortAz = true) }

        composeTestRule.onNodeWithTag("favourite_artist_4").performClick()

        assertEquals(artists[3], selected)
    }

    @Test
    fun `sorted and grouped by genre, each genre has its artists in alphabetical order`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = artists, onArtistSelected = {}, groupByGenre = true, sortAz = true)
        }

        assertTrue(y("genre_header_art rock") < y("favourite_artist_2"))
        assertOrder("2", "1")
        assertTrue(y("favourite_artist_1") < y("genre_header_post-punk"))
        assertOrder("4", "3")
        composeTestRule.onNodeWithTag("favourites_genre_hint")
            .assertTextEquals("Grouped by genre and sorted A to Z. Turn them off to change the order.")
        composeTestRule.onNodeWithTag("favourites_az_hint").assertDoesNotExist()
    }

    @Test
    fun `grouped by genre but not sorted, each genre keeps the user's order`() {
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}, groupByGenre = true) }

        assertOrder("1", "2")
        assertOrder("3", "4")
    }

    @Test
    fun `sorting does not change the user's own order, which is there again when it is off`() {
        var sorted by mutableStateOf(true)
        composeTestRule.setContent { FavouritesScreen(artists = artists, onArtistSelected = {}, sortAz = sorted) }
        assertOrder("2", "4", "3", "1")

        composeTestRule.runOnIdle { sorted = false }
        composeTestRule.waitForIdle()

        assertOrder("1", "2", "3", "4")
    }

    @Test
    fun `a leading The is ignored, so The Smiths is under S`() {
        val named = listOf(
            FavouriteArtistUiModel("1", "Talk Talk"),
            FavouriteArtistUiModel("2", "The Smiths"),
            FavouriteArtistUiModel("3", "Simple Minds"),
            FavouriteArtistUiModel("4", "the cure"),
            FavouriteArtistUiModel("5", "Big Country")
        )
        composeTestRule.setContent { FavouritesScreen(artists = named, onArtistSelected = {}, sortAz = true) }

        assertOrder("5", "4", "3", "2", "1")
    }

    @Test
    fun `only the word The is ignored, not a name that starts with those letters, nor The on its own`() {
        val named = listOf(
            FavouriteArtistUiModel("1", "Theatre of Hate"),
            FavouriteArtistUiModel("2", "The"),
            FavouriteArtistUiModel("3", "Sparks"),
            FavouriteArtistUiModel("4", "Nick Cave")
        )
        composeTestRule.setContent { FavouritesScreen(artists = named, onArtistSelected = {}, sortAz = true) }

        // Nick Cave under N, Sparks under S, then "The" and "Theatre of Hate" under T.
        assertOrder("4", "3", "2", "1")
    }
}
