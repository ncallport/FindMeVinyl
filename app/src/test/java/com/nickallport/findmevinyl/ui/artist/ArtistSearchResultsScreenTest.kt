package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistSearchResultsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows each result's name and description`() {
        val state = ArtistSearchUiState.Success(
            listOf(
                ArtistDisplayItem(
                    id = "1",
                    name = "Genesis",
                    description = "Band from GB, active since 1967"
                )
            )
        )

        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = state, onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Genesis").assertExists()
        composeTestRule.onNodeWithText("Band from GB, active since 1967").assertExists()
    }

    @Test
    fun `shows the error message when the search failed`() {
        val state = ArtistSearchUiState.Error("Something went wrong")

        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = state, onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }

    @Test
    fun `tapping a result invokes the callback with that artist`() {
        var selected: ArtistDisplayItem? = null
        val state = ArtistSearchUiState.Success(
            listOf(
                ArtistDisplayItem(id = "1", name = "Genesis", description = "Band from GB, active since 1967")
            )
        )

        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = state, onArtistSelected = { selected = it })
        }

        composeTestRule.onNodeWithTag("artist_result_1").performClick()

        assertEquals("1", selected?.id)
        assertEquals("Genesis", selected?.name)
    }

    private val twoArtists = ArtistSearchUiState.Success(
        listOf(
            ArtistDisplayItem(id = "1", name = "Genesis", description = "Band from GB"),
            ArtistDisplayItem(id = "2", name = "Roxy Music", description = "Band from GB")
        )
    )

    private fun picture(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows a placeholder picture in each result before any picture has loaded`() {
        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = twoArtists, onArtistSelected = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("artist_placeholder", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `asks for the picture of each result and shows the ones that come back`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            ArtistSearchResultsScreen(
                state = twoArtists,
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
            ArtistSearchResultsScreen(
                state = twoArtists, onArtistSelected = {}, loadArtistImage = { picture() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertTextEquals("Artist photos: Wikimedia Commons")
    }

    @Test
    fun `shows no credit while there is no picture`() {
        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = twoArtists, onArtistSelected = {}, loadArtistImage = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertDoesNotExist()
    }

    @Test
    fun `shows a prompt when idle`() {
        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = ArtistSearchUiState.Idle, onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Search for an artist to get started").assertExists()
    }

    @Test
    fun `shows a loading indicator while searching`() {
        composeTestRule.setContent {
            ArtistSearchResultsScreen(state = ArtistSearchUiState.Loading, onArtistSelected = {})
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertExists()
    }
}