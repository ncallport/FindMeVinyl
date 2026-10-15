package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeTestRule.onNodeWithText("Lexicon of Love").assertExists()
        composeTestRule.onNodeWithText("Avalon").assertExists()
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
    fun `tapping Share passes the export text to the share callback`() {
        var sharedText: String? = null
        val text = "My vinyl want list:\n\nABC\n- Lexicon of Love"

        composeTestRule.setContent {
            WantListRoute(
                load = { sample },
                exportText = { text },
                onShareText = { sharedText = it }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(text, sharedText)
    }

    @Test
    fun `removing an album tells the caller and shows the list again without it`() {
        var current = twoAlbums
        val removed = mutableListOf<String>()

        composeTestRule.setContent {
            WantListRoute(
                load = { current },
                exportText = { "" },
                onShareText = {},
                onRemove = { id -> removed.add(id); current = current.filter { it.albumId != id } }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("want_remove_2").performClick()
        composeTestRule.onNodeWithTag("want_remove_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("2"), removed)
        composeTestRule.onNodeWithText("Siren").assertDoesNotExist()
        composeTestRule.onNodeWithText("Avalon").assertExists()
    }

    @Test
    fun `removing the last album shows the empty message`() {
        var current = listOf(WantedAlbumUiModel("1", "Roxy Music", "Avalon"))

        composeTestRule.setContent {
            WantListRoute(
                load = { current },
                exportText = { "" },
                onShareText = {},
                onRemove = { current = emptyList() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("want_remove_1").performClick()
        composeTestRule.onNodeWithTag("want_remove_confirm").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Your want list is empty").assertExists()
    }
}
