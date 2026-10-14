package com.nickallport.findmevinyl.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the logo and the three buttons`() {
        composeTestRule.setContent {
            FindMeVinylTheme { HomeScreen(onArtists = {}, onWants = {}, onShops = {}) }
        }

        composeTestRule.onNodeWithTag("logo_large").assertExists()
        composeTestRule.onNodeWithText("ARTISTS").assertExists()
        composeTestRule.onNodeWithText("WANTS").assertExists()
        composeTestRule.onNodeWithText("SHOPS").assertExists()
    }

    @Test
    fun `each button goes to its page`() {
        val went = mutableListOf<String>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                HomeScreen(onArtists = { went.add("artists") }, onWants = { went.add("wants") }, onShops = { went.add("shops") })
            }
        }

        composeTestRule.onNodeWithTag("home_artists").performClick()
        composeTestRule.onNodeWithTag("home_wants").performClick()
        composeTestRule.onNodeWithTag("home_shops").performClick()

        assertEquals(listOf("artists", "wants", "shops"), went)
    }

    @Test
    fun `works in dark mode too`() {
        composeTestRule.setContent {
            FindMeVinylTheme(darkTheme = true) { HomeScreen(onArtists = {}, onWants = {}, onShops = {}) }
        }

        composeTestRule.onNodeWithText("ARTISTS").assertExists()
    }
}
