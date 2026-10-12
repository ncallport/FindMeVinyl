package com.nickallport.findmevinyl.ui.wantlist

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
}