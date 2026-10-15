package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertEquals
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
    fun `tells you how to add albums when the list is empty`() {
        composeTestRule.setContent {
            WantListScreen(wanted = emptyList(), onShare = {})
        }

        composeTestRule.onNodeWithText("Tap + Want on an album to add it here").assertExists()
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

    @Test
    fun `an artist with several albums has buttons to move each album`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_down_2").assertIsEnabled()
        composeTestRule.onNodeWithTag("want_up_3").assertIsEnabled()
    }

    @Test
    fun `the first album of an artist cannot move up and the last cannot move down`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_up_2").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("want_down_3").assertIsNotEnabled()
    }

    @Test
    fun `an artist with one album has nothing to move past, so no move buttons`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("want_down_1").assertDoesNotExist()
    }

    @Test
    fun `the move buttons report the album`() {
        val up = mutableListOf<String>()
        val down = mutableListOf<String>()

        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {}, onMoveUp = { up.add(it) }, onMoveDown = { down.add(it) })
        }

        composeTestRule.onNodeWithTag("want_up_3").performClick()
        composeTestRule.onNodeWithTag("want_down_2").performClick()

        assertEquals(listOf("3"), up)
        assertEquals(listOf("2"), down)
    }

    @Test
    fun `the move buttons say which album they move`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithContentDescription("Move Siren up").assertExists()
        composeTestRule.onNodeWithContentDescription("Move Avalon down").assertExists()
    }

    @Test
    fun `shows the artists and albums in the order given`() {
        val ordered = listOf(
            WantedAlbumUiModel("2", "Roxy Music", "Siren"),
            WantedAlbumUiModel("3", "Roxy Music", "Avalon"),
            WantedAlbumUiModel("1", "ABC", "Lexicon of Love")
        )
        composeTestRule.setContent {
            WantListScreen(wanted = ordered, onShare = {})
        }

        val tops = listOf("Roxy Music", "Siren", "Avalon", "ABC").map {
            composeTestRule.onNodeWithText(it).fetchSemanticsNode().positionInRoot.y
        }
        assertEquals(tops.sorted(), tops)
    }
}
