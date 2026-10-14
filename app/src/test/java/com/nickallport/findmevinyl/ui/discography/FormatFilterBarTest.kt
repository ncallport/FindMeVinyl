package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.discography.FormatOption
import org.junit.Assert.assertEquals
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
}
