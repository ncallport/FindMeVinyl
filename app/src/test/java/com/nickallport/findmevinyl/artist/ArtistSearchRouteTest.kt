package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistSearchRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the idle prompt before any search`() {
        composeTestRule.setContent {
            ArtistSearchRoute(search = { ArtistSearchUiState.Success(emptyList()) }, onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Search for an artist to get started").assertExists()
    }

    @Test
    fun `searching shows the results once the search completes`() {
        val fakeSearch: suspend (String) -> ArtistSearchUiState = {
            ArtistSearchUiState.Success(listOf(ArtistDisplayItem("1", "Genesis", "Band from GB")))
        }

        composeTestRule.setContent {
            ArtistSearchRoute(search = fakeSearch, onArtistSelected = {})
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Genesis")
        composeTestRule.onNodeWithTag("search_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_result_1").assertExists()
    }
}
