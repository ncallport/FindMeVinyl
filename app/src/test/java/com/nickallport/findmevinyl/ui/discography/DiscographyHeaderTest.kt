package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
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
}