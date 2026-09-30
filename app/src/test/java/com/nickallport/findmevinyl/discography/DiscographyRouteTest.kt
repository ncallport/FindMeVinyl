package com.nickallport.findmevinyl.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscographyRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `loads and shows albums for the given artist`() {
        val fakeLoad: suspend (String) -> DiscographyUiState = { artistId ->
            DiscographyUiState.Success(
                mapOf(
                    DiscographyCategory.STUDIO_ALBUM to listOf(
                        AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false)
                    )
                )
            )
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "some-artist-id", load = fakeLoad, onToggleOwned = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `shows the error message when loading fails`() {
        val fakeLoad: suspend (String) -> DiscographyUiState = {
            DiscographyUiState.Error("Something went wrong")
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "some-artist-id", load = fakeLoad, onToggleOwned = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }
}
