package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.discography.Grouping
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.discography.FormatOption
import org.junit.Assert.assertEquals
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormatFilterBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val options = listOf(FormatOption("LP", 12), FormatOption("CD", 3))

    @Test
    fun `shows All and a chip for each format with its count`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = null, unchecked = 0, onSelect = {})
        }

        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("LP 12").assertExists()
        composeTestRule.onNodeWithText("CD 3").assertExists()
    }

    @Test
    fun `shows nothing while no formats are known and none is chosen`() {
        composeTestRule.setContent {
            FormatFilterBar(options = emptyList(), selected = null, unchecked = 40, onSelect = {})
        }

        composeTestRule.onNodeWithText("All").assertDoesNotExist()
        composeTestRule.onNodeWithTag("unchecked_note").assertDoesNotExist()
    }

    @Test
    fun `All is selected when no format is chosen`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = null, unchecked = 0, onSelect = {})
        }

        composeTestRule.onNodeWithTag("format_chip_all").assertIsSelected()
        composeTestRule.onNodeWithTag("format_chip_LP").assertIsNotSelected()
    }

    @Test
    fun `the chosen format is selected and All is not`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = "CD", unchecked = 0, onSelect = {})
        }

        composeTestRule.onNodeWithTag("format_chip_CD").assertIsSelected()
        composeTestRule.onNodeWithTag("format_chip_LP").assertIsNotSelected()
        composeTestRule.onNodeWithTag("format_chip_all").assertIsNotSelected()
    }

    @Test
    fun `tapping a chip chooses that format`() {
        var chosen: String? = "none yet"

        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = null, unchecked = 0, onSelect = { chosen = it })
        }
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()

        assertEquals("LP", chosen)
    }

    @Test
    fun `tapping the chosen chip clears the choice`() {
        var chosen: String? = "none yet"

        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = "LP", unchecked = 0, onSelect = { chosen = it })
        }
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()

        assertEquals(null, chosen)
    }

    @Test
    fun `tapping All clears the choice`() {
        var chosen: String? = "none yet"

        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = "LP", unchecked = 0, onSelect = { chosen = it })
        }
        composeTestRule.onNodeWithTag("format_chip_all").performClick()

        assertEquals(null, chosen)
    }

    @Test
    fun `says how many albums are not checked yet while a format is chosen`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = "LP", unchecked = 3, onSelect = {})
        }

        composeTestRule.onNodeWithTag("unchecked_note").assertExists()
        composeTestRule.onNodeWithText("3 not checked yet").assertExists()
    }

    @Test
    fun `says nothing about unchecked albums when no format is chosen or none are left`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = null, unchecked = 3, onSelect = {})
        }
        composeTestRule.onNodeWithTag("unchecked_note").assertDoesNotExist()
    }

    @Test
    fun `says nothing about unchecked albums when none are left`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = "LP", unchecked = 0, onSelect = {})
        }
        composeTestRule.onNodeWithTag("unchecked_note").assertDoesNotExist()
    }

    @Test
    fun `keeps a chip for the chosen format even when no album has it, so it can be cleared`() {
        composeTestRule.setContent {
            FormatFilterBar(options = emptyList(), selected = "MC", unchecked = 0, onSelect = {})
        }

        composeTestRule.onNodeWithTag("format_chip_MC").assertIsSelected()
        composeTestRule.onNodeWithText("MC 0").assertExists()
        composeTestRule.onNodeWithTag("format_chip_all").assertExists()
    }

    // The Group owned chip, which leads the row when the bar is given it.

    @Test
    fun `has no Group owned chip unless it is given one`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = null, unchecked = 0, onSelect = {})
        }

        composeTestRule.onNodeWithTag("group_owned_toggle").assertDoesNotExist()
    }

    @Test
    fun `shows the Group owned chip even while no formats are known`() {
        composeTestRule.setContent {
            FormatFilterBar(
                options = emptyList(), selected = null, unchecked = 40, onSelect = {},
                grouping = Grouping.None
            )
        }

        composeTestRule.onNodeWithTag("group_owned_toggle").assertExists()
        composeTestRule.onNodeWithText("All").assertDoesNotExist()
    }

    @Test
    fun `the Group owned chip is selected when group owned is on`() {
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = {},
                grouping = Grouping(owned = true)
            )
        }

        composeTestRule.onNodeWithTag("group_owned_toggle").assertIsSelected()
    }

    @Test
    fun `the Group owned chip is not selected when group owned is off`() {
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = {},
                grouping = Grouping.None
            )
        }

        composeTestRule.onNodeWithTag("group_owned_toggle").assertIsNotSelected()
    }

    @Test
    fun `tapping the Group owned chip asks to switch it, and does not choose a format`() {
        var toggled = 0
        val chosen = mutableListOf<String?>()
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = { chosen.add(it) },
                grouping = Grouping.None, onToggleGroupOwned = { toggled++ }
            )
        }

        composeTestRule.onNodeWithTag("group_owned_toggle").performClick()

        assertEquals(1, toggled)
        assertEquals(emptyList<String?>(), chosen)
    }

    @Test
    fun `the Group owned chip comes before All, on the same line`() {
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = {},
                grouping = Grouping.None
            )
        }

        val owned = composeTestRule.onNodeWithTag("group_owned_toggle").fetchSemanticsNode().positionInRoot
        val all = composeTestRule.onNodeWithTag("format_chip_all").fetchSemanticsNode().positionInRoot

        assertTrue("Group owned is left of All", owned.x < all.x)
        assertEquals(owned.y, all.y, 1f)
    }

    @Test
    fun `choosing a format works as before with the Group owned chip there`() {
        val chosen = mutableListOf<String?>()
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = { chosen.add(it) },
                grouping = Grouping(owned = true)
            )
        }

        composeTestRule.onNodeWithTag("format_chip_LP").performClick()

        assertEquals(listOf<String?>("LP"), chosen)
    }

    // The Group wanted chip, next to Group owned.

    @Test
    fun `has a Group wanted chip after Group owned and before All, and says so`() {
        composeTestRule.setContent {
            FormatFilterBar(options = options, selected = null, unchecked = 0, onSelect = {}, grouping = Grouping.None)
        }

        composeTestRule.onNodeWithText("Group wanted").assertExists()
        val owned = composeTestRule.onNodeWithTag("group_owned_toggle").fetchSemanticsNode().positionInRoot
        val wanted = composeTestRule.onNodeWithTag("group_wanted_toggle").fetchSemanticsNode().positionInRoot
        val all = composeTestRule.onNodeWithTag("format_chip_all").fetchSemanticsNode().positionInRoot
        assertTrue(owned.x < wanted.x && wanted.x < all.x)
    }

    @Test
    fun `the Group wanted chip is selected only when wanted is grouped`() {
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = {},
                grouping = Grouping(wanted = true)
            )
        }

        composeTestRule.onNodeWithTag("group_wanted_toggle").assertIsSelected()
        composeTestRule.onNodeWithTag("group_owned_toggle").assertIsNotSelected()
    }

    @Test
    fun `tapping the Group wanted chip asks to switch it and nothing else`() {
        var owned = 0
        var wanted = 0
        composeTestRule.setContent {
            FormatFilterBar(
                options = options, selected = null, unchecked = 0, onSelect = {},
                grouping = Grouping.None, onToggleGroupOwned = { owned++ }, onToggleGroupWanted = { wanted++ }
            )
        }

        composeTestRule.onNodeWithTag("group_wanted_toggle").performClick()

        assertEquals(0, owned)
        assertEquals(1, wanted)
    }
}
