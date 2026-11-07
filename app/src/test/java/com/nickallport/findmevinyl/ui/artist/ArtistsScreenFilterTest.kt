package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The filter box beside List and Grid, and the count in the title over the favourites. */
@RunWith(RobolectricTestRunner::class)
class ArtistsScreenFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val favourites = listOf(
        FavouriteArtistUiModel("1", "Roxy Music"), FavouriteArtistUiModel("2", "Genesis"),
        FavouriteArtistUiModel("3", "Slade"), FavouriteArtistUiModel("4", "Sleep")
    )

    private fun show(list: List<FavouriteArtistUiModel> = favourites, startInGrid: Boolean = false) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistsScreen(onSearch = {}, loadFavourites = { list }, onArtistSelected = {}, startInGrid = startInGrid)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun type(text: String) {
        composeTestRule.onNodeWithTag("favourites_filter").performTextInput(text)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `the title says how many artists there are`() {
        show()

        composeTestRule.onNodeWithTag("favourites_title").assertTextEquals("Your favourites (4 artists)")
    }

    @Test
    fun `the title says artist for one`() {
        show(listOf(FavouriteArtistUiModel("1", "Roxy Music")))

        composeTestRule.onNodeWithTag("favourites_title").assertTextEquals("Your favourites (1 artist)")
    }

    @Test
    fun `the title has no count while there are no favourites`() {
        show(emptyList())

        composeTestRule.onNodeWithTag("favourites_title").assertTextEquals("Your favourites")
    }

    @Test
    fun `there is a filter box beside the List and Grid choices`() {
        show()

        composeTestRule.onNodeWithTag("favourites_filter").assertExists()
        composeTestRule.onNodeWithTag("favourites_view_list").assertExists()
        composeTestRule.onNodeWithTag("favourites_view_grid").assertExists()
    }

    @Test
    fun `typing keeps the artists that begin with the letters`() {
        show()

        type("S")
        composeTestRule.onNodeWithText("Slade").assertExists()
        composeTestRule.onNodeWithText("Sleep").assertExists()
        composeTestRule.onNodeWithText("Roxy Music").assertDoesNotExist()
        composeTestRule.onNodeWithText("Genesis").assertDoesNotExist()

        type("le")
        composeTestRule.onNodeWithText("Sleep").assertExists()
        composeTestRule.onNodeWithText("Slade").assertDoesNotExist()
    }

    @Test
    fun `the title says how many of the artists match`() {
        show()

        type("sl")

        composeTestRule.onNodeWithTag("favourites_title").assertTextEquals("Your favourites (2 of 4 artists)")
    }

    @Test
    fun `the grid is filtered too`() {
        show(startInGrid = true)

        type("sl")

        composeTestRule.onNodeWithTag("favourite_artist_3").assertExists()
        composeTestRule.onNodeWithTag("favourite_artist_4").assertExists()
        composeTestRule.onNodeWithTag("favourite_artist_1").assertDoesNotExist()
    }

    @Test
    fun `when nobody matches it says so`() {
        show()

        type("zz")

        composeTestRule.onNodeWithTag("favourites_no_match").assertExists()
        composeTestRule.onNodeWithTag("favourites_title").assertTextEquals("Your favourites (0 of 4 artists)")
    }

    @Test
    fun `nobody matching is not the same as having no favourites`() {
        show()

        type("zz")

        composeTestRule.onNodeWithText("No favourite artists yet").assertDoesNotExist()
    }

    @Test
    fun `the clear button empties the filter and brings everyone back`() {
        show()
        type("sl")

        composeTestRule.onNodeWithTag("favourites_filter_clear").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithTag("favourites_title").assertTextEquals("Your favourites (4 artists)")
        composeTestRule.onNodeWithTag("favourites_filter_clear").assertDoesNotExist()
    }

    @Test
    fun `there is no clear button while the filter is empty`() {
        show()

        composeTestRule.onNodeWithTag("favourites_filter_clear").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the filter is still there after the phone is turned`() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            FindMeVinylTheme { ArtistsScreen(onSearch = {}, loadFavourites = { favourites }, onArtistSelected = {}) }
        }
        waitForIdle()
        onNodeWithTag("favourites_filter").performTextInput("sl")
        waitForIdle()

        restoration.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithTag("favourites_title").assertTextEquals("Your favourites (2 of 4 artists)")
    }
}
