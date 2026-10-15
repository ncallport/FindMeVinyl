package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
    fun `shows the artist pictures that the loader gives`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FavouritesRoute(
                load = { listOf(FavouriteArtistUiModel("1", "Roxy Music")) },
                onArtistSelected = {},
                loadArtistImage = { id -> asked.add(id); Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888) }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
        assertEquals(listOf("1"), asked)
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

    @Test
    fun `moving an artist up tells the caller and shows the list again from the load`() {
        var order = listOf(
            FavouriteArtistUiModel("1", "Roxy Music"),
            FavouriteArtistUiModel("2", "The Smiths")
        )
        val moved = mutableListOf<String>()

        composeTestRule.setContent {
            FavouritesRoute(
                load = { order },
                onArtistSelected = {},
                onMoveUp = { id -> moved.add(id); order = order.reversed() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_up_2").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("2"), moved)
        // The Smiths is now first, so it cannot move up any more.
        composeTestRule.onNodeWithTag("favourite_up_2").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("favourite_up_1").assertIsEnabled()
    }

    @Test
    fun `moving an artist down tells the caller`() {
        var order = listOf(
            FavouriteArtistUiModel("1", "Roxy Music"),
            FavouriteArtistUiModel("2", "The Smiths")
        )
        val moved = mutableListOf<String>()

        composeTestRule.setContent {
            FavouritesRoute(
                load = { order },
                onArtistSelected = {},
                onMoveDown = { id -> moved.add(id); order = order.reversed() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_down_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1"), moved)
        composeTestRule.onNodeWithTag("favourite_down_1").assertIsNotEnabled()
    }
}
