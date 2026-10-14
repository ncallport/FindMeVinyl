package com.nickallport.findmevinyl.ui.brand

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the big logo is announced as Find Me Vinyl`() {
        composeTestRule.setContent { FindMeVinylTheme { FindMeVinylLogo() } }

        composeTestRule.onNodeWithTag("logo_large").assertContentDescriptionEquals("Find Me Vinyl")
    }

    @Test
    fun `the big logo draws in both light and dark themes`() {
        composeTestRule.setContent { FindMeVinylTheme(darkTheme = true) { FindMeVinylLogo() } }
        composeTestRule.onNodeWithTag("logo_large").assertExists()
    }

    @Test
    fun `the masthead shows the name`() {
        composeTestRule.setContent { FindMeVinylTheme { Masthead(onHome = {}) } }

        composeTestRule.onNodeWithTag("masthead").assertContentDescriptionEquals("Find Me Vinyl, go to the front screen")
    }

    @Test
    fun `tapping the masthead goes home`() {
        var taps = 0
        composeTestRule.setContent { FindMeVinylTheme { Masthead(onHome = { taps++ }) } }

        composeTestRule.onNodeWithTag("masthead").assertHasClickAction().performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `a masthead with nowhere to go home to is not tappable`() {
        composeTestRule.setContent { FindMeVinylTheme { Masthead(onHome = null) } }

        composeTestRule.onNodeWithTag("masthead").assertContentDescriptionEquals("Find Me Vinyl")
    }
}
