package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows each favourite artist's name`() {
        val artists = listOf(
            FavouriteArtistUiModel("1", "Roxy Music"),
            FavouriteArtistUiModel("2", "The Smiths")
        )

        composeTestRule.setContent {
            FavouritesScreen(artists = artists, onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("The Smiths").assertExists()
    }

    private val twoArtists = listOf(
        FavouriteArtistUiModel("1", "Roxy Music"),
        FavouriteArtistUiModel("2", "The Smiths")
    )

    private fun picture(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows a placeholder picture for each artist before any picture has loaded`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("artist_placeholder", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `asks for the picture of each artist and shows the ones that come back`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FavouritesScreen(
                artists = twoArtists,
                onArtistSelected = {},
                loadArtistImage = { id -> asked.add(id); if (id == "1") picture() else null }
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(setOf("1", "2"), asked.toSet())
        composeTestRule.onAllNodesWithTag("artist_image", useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("artist_placeholder", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `credits Wikimedia Commons once a picture is shown`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { picture() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertTextEquals("Artist photos: Wikimedia Commons")
    }

    @Test
    fun `shows no credit while there is no picture`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertDoesNotExist()
    }

    @Test
    fun `shows a message when there are no favourites`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = emptyList(), onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("No favourite artists yet").assertExists()
    }

    @Test
    fun `tapping an artist invokes the callback with that artist`() {
        var selected: FavouriteArtistUiModel? = null
        val artists = listOf(FavouriteArtistUiModel("1", "Roxy Music"))

        composeTestRule.setContent {
            FavouritesScreen(artists = artists, onArtistSelected = { selected = it })
        }

        composeTestRule.onNodeWithTag("favourite_artist_1").performClick()

        assertEquals("1", selected?.id)
        assertEquals("Roxy Music", selected?.name)
    }
}