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
import org.robolectric.annotation.Config

// A phone-sized window: the logo, three big buttons and Settings do not fit in the small default one.
@Config(qualifiers = "w360dp-h900dp")
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

    @Test
    fun `has a Settings button when it is given somewhere to go`() {
        var opened = 0
        composeTestRule.setContent {
            FindMeVinylTheme { HomeScreen(onArtists = {}, onWants = {}, onShops = {}, onSettings = { opened++ }) }
        }

        composeTestRule.onNodeWithTag("home_settings").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `has no Settings button when it is not given somewhere to go`() {
        composeTestRule.setContent {
            FindMeVinylTheme { HomeScreen(onArtists = {}, onWants = {}, onShops = {}) }
        }

        composeTestRule.onNodeWithTag("home_settings").assertDoesNotExist()
    }

    @Test
    fun `the Settings button is quieter than the three big ones and below them`() {
        composeTestRule.setContent {
            FindMeVinylTheme { HomeScreen(onArtists = {}, onWants = {}, onShops = {}, onSettings = {}) }
        }

        val shops = composeTestRule.onNodeWithTag("home_shops").fetchSemanticsNode().boundsInRoot
        val settings = composeTestRule.onNodeWithTag("home_settings").fetchSemanticsNode().boundsInRoot

        assertEquals(true, settings.top >= shops.bottom)
        assertEquals(true, settings.height < shops.height)
    }

    @Test
    fun `the Settings button does not take the place of the others`() {
        val went = mutableListOf<String>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                HomeScreen(onArtists = { went.add("artists") }, onWants = {}, onShops = {}, onSettings = { went.add("settings") })
            }
        }

        composeTestRule.onNodeWithTag("home_artists").performClick()

        assertEquals(listOf("artists"), went)
    }
}
