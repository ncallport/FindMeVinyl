package com.nickallport.findmevinyl.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the hint while empty`() {
        composeTestRule.setContent {
            FindMeVinylTheme { SearchField(value = "", onValueChange = {}, onSearch = {}, hint = "Search for an artist") }
        }

        composeTestRule.onNodeWithText("Search for an artist").assertExists()
    }

    @Test
    fun `reports what is typed`() {
        var text by mutableStateOf("")
        composeTestRule.setContent {
            FindMeVinylTheme { SearchField(value = text, onValueChange = { text = it }, onSearch = {}, hint = "h") }
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Genesis")

        assertEquals("Genesis", text)
        composeTestRule.onNodeWithTag("search_field").assertTextContains("Genesis")
    }

    @Test
    fun `the keyboard search key searches for the trimmed text`() {
        var searched: String? = null
        composeTestRule.setContent {
            FindMeVinylTheme { SearchField(value = "  Roxy Music  ", onValueChange = {}, onSearch = { searched = it }, hint = "h") }
        }

        composeTestRule.onNodeWithTag("search_field").performImeAction()

        assertEquals("Roxy Music", searched)
    }

    @Test
    fun `the keyboard search key does nothing when the text is blank`() {
        var searches = 0
        composeTestRule.setContent {
            FindMeVinylTheme { SearchField(value = "   ", onValueChange = {}, onSearch = { searches++ }, hint = "h") }
        }

        composeTestRule.onNodeWithTag("search_field").performImeAction()

        assertEquals(0, searches)
    }

    @Test
    fun `has no separate search button`() {
        composeTestRule.setContent {
            FindMeVinylTheme { SearchField(value = "x", onValueChange = {}, onSearch = {}, hint = "h") }
        }

        composeTestRule.onNodeWithTag("search_button").assertDoesNotExist()
    }
}
