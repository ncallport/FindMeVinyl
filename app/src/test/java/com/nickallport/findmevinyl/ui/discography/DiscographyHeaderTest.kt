package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertEquals
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
}
