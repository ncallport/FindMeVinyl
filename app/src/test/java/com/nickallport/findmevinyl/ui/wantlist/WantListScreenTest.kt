package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.export.ShareOptions
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
    fun `each want is a block with the artist and the album, whoever it is by`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_title_1").assertTextEquals("ABC - Lexicon of Love")
        composeTestRule.onNodeWithTag("want_title_2").assertTextEquals("Roxy Music - Avalon")
        composeTestRule.onNodeWithTag("want_title_3").assertTextEquals("Roxy Music - Siren")
    }

    @Test
    fun `an artist's name is on every block, as the list is not grouped by artist`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onAllNodesWithText("Roxy Music - ", substring = true).assertCountEquals(2)
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
    fun `tapping Share asks what to share before sharing anything`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = { shared = it })
        }

        composeTestRule.onNodeWithTag("share_button").performClick()

        composeTestRule.onNodeWithTag("share_dialog").assertExists()
        assertEquals(null, shared)
    }

    @Test
    fun `confirming with nothing changed shares the whole list with no message`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = { shared = it })
        }
        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_confirm").performClick()

        assertEquals(ShareOptions(intro = "", limit = null), shared)
        composeTestRule.onNodeWithTag("share_dialog").assertDoesNotExist()
    }

    @Test
    fun `cancelling shares nothing and closes the options`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = { shared = it })
        }
        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_cancel").performClick()

        assertEquals(null, shared)
        composeTestRule.onNodeWithTag("share_dialog").assertDoesNotExist()
    }

    @Test
    fun `a message typed in the options is passed on`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = { shared = it })
        }
        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_intro").performTextInput("It is my birthday soon")
        composeTestRule.onNodeWithTag("share_confirm").performClick()

        assertEquals("It is my birthday soon", shared?.intro)
    }

    private val twentyFive = (1..25).map { WantedAlbumUiModel("$it", "Artist ${(it + 4) / 5}", "Album $it") }

    @Test
    fun `offers the top 10 and top 20 of a list of 25, and all of it`() {
        composeTestRule.setContent { WantListScreen(wanted = twentyFive, onShare = {}) }
        composeTestRule.onNodeWithTag("share_button").performClick()

        composeTestRule.onNodeWithTag("share_limit_10").assertExists()
        composeTestRule.onNodeWithTag("share_limit_20").assertExists()
        composeTestRule.onNodeWithTag("share_limit_all").assertExists()
        composeTestRule.onNodeWithTag("share_limit_30").assertDoesNotExist()
    }

    @Test
    fun `a list of 8 has nothing to choose between, so it shows no choices and says it shares all of it`() {
        composeTestRule.setContent { WantListScreen(wanted = twentyFive.take(8), onShare = {}) }
        composeTestRule.onNodeWithTag("share_button").performClick()

        composeTestRule.onNodeWithTag("share_limit_10").assertDoesNotExist()
        composeTestRule.onNodeWithTag("share_limit_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("share_summary").assertTextEquals("Sharing all 8 albums")
    }

    @Test
    fun `choosing top 10 passes the limit on, and the summary says how many`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent { WantListScreen(wanted = twentyFive, onShare = { shared = it }) }
        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_summary").assertTextEquals("Sharing all 25 albums")

        composeTestRule.onNodeWithTag("share_limit_10").performClick()
        composeTestRule.onNodeWithTag("share_summary").assertTextEquals("Sharing the top 10 of 25 albums")
        composeTestRule.onNodeWithTag("share_confirm").performClick()

        assertEquals(10, shared?.limit)
    }

    @Test
    fun `the choice can be changed back to all`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent { WantListScreen(wanted = twentyFive, onShare = { shared = it }) }
        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_limit_20").performClick()
        composeTestRule.onNodeWithTag("share_limit_all").performClick()
        composeTestRule.onNodeWithTag("share_confirm").performClick()

        assertEquals(null, shared?.limit)
    }

    @Test
    fun `one album is summed up in the singular`() {
        composeTestRule.setContent { WantListScreen(wanted = sample.take(1), onShare = {}) }
        composeTestRule.onNodeWithTag("share_button").performClick()

        composeTestRule.onNodeWithTag("share_summary").assertTextEquals("Sharing 1 album")
    }

    @Test
    fun `the last choices are still there the next time the options are opened`() {
        var shared: ShareOptions? = null

        composeTestRule.setContent { WantListScreen(wanted = twentyFive, onShare = { shared = it }) }
        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_intro").performTextInput("Birthday ideas")
        composeTestRule.onNodeWithTag("share_limit_10").performClick()
        composeTestRule.onNodeWithTag("share_cancel").performClick()

        composeTestRule.onNodeWithTag("share_button").performClick()
        composeTestRule.onNodeWithTag("share_confirm").performClick()

        assertEquals(ShareOptions("Birthday ideas", 10), shared)
    }

    @Test
    fun `every block has buttons to move it`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_down_1").assertIsEnabled()
        composeTestRule.onNodeWithTag("want_up_2").assertIsEnabled()
        composeTestRule.onNodeWithTag("want_up_3").assertIsEnabled()
    }

    @Test
    fun `the first want cannot move up and the last cannot move down`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_up_1").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("want_down_3").assertIsNotEnabled()
    }

    @Test
    fun `one want on its own has nothing to move past, so no move buttons`() {
        composeTestRule.setContent {
            WantListScreen(wanted = listOf(sample[0]), onShare = {})
        }

        composeTestRule.onNodeWithTag("want_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("want_down_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("want_remove_1").assertExists()
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
    fun `shows the wants in the order given, even when the artists are mixed`() {
        val ordered = listOf(
            WantedAlbumUiModel("2", "Roxy Music", "Siren"),
            WantedAlbumUiModel("1", "ABC", "Lexicon of Love"),
            WantedAlbumUiModel("3", "Roxy Music", "Avalon")
        )
        composeTestRule.setContent {
            WantListScreen(wanted = ordered, onShare = {})
        }

        val tops = listOf("2", "1", "3").map {
            composeTestRule.onNodeWithTag("want_title_$it").fetchSemanticsNode().positionInRoot.y
        }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `every want has a button to remove it`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithTag("want_remove_1").assertExists()
        composeTestRule.onNodeWithTag("want_remove_2").assertExists()
        composeTestRule.onNodeWithTag("want_remove_3").assertExists()
    }

    @Test
    fun `the remove buttons say which album they remove`() {
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {})
        }

        composeTestRule.onNodeWithContentDescription("Remove Siren from wants").assertExists()
    }

    @Test
    fun `tapping remove asks first, and does not remove yet`() {
        val removed = mutableListOf<String>()
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {}, onRemove = { removed.add(it) })
        }

        composeTestRule.onNodeWithTag("want_remove_3").performClick()

        composeTestRule.onNodeWithTag("want_remove_dialog").assertExists()
        composeTestRule.onNodeWithText("Remove Siren from your wants?").assertExists()
        assertEquals(emptyList<String>(), removed)
    }

    @Test
    fun `confirming removes the album and closes the question`() {
        val removed = mutableListOf<String>()
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {}, onRemove = { removed.add(it) })
        }

        composeTestRule.onNodeWithTag("want_remove_3").performClick()
        composeTestRule.onNodeWithTag("want_remove_confirm").performClick()

        assertEquals(listOf("3"), removed)
        composeTestRule.onNodeWithTag("want_remove_dialog").assertDoesNotExist()
    }

    @Test
    fun `keeping the album closes the question and removes nothing`() {
        val removed = mutableListOf<String>()
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {}, onRemove = { removed.add(it) })
        }

        composeTestRule.onNodeWithTag("want_remove_3").performClick()
        composeTestRule.onNodeWithTag("want_remove_cancel").performClick()

        assertEquals(emptyList<String>(), removed)
        composeTestRule.onNodeWithTag("want_remove_dialog").assertDoesNotExist()
    }

    @Test
    fun `the remove button does not move the album`() {
        val moved = mutableListOf<String>()
        composeTestRule.setContent {
            WantListScreen(wanted = sample, onShare = {}, onMoveUp = { moved.add(it) }, onMoveDown = { moved.add(it) })
        }

        composeTestRule.onNodeWithTag("want_remove_3").performClick()

        assertEquals(emptyList<String>(), moved)
    }

    @Test
    fun `an album wanted on a format shows it under the title`() {
        composeTestRule.setContent {
            WantListScreen(wanted = listOf(WantedAlbumUiModel("1", "Bob Dylan", "Desire", wantFormat = "LP")), onShare = {})
        }

        composeTestRule.onNodeWithTag("want_format_1", useUnmergedTree = true).assertTextEquals("on LP")
        composeTestRule.onNodeWithText("Desire", substring = true).assertExists()
    }
}
