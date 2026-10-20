package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The first twenty wants are highlighted. A tall window, so every block is composed; no dialog is opened here. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class WantListTiersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun many(count: Int) = (1..count).map { WantedAlbumUiModel("$it", "Artist $it", "Album $it") }

    private fun y(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `the first ten wants are under Top ten wanted, in the wanted colour`() {
        composeTestRule.setContent { WantListScreen(wanted = many(25), onShare = {}) }

        composeTestRule.onNodeWithTag("wants_top_heading").assertTextEquals("Top ten wanted")
        assertTrue(y("wants_top_heading") < y("want_1"))
        (1..10).forEach { composeTestRule.onNodeWithTag("want_$it").assertTier("Top ten") }
    }

    @Test
    fun `the next ten are under Next ten wanted, faded`() {
        composeTestRule.setContent { WantListScreen(wanted = many(25), onShare = {}) }

        composeTestRule.onNodeWithTag("wants_next_heading").assertTextEquals("Next ten wanted")
        assertTrue(y("want_10") < y("wants_next_heading"))
        assertTrue(y("wants_next_heading") < y("want_11"))
        (11..20).forEach { composeTestRule.onNodeWithTag("want_$it").assertTier("Next ten") }
    }

    @Test
    fun `the wants after the twentieth have no heading and no colour`() {
        composeTestRule.setContent { WantListScreen(wanted = many(25), onShare = {}) }

        (21..25).forEach { composeTestRule.onNodeWithTag("want_$it").assertTier(null) }
        composeTestRule.onAllNodesWithTag("wants_top_heading").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("wants_next_heading").assertCountEquals(1)
    }

    @Test
    fun `a short list has only the top ten heading`() {
        composeTestRule.setContent { WantListScreen(wanted = many(10), onShare = {}) }

        composeTestRule.onNodeWithTag("wants_top_heading").assertExists()
        composeTestRule.onNodeWithTag("wants_next_heading").assertDoesNotExist()
    }

    @Test
    fun `the eleventh want brings in the next ten heading`() {
        composeTestRule.setContent { WantListScreen(wanted = many(11), onShare = {}) }

        composeTestRule.onNodeWithTag("wants_next_heading").assertExists()
    }

    @Test
    fun `an empty list has no headings`() {
        composeTestRule.setContent { WantListScreen(wanted = emptyList(), onShare = {}) }

        composeTestRule.onNodeWithTag("wants_top_heading").assertDoesNotExist()
        composeTestRule.onNodeWithTag("wants_next_heading").assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.assertTier(tier: String?) {
        val actual = fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals(tier, actual)
    }
}
