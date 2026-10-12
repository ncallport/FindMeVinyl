package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WantListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = listOf(
        WantedAlbumUiModel("1", "ABC", "Lexicon of Love"),
        WantedAlbumUiModel("2", "Roxy Music", "Avalon"),
        WantedAlbumUiModel("3", "Roxy Music", "Siren")
    )

    @Test
    fun `shows each artist and their wanted albums`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithText("ABC").assertExists()
        composeTestRule.onNodeWithText("Lexicon of Love").assertExists()
        composeTestRule.onNodeWithText("Avalon").assertExists()
        composeTestRule.onNodeWithText("Siren").assertExists()
    }

    @Test
    fun `shows an artist's name once even when they have several albums`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onAllNodesWithText("Roxy Music").assertCountEquals(1)
    }

    @Test
    fun `shows a message and no share button when nothing is wanted`() {
        composeTestRule.setContent {
            WantListScreen(wanted = emptyList(), onShare = {})
        }

        composeTestRule.onNodeWithText("Your want list is empty").assertExists()
        composeTestRule.onNodeWithTag("share_button").assertDoesNotExist()
    }

    @Test
    fun `tapping Share invokes the callback`() {
        var shared = false

        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = { shared = true })
        }

        composeTestRule.onNodeWithTag("share_button").performClick()

        assertTrue(shared)
    }
}