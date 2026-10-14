package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val favourites = listOf(FavouriteArtistUiModel("1", "Roxy Music"), FavouriteArtistUiModel("2", "Genesis"))

    @Test
    fun `shows a search field and the favourite artists`() {
        composeTestRule.setContent {
            FindMeVinylTheme { ArtistsScreen(onSearch = {}, loadFavourites = { favourites }, onArtistSelected = {}) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_field").assertExists()
        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("Genesis").assertExists()
    }

    @Test
    fun `says when there are no favourites yet`() {
        composeTestRule.setContent {
            FindMeVinylTheme { ArtistsScreen(onSearch = {}, loadFavourites = { emptyList() }, onArtistSelected = {}) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No favourite artists yet").assertExists()
    }

    @Test
    fun `searching with the keyboard goes to the results with the typed text`() {
        var searched: String? = null
        composeTestRule.setContent {
            FindMeVinylTheme { ArtistsScreen(onSearch = { searched = it }, loadFavourites = { favourites }, onArtistSelected = {}) }
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Kate Bush")
        composeTestRule.onNodeWithTag("search_field").performImeAction()

        assertEquals("Kate Bush", searched)
    }

    @Test
    fun `tapping a favourite opens it`() {
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent {
            FindMeVinylTheme { ArtistsScreen(onSearch = {}, loadFavourites = { favourites }, onArtistSelected = { selected = it }) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_artist_2").performClick()

        assertEquals("Genesis", selected?.name)
    }

    @Test
    fun `has no search button and no link menu`() {
        composeTestRule.setContent {
            FindMeVinylTheme { ArtistsScreen(onSearch = {}, loadFavourites = { favourites }, onArtistSelected = {}) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourites_link").assertDoesNotExist()
        composeTestRule.onNodeWithTag("want_list_link").assertDoesNotExist()
        composeTestRule.onNodeWithTag("find_shops_link").assertDoesNotExist()
    }
}
