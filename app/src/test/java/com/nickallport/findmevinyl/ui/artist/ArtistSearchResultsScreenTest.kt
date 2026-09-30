package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
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
    fun `tapping a result invokes the callback with its id`() {
        var selectedId: String? = null
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
            ArtistSearchResultsScreen(state = state, onArtistSelected = { selectedId = it })
        }

        composeTestRule.onNodeWithTag("artist_result_1").performClick()

        Assert.assertEquals("1", selectedId)
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