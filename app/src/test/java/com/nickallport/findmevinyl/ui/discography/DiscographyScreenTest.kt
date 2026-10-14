package com.nickallport.findmevinyl.ui.discography

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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

    private val oneAlbum = mapOf(
        DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Foxtrot", owned = false, wanted = false))
    )

    @Test
    fun `shows a footer while more albums are still loading`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = oneAlbum, onToggleOwned = {}, loadingMore = true)
        }

        composeTestRule.onNodeWithTag("loading_more").assertExists()
        composeTestRule.onNodeWithText("Loading more albums…").assertExists()
    }

    @Test
    fun `has no footer once everything has loaded`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = oneAlbum, onToggleOwned = {})
        }

        composeTestRule.onNodeWithTag("loading_more").assertDoesNotExist()
        composeTestRule.onNodeWithTag("load_error").assertDoesNotExist()
    }

    @Test
    fun `says some albums could not be loaded and offers to try again`() {
        var retries = 0

        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = oneAlbum, onToggleOwned = {},
                loadError = "HTTP 503 Server Error", onRetry = { retries++ }
            )
        }

        composeTestRule.onNodeWithTag("load_error").assertExists()
        composeTestRule.onNodeWithText("Couldn't load all the albums").assertExists()
        composeTestRule.onNodeWithText("HTTP 503 Server Error").assertExists()

        composeTestRule.onNodeWithTag("retry_button").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `keeps its place when albums are added above the ones on screen`() {
        val original = (10..40).map { AlbumUiModel("$it", "Album $it", owned = false, wanted = false) }
        var albums by mutableStateOf(original)

        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = mapOf(DiscographyCategory.STUDIO_ALBUM to albums),
                onToggleOwned = {}
            )
        }
        composeTestRule.onNodeWithTag("discography_list").performScrollToIndex(20)
        composeTestRule.onNodeWithText("Album 29").assertIsDisplayed()

        albums = (1..9).map { AlbumUiModel("$it", "Album $it", owned = false, wanted = false) } + original
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album 29").assertIsDisplayed()
    }

    @Test
    fun `tapping an album opens it with its id`() {
        var openedId: String? = null
        var toggledId: String? = null
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = grouped,
                onToggleOwned = { toggledId = it },
                onOpenAlbum = { openedId = it }
            )
        }

        composeTestRule.onNodeWithTag("album_row").performClick()

        assertEquals("1", openedId)
        assertEquals(null, toggledId)
    }

    @Test
    fun `tapping the Owned pill invokes the callback with the album id`() {
        var toggledId: String? = null
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = { toggledId = it })
        }

        composeTestRule.onNodeWithTag("owned_toggle").performClick()

        assertEquals("1", toggledId)
    }
}
