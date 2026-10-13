package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertTextContains
import org.junit.Assert.assertEquals

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

        Assert.assertEquals("Genesis", searchedFor)
    }

    @Test
    fun `tapping Favourite Artists invokes the callback`() {
        var opened = false

        composeTestRule.setContent {
            ArtistSearchScreen(onSearch = {}, onOpenFavourites = { opened = true })
        }

        composeTestRule.onNodeWithTag("favourites_link").performClick()

        assertTrue(opened)
    }

    @Test
    fun `tapping Want List invokes the callback`() {
        var opened = false

        composeTestRule.setContent {
            ArtistSearchScreen(onSearch = {}, onOpenWantList = { opened = true })
        }

        composeTestRule.onNodeWithTag("want_list_link").performClick()

        assertTrue(opened)
    }

    @Test
    fun `tapping Find Shops invokes the callback`() {
        var opened = false

        composeTestRule.setContent {
            ArtistSearchScreen(onSearch = {}, onOpenShops = { opened = true })
        }

        composeTestRule.onNodeWithTag("find_shops_link").performClick()

        assertTrue(opened)
    }

    @Test
    fun `starts with the query it was given`() {
        composeTestRule.setContent {
            ArtistSearchScreen(onSearch = {}, initialQuery = "Genesis")
        }

        composeTestRule.onNodeWithTag("search_field").assertTextContains("Genesis")
    }

    @Test
    fun `reports each change to the query`() {
        var latest: String? = null

        composeTestRule.setContent {
            ArtistSearchScreen(onSearch = {}, onQueryChange = { latest = it })
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Genesis")

        assertEquals("Genesis", latest)
    }
}