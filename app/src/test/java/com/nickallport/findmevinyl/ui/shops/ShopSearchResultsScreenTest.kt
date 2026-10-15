package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithTag
import com.nickallport.findmevinyl.shops.PlaceCandidate

@RunWith(RobolectricTestRunner::class)
class ShopSearchResultsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val newport = ShopSearchUiState.Success(
        shops = listOf(Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null)),
        placeName = "Newport, Wales, United Kingdom",
        centerLatitude = 51.5842,
        centerLongitude = -2.9977,
        otherPlaces = listOf(
            PlaceCandidate("Newport, Isle of Wight, England, United Kingdom", 50.7, -1.2915),
            PlaceCandidate("Newport, Kentucky, United States", 39.1, -84.45)
        )
    )

    @Test
    fun `shows the shops that were found`() {
        val state = ShopSearchUiState.Success(
            shops = listOf(
                Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null),
                Shop("2", "Oxfam", ShopCategory.CHARITY_SHOP, 51.52, -0.14, null)
            ),
            placeName = "London, UK",
            centerLatitude = 51.5074,
            centerLongitude = -0.1278
        )

        composeTestRule.setContent { ShopSearchResultsScreen(state = state) }

        composeTestRule.onNodeWithText("Sister Ray").assertExists()
        composeTestRule.onNodeWithText("Oxfam").assertExists()
    }

    @Test
    fun `shows a message when the search found no shops`() {
        val state = ShopSearchUiState.Success(
            shops = emptyList(),
            placeName = "London, UK",
            centerLatitude = 51.5074,
            centerLongitude = -0.1278
        )

        composeTestRule.setContent { ShopSearchResultsScreen(state = state) }

        composeTestRule.onNodeWithText("No shops found nearby").assertExists()
    }

    @Test
    fun `names the town when it could not be found`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(state = ShopSearchUiState.TownNotFound("Atlantis"))
        }

        composeTestRule.onNodeWithText("Couldn't find Atlantis").assertExists()
    }

    @Test
    fun `shows the error message when the search failed`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(state = ShopSearchUiState.Error("Something went wrong"))
        }

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }

    @Test
    fun `shows the place that was searched above the shops`() {
        val state = ShopSearchUiState.Success(
            shops = listOf(
                Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null)
            ),
            placeName = "Cartaxo, Santarem, Portugal",
            centerLatitude = 39.1600,
            centerLongitude = -8.7900
        )

        composeTestRule.setContent { ShopSearchResultsScreen(state = state) }

        composeTestRule.onNodeWithText("Cartaxo, Santarem, Portugal").assertExists()
        composeTestRule.onNodeWithText("Sister Ray").assertExists()
    }

    @Test
    fun `shows the place name even when no shops were found`() {
        val state = ShopSearchUiState.Success(
            shops = emptyList(),
            placeName = "Cartaxo, Santarem, Portugal",
            centerLatitude = 39.1600,
            centerLongitude = -8.7900
        )

        composeTestRule.setContent { ShopSearchResultsScreen(state = state) }

        composeTestRule.onNodeWithText("Cartaxo, Santarem, Portugal").assertExists()
        composeTestRule.onNodeWithText("No shops found nearby").assertExists()
    }

    @Test
    fun `tapping a found shop invokes the callback with that shop`() {
        var selected: Shop? = null
        val state = ShopSearchUiState.Success(
            shops = listOf(
                Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null),
                Shop("2", "Oxfam", ShopCategory.CHARITY_SHOP, 51.52, -0.14, null)
            ),
            placeName = "London, UK",
            centerLatitude = 51.5074,
            centerLongitude = -0.1278
        )

        composeTestRule.setContent {
            ShopSearchResultsScreen(state = state, onShopSelected = { selected = it })
        }

        composeTestRule.onNodeWithText("Sister Ray").performClick()

        assertEquals("1", selected?.id)
    }

    @Test
    fun `offers a way to pick another place when there are alternatives`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(state = newport, onPlaceSelected = {})
        }

        composeTestRule.onNodeWithText("Not the right place?").assertExists()
        composeTestRule.onNodeWithText("Newport, Kentucky, United States").assertDoesNotExist()
    }

    @Test
    fun `tapping the link shows the alternatives`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(state = newport, onPlaceSelected = {})
        }

        composeTestRule.onNodeWithTag("other_places_toggle").performClick()

        composeTestRule.onNodeWithText("Newport, Isle of Wight, England, United Kingdom").assertExists()
        composeTestRule.onNodeWithText("Newport, Kentucky, United States").assertExists()
    }

    @Test
    fun `tapping an alternative invokes the callback with that place`() {
        var selected: PlaceCandidate? = null

        composeTestRule.setContent {
            ShopSearchResultsScreen(state = newport, onPlaceSelected = { selected = it })
        }

        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText("Newport, Kentucky, United States").performClick()

        assertEquals(PlaceCandidate("Newport, Kentucky, United States", 39.1, -84.45), selected)
    }

    @Test
    fun `does not offer the link when there are no alternatives`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(
                state = newport.copy(otherPlaces = emptyList()),
                onPlaceSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Not the right place?").assertDoesNotExist()
    }

    @Test
    fun `does not offer the link when no callback is given`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(state = newport)
        }

        composeTestRule.onNodeWithText("Not the right place?").assertDoesNotExist()
    }

    @Test
    fun `offers the link even when no shops were found`() {
        composeTestRule.setContent {
            ShopSearchResultsScreen(
                state = newport.copy(shops = emptyList()),
                onPlaceSelected = {}
            )
        }

        composeTestRule.onNodeWithText("No shops found nearby").assertExists()
        composeTestRule.onNodeWithText("Not the right place?").assertExists()
    }

    @Test
    fun `the alternatives close again once a new result arrives`() {
        val state = mutableStateOf<ShopSearchUiState>(newport)

        composeTestRule.setContent {
            ShopSearchResultsScreen(
                state = state.value,
                onPlaceSelected = { place ->
                    state.value = newport.copy(placeName = place.name)
                }
            )
        }

        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText("Newport, Kentucky, United States").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Newport, Isle of Wight, England, United Kingdom").assertDoesNotExist()
        composeTestRule.onNodeWithText("Not the right place?").assertExists()
    }

    // Shops left out by the settings.

    private fun london(vararg shops: Shop) = ShopSearchUiState.Success(
        shops = shops.toList(), placeName = "London, UK", centerLatitude = 51.5074, centerLongitude = -0.1278
    )

    private val sisterRay = Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null)

    @Test
    fun `says nothing when no shops were left out`() {
        composeTestRule.setContent { ShopSearchResultsScreen(state = london(sisterRay)) }

        composeTestRule.onNodeWithTag("hidden_shops_note").assertDoesNotExist()
    }

    @Test
    fun `says how many charity shops were left out`() {
        composeTestRule.setContent { ShopSearchResultsScreen(state = london(sisterRay), hiddenShops = 3) }

        composeTestRule.onNodeWithText("3 charity shops hidden by your settings").assertExists()
    }

    @Test
    fun `says it in the singular for one`() {
        composeTestRule.setContent { ShopSearchResultsScreen(state = london(sisterRay), hiddenShops = 1) }

        composeTestRule.onNodeWithText("1 charity shop hidden by your settings").assertExists()
    }

    @Test
    fun `tapping the note goes to the settings`() {
        var opened = 0
        composeTestRule.setContent {
            ShopSearchResultsScreen(state = london(sisterRay), hiddenShops = 2, onOpenSettings = { opened++ })
        }

        composeTestRule.onNodeWithTag("hidden_shops_note").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `when only charity shops were found, it does not say there were no shops`() {
        composeTestRule.setContent { ShopSearchResultsScreen(state = london(), hiddenShops = 4) }

        composeTestRule.onNodeWithText("No other shops found nearby").assertExists()
        composeTestRule.onNodeWithText("4 charity shops hidden by your settings").assertExists()
    }

    @Test
    fun `when nothing was found at all, it says so as before`() {
        composeTestRule.setContent { ShopSearchResultsScreen(state = london()) }

        composeTestRule.onNodeWithText("No shops found nearby").assertExists()
    }
}
