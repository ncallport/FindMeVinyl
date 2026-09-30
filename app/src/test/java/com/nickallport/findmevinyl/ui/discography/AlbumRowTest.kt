package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the album title`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `shows an Owned label when the album is owned`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = true, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithText("Owned").assertExists()
    }

    @Test
    fun `does not show an Owned label when the album is not owned`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithText("Owned").assertDoesNotExist()
    }

    @Test
    fun `tapping the row invokes the toggle callback`() {
        var wasClicked = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = { wasClicked = true }
            )
        }

        composeTestRule.onNodeWithTag("album_row").performClick()

        assertTrue(wasClicked)
    }

    @Test
    fun `shows a Want label when the album is wanted`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = true),
                onToggleOwned = {}
            )
        }
        composeTestRule.onNodeWithText("Want").assertExists()
    }

    @Test
    fun `does not show a Want label when the album is not wanted`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }
        composeTestRule.onNodeWithText("Want").assertDoesNotExist()
    }

    @Test
    fun `tapping the Want label invokes the wanted callback without triggering the owned callback`() {
        var ownedToggled = false
        var wantedToggled = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = { ownedToggled = true },
                onToggleWanted = { wantedToggled = true }
            )
        }

        composeTestRule.onNodeWithTag("wanted_toggle").performClick()

        assertTrue(wantedToggled)
        assertFalse(ownedToggled)
    }
}
