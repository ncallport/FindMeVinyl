package com.nickallport.findmevinyl.ui.favourites

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouritesRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the loaded favourites`() {
        val load: suspend () -> List<FavouriteArtistUiModel> = {
            listOf(
                FavouriteArtistUiModel("1", "Roxy Music"),
                FavouriteArtistUiModel("2", "The Smiths")
            )
        }

        composeTestRule.setContent {
            FavouritesRoute(load = load, onArtistSelected = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("The Smiths").assertExists()
    }

    @Test
    fun `shows the empty message when there are no favourites`() {
        composeTestRule.setContent {
            FavouritesRoute(load = { emptyList() }, onArtistSelected = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No favourite artists yet").assertExists()
    }

    @Test
    fun `tapping an artist invokes the callback with that artist`() {
        var selected: FavouriteArtistUiModel? = null

        composeTestRule.setContent {
            FavouritesRoute(
                load = { listOf(FavouriteArtistUiModel("1", "Roxy Music")) },
                onArtistSelected = { selected = it }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_artist_1").performClick()

        assertEquals("1", selected?.id)
        assertEquals("Roxy Music", selected?.name)
    }
}