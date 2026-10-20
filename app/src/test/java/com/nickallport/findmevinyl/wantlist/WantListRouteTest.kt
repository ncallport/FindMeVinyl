package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.export.ShareOptions
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WantListRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = listOf(
        WantedAlbumUiModel("1", "ABC", "Lexicon of Love"),
        WantedAlbumUiModel("2", "Roxy Music", "Avalon")
    )

    @Test
    fun `shows the loaded want list`() {
        composeTestRule.setContent {
            WantListRoute(load = { sample }, exportText = { "" }, onShareText = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Lexicon of Love", substring = true).assertExists()
        composeTestRule.onNodeWithText("Avalon", substring = true).assertExists()
    }

    @Test
    fun `shows the empty message when nothing is wanted`() {
        composeTestRule.setContent {
            WantListRoute(load = { emptyList() }, exportText = { "" }, onShareText = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Your want list is empty").assertExists()
    }

    private val twoAlbums = listOf(
        WantedAlbumUiModel("1", "Roxy Music", "Avalon"),
        WantedAlbumUiModel("2", "Roxy Music", "Siren")
    )

    @Test
    fun `moving an album up tells the caller and shows the list again from the load`() {
        var order = twoAlbums
        val moved = mutableListOf<String>()

        composeTestRule.setContent {
            WantListRoute(
                load = { order },
                exportText = { "" },
                onShareText = {},
                onMoveUp = { id -> moved.add(id); order = order.reversed() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("want_up_2").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("2"), moved)
        // Siren is now first, so it cannot move up any more.
        composeTestRule.onNodeWithTag("want_up_2").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("want_up_1").assertIsEnabled()
    }

    @Test
    fun `moving an album down tells the caller`() {
        var order = twoAlbums
        val moved = mutableListOf<String>()

        composeTestRule.setContent {
            WantListRoute(
                load = { order },
                exportText = { "" },
                onShareText = {},
                onMoveDown = { id -> moved.add(id); order = order.reversed() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("want_down_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1"), moved)
        composeTestRule.onNodeWithTag("want_down_1").assertIsNotEnabled()
    }

    @Test
    fun `choosing what to share, then confirming, passes the export text to the share callback`() {
        var sharedText: String? = null
        var asked: ShareOptions? = null
        val text = "My vinyl want list:\n\nABC\n- Lexicon of Love"

        composeTestRule.setContent {
            WantListRoute(
                load = { sample },
                exportText = { asked = it; text },
                onShareText = { sharedText = it }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_intro").performTextInput("Hello")
        composeTestRule.onNodeWithTag("share_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(ShareOptions("Hello", null), asked)
        assertEquals(text, sharedText)
    }

    @Test
    fun `cancelling the options shares nothing`() {
        var sharedText: String? = null

        composeTestRule.setContent {
            WantListRoute(load = { sample }, exportText = { "text" }, onShareText = { sharedText = it })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_cancel").performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, sharedText)
    }

    @Test
    fun `sending a want to the top tells the caller and shows the list again from the load`() {
        var order = twoAlbums
        val sent = mutableListOf<String>()
        composeTestRule.setContent {
            WantListRoute(
                load = { order }, exportText = { "" }, onShareText = {},
                onMoveToTop = { id -> sent.add(id); order = order.reversed() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("want_top_2").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("2"), sent)
        composeTestRule.onNodeWithTag("want_top_2").assertIsNotEnabled()
    }
}
