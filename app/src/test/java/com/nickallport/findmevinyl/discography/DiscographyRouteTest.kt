package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals

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
            DiscographyRoute(artistId = "some-artist-id", artistName = "Genesis", load = fakeLoad, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `asks for the cover of each album and shows it`() {
        val asked = mutableListOf<String>()
        val fakeLoad: suspend (String) -> DiscographyUiState = {
            DiscographyUiState.Success(
                mapOf(
                    DiscographyCategory.STUDIO_ALBUM to listOf(
                        AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false)
                    )
                )
            )
        }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "some-artist-id",
                artistName = "Genesis",
                load = fakeLoad,
                onToggleOwned = { _, _ -> },
                loadCover = { id ->
                    asked.add(id)
                    Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_image", useUnmergedTree = true).assertExists()
        assertEquals(listOf("1"), asked)
    }

    @Test
    fun `looks up the formats of albums that have none and shows them`() {
        val asked = mutableListOf<String>()
        val fakeLoad: suspend (String) -> DiscographyUiState = {
            DiscographyUiState.Success(
                mapOf(
                    DiscographyCategory.STUDIO_ALBUM to listOf(
                        AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false)
                    )
                )
            )
        }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "some-artist-id",
                artistName = "Genesis",
                load = fakeLoad,
                onToggleOwned = { _, _ -> },
                loadFormats = { id -> asked.add(id); listOf("LP", "CD") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true)
            .assertTextEquals("LP · CD")
        assertEquals(listOf("1"), asked)
    }

    @Test
    fun `shows the error message when loading fails`() {
        val fakeLoad: suspend (String) -> DiscographyUiState = {
            DiscographyUiState.Error("Something went wrong")
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "some-artist-id", artistName = "Genesis", load = fakeLoad, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }

    private val emptySuccess: suspend (String) -> DiscographyUiState = {
        DiscographyUiState.Success(emptyMap())
    }

    @Test
    fun `shows the artist name in the header`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
    }

    @Test
    fun `shows Saved when the artist is already a favourite`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `tapping Save saves the artist and then shows Saved`() {
        var saved = false
        var savedWith: Pair<String, String>? = null

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { saved },
                onSaveFavourite = { id, name ->
                    saved = true
                    savedWith = id to name
                }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()
        composeTestRule.waitForIdle()

        assertEquals("id-1" to "Roxy Music", savedWith)
        composeTestRule.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `tapping Saved removes the favourite and then shows Save`() {
        var saved = true

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { saved },
                onRemoveFavourite = { saved = false }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save").assertExists()
    }
}
