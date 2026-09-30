package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.discography.DiscographyCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscographyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows a header and the albums for each category present`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            ),
            DiscographyCategory.LIVE_ALBUM to listOf(
                AlbumUiModel("2", "Live at Pompeii", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {})
        }

        composeTestRule.onNodeWithText("Studio Albums").assertExists()
        composeTestRule.onNodeWithText("The Dark Side of the Moon").assertExists()
        composeTestRule.onNodeWithText("Live Albums").assertExists()
        composeTestRule.onNodeWithText("Live at Pompeii").assertExists()
    }

    @Test
    fun `categories with no albums are not shown`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {})
        }

        composeTestRule.onNodeWithText("Compilations").assertDoesNotExist()
    }

    @Test
    fun `tapping an album invokes the callback with its id`() {
        var toggledId: String? = null
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = { toggledId = it })
        }

        composeTestRule.onNodeWithTag("album_row").performClick()

        assertEquals("1", toggledId)
    }
}
