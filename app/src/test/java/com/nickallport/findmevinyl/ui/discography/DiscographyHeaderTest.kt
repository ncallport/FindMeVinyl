package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertEquals
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscographyHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the artist name`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
    }

    @Test
    fun `offers Save when the artist is not a favourite`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `shows Saved when the artist is a favourite`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = true, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `tapping the button invokes the callback`() {
        var toggled = false

        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = { toggled = true })
        }

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()

        assertTrue(toggled)
    }

    @Test
    fun `has an Artists button that goes to the favourite artists`() {
        var opened = false
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                onOpenArtists = { opened = true }
            )
        }

        composeTestRule.onNodeWithTag("artists_button").assertTextEquals("Artists")
        composeTestRule.onNodeWithTag("artists_button").performClick()

        assertEquals(true, opened)
    }

    @Test
    fun `has no Artists button when it is not given a place to go`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("artists_button").assertDoesNotExist()
    }

    @Test
    fun `the buttons are on a line below the artist's name, side by side`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {}, onOpenArtists = {}
            )
        }

        fun top(node: androidx.compose.ui.test.SemanticsNodeInteraction) =
            node.fetchSemanticsNode().positionInRoot.y
        val name = top(composeTestRule.onNodeWithText("Roxy Music"))
        val artists = top(composeTestRule.onNodeWithTag("artists_button"))
        val save = top(composeTestRule.onNodeWithTag("favourite_toggle"))

        assertTrue("the buttons are below the name", artists > name && save > name)
        assertEquals(artists, save, 1f)
    }

    @Test
    fun `tapping Artists does not save or unsave the artist`() {
        var toggled = false
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = { toggled = true }, onOpenArtists = {}
            )
        }

        composeTestRule.onNodeWithTag("artists_button").performClick()

        assertEquals(false, toggled)
    }

    // The photo of the artist, which the user can add or change.

    private fun picture() = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)

    @Test
    fun `shows the artist's picture beside the name`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
        val image = composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).fetchSemanticsNode().positionInRoot
        val name = composeTestRule.onNodeWithText("Roxy Music").fetchSemanticsNode().positionInRoot
        assertTrue("the picture is left of the name", image.x < name.x)
    }

    @Test
    fun `shows the record placeholder when the artist has no picture`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { null }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `offers to change the photo when there is a picture, and to add one when there is not`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Change photo").assertExists()
    }

    @Test
    fun `offers to add a photo when there is no picture`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { null }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add photo").assertExists()
        composeTestRule.onNodeWithText("Change photo").assertDoesNotExist()
    }

    @Test
    fun `tapping the photo or its caption asks to choose one`() {
        var chosen = 0
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = { chosen++ }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("change_photo").performClick()

        assertEquals(1, chosen)
    }

    @Test
    fun `has no photo or caption when it is not given a way to choose one`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("change_photo").assertDoesNotExist()
        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `offers to use the original photo only when the user has chosen one`() {
        var reset = 0
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = {},
                hasUserPhoto = true, onUseOriginalPhoto = { reset++ }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("use_original_photo").performClick()

        assertEquals(1, reset)
    }

    @Test
    fun `does not offer the original photo when the user has not chosen one`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = {},
                hasUserPhoto = false, onUseOriginalPhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("use_original_photo").assertDoesNotExist()
    }

    @Test
    fun `loads the picture again when the photo changes`() {
        var version by androidx.compose.runtime.mutableStateOf(0)
        val asked = mutableListOf<String>()
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", photoVersion = version,
                loadArtistImage = { id -> asked.add(id); if (version == 0) null else picture() },
                onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertExists()

        version = 1
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
        assertEquals(listOf("a", "a"), asked)
    }

    @Test
    fun `tapping the photo does not save the artist or go to the favourites`() {
        var toggled = false
        var opened = false
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = { toggled = true },
                onOpenArtists = { opened = true },
                artistId = "a", loadArtistImage = { null }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("change_photo").performClick()

        assertEquals(false, toggled)
        assertEquals(false, opened)
    }
}
