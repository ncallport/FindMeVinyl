package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistSearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `typing a query and tapping search invokes the callback with that text`() {
        var searchedFor: String? = null

        composeTestRule.setContent {
            ArtistSearchScreen(onSearch = { searchedFor = it })
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Genesis")
        composeTestRule.onNodeWithTag("search_button").performClick()

        assertEquals("Genesis", searchedFor)
    }
}
