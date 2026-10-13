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
import androidx.compose.ui.test.assertTextContains
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

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

    @Test
    fun `shows previous results when given a starting state`() {
        val previous = ArtistSearchUiState.Success(
            listOf(ArtistDisplayItem("1", "Genesis", "Band from GB"))
        )

        composeTestRule.setContent {
            ArtistSearchRoute(
                search = { previous },
                onArtistSelected = {},
                initialState = previous
            )
        }

        composeTestRule.onNodeWithTag("artist_result_1").assertExists()
    }

    @Test
    fun `starts with the query it was given`() {
        composeTestRule.setContent {
            ArtistSearchRoute(
                search = { ArtistSearchUiState.Success(emptyList()) },
                onArtistSelected = {},
                initialQuery = "Genesis"
            )
        }

        composeTestRule.onNodeWithTag("search_field").assertTextContains("Genesis")
    }

    @Test
    fun `reports changes to the query`() {
        var latest: String? = null

        composeTestRule.setContent {
            ArtistSearchRoute(
                search = { ArtistSearchUiState.Success(emptyList()) },
                onArtistSelected = {},
                onQueryChange = { latest = it }
            )
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Genesis")

        assertEquals("Genesis", latest)
    }

    @Test
    fun `reports the finished search result but not the loading state`() {
        val result = ArtistSearchUiState.Success(
            listOf(ArtistDisplayItem("1", "Genesis", "Band from GB"))
        )
        val reported = mutableListOf<ArtistSearchUiState>()

        composeTestRule.setContent {
            ArtistSearchRoute(
                search = { result },
                onArtistSelected = {},
                onStateChange = { reported.add(it) }
            )
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Genesis")
        composeTestRule.onNodeWithTag("search_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf<ArtistSearchUiState>(result), reported)
    }
}
