package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.SearchResultCache
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.nickallport.findmevinyl.shops.PlaceCandidate

@RunWith(RobolectricTestRunner::class)
class ShopSearchRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val oneShop = ShopSearchUiState.Success(
        shops = listOf(Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null)),
        placeName = "London, UK",
        centerLatitude = 51.5074,
        centerLongitude = -0.1278
    )

    private val nearbyShops = ShopSearchUiState.Success(
        shops = listOf(Shop("9", "Near Me Records", ShopCategory.RECORD_SHOP, 39.16, -8.79, null)),
        placeName = "Your location",
        centerLatitude = 39.16,
        centerLongitude = -8.79
    )

    private val wales = PlaceCandidate("Newport, Wales, United Kingdom", 51.5842, -2.9977)
    private val wight = PlaceCandidate("Newport, Isle of Wight, England, United Kingdom", 50.7, -1.2915)
    private val kentucky = PlaceCandidate("Newport, Kentucky, United States", 39.1, -84.45)

    private val newportResult = ShopSearchUiState.Success(
        shops = listOf(Shop("1", "Welsh Records", ShopCategory.RECORD_SHOP, 51.58, -2.99, null)),
        placeName = wales.name,
        centerLatitude = wales.latitude,
        centerLongitude = wales.longitude,
        otherPlaces = listOf(wight, kentucky)
    )

    private fun searchNewport() {
        composeTestRule.onNodeWithTag("town_field").performTextInput("Newport")
        composeTestRule.onNodeWithTag("town_search_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows a prompt before any search`() {
        composeTestRule.setContent {
            ShopSearchRoute(search = { oneShop })
        }

        composeTestRule.onNodeWithText("Enter a town to find record shops").assertExists()
    }

    @Test
    fun `shows the shops once the search completes`() {
        composeTestRule.setContent {
            ShopSearchRoute(search = { oneShop })
        }

        composeTestRule.onNodeWithTag("town_field").performTextInput("London")
        composeTestRule.onNodeWithTag("town_search_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sister Ray").assertExists()
    }

    @Test
    fun `passes the trimmed town to the search`() {
        var searchedFor: String? = null

        composeTestRule.setContent {
            ShopSearchRoute(search = { town ->
                searchedFor = town
                oneShop
            })
        }

        composeTestRule.onNodeWithTag("town_field").performTextInput("  London  ")
        composeTestRule.onNodeWithTag("town_search_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("London", searchedFor)
    }

    @Test
    fun `shows a message when the town is not found`() {
        composeTestRule.setContent {
            ShopSearchRoute(search = { town -> ShopSearchUiState.TownNotFound(town) })
        }

        composeTestRule.onNodeWithTag("town_field").performTextInput("Atlantis")
        composeTestRule.onNodeWithTag("town_search_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Couldn't find Atlantis").assertExists()
    }

    @Test
    fun `does not search when the town is blank`() {
        var searchCount = 0

        composeTestRule.setContent {
            ShopSearchRoute(search = {
                searchCount++
                oneShop
            })
        }

        composeTestRule.onNodeWithTag("town_field").performTextInput("   ")
        composeTestRule.onNodeWithTag("town_search_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, searchCount)
    }

    @Test
    fun `tapping Use my location shows the nearby shops`() {
        composeTestRule.setContent {
            ShopSearchRoute(
                search = { oneShop },
                searchNearby = { nearbyShops }
            )
        }

        composeTestRule.onNodeWithTag("use_location_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Near Me Records").assertExists()
        composeTestRule.onNodeWithText("Your location").assertExists()
    }

    @Test
    fun `shows the error message when the nearby search fails`() {
        composeTestRule.setContent {
            ShopSearchRoute(
                search = { oneShop },
                searchNearby = {
                    ShopSearchUiState.Error("Location permission is needed to find shops near you")
                }
            )
        }

        composeTestRule.onNodeWithTag("use_location_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Location permission is needed to find shops near you")
            .assertExists()
    }

    @Test
    fun `does not offer Use my location when no nearby search is provided`() {
        composeTestRule.setContent {
            ShopSearchRoute(search = { oneShop })
        }

        composeTestRule.onNodeWithTag("use_location_button").assertDoesNotExist()
    }

    @Test
    fun `tapping a found shop invokes the callback with that shop`() {
        var selected: Shop? = null

        composeTestRule.setContent {
            ShopSearchRoute(
                search = { oneShop },
                onShopSelected = { selected = it }
            )
        }

        composeTestRule.onNodeWithTag("town_field").performTextInput("London")
        composeTestRule.onNodeWithTag("town_search_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sister Ray").performClick()

        assertEquals("1", selected?.id)
    }

    @Test
    fun `picking another place shows the shops found there`() {
        val isleResult = ShopSearchUiState.Success(
            shops = listOf(Shop("2", "Island Records", ShopCategory.RECORD_SHOP, 50.70, -1.29, null)),
            placeName = wight.name,
            centerLatitude = wight.latitude,
            centerLongitude = wight.longitude
        )

        composeTestRule.setContent {
            ShopSearchRoute(
                search = { newportResult },
                searchPlace = { _, _ -> isleResult }
            )
        }

        searchNewport()
        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText(wight.name).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Island Records").assertExists()
        composeTestRule.onNodeWithText(wight.name).assertExists()
    }

    @Test
    fun `puts the old best match first among the alternatives after a pick`() {
        var passedPlace: PlaceCandidate? = null
        var passedOthers: List<PlaceCandidate>? = null

        composeTestRule.setContent {
            ShopSearchRoute(
                search = { newportResult },
                searchPlace = { place, others ->
                    passedPlace = place
                    passedOthers = others
                    newportResult.copy(placeName = place.name, otherPlaces = others)
                }
            )
        }

        searchNewport()
        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText(wight.name).performClick()
        composeTestRule.waitForIdle()

        assertEquals(wight, passedPlace)
        assertEquals(listOf(wales, kentucky), passedOthers)
    }

    @Test
    fun `can switch back to the original place after a pick`() {
        val searchedPlaces = mutableListOf<String>()

        composeTestRule.setContent {
            ShopSearchRoute(
                search = { newportResult },
                searchPlace = { place, others ->
                    searchedPlaces.add(place.name)
                    newportResult.copy(placeName = place.name, otherPlaces = others)
                }
            )
        }

        searchNewport()
        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText(wight.name).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText(wales.name).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(wight.name, wales.name), searchedPlaces)
    }

    @Test
    fun `does not offer other places when no place search is provided`() {
        composeTestRule.setContent {
            ShopSearchRoute(search = { newportResult })
        }

        searchNewport()

        composeTestRule.onNodeWithText("Welsh Records").assertExists()
        composeTestRule.onNodeWithText("Not the right place?").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `keeps the town and results after the state is restored`() = runComposeUiTest {
        val cache = SearchResultCache()
        var searches = 0
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            ShopSearchRoute(
                search = {
                    searches++
                    newportResult
                },
                cache = cache
            )
        }

        onNodeWithTag("town_field").performTextInput("Newport")
        onNodeWithTag("town_search_button").performClick()
        waitForIdle()

        restorationTester.emulateSaveAndRestore()

        onNodeWithTag("town_field").assertTextContains("Newport")
        onNodeWithText("Welsh Records").assertExists()
        assertEquals(1, searches)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `shows the prompt and keeps the town when the cache is empty after a restore`() =
        runComposeUiTest {
            val cache = SearchResultCache()
            val restorationTester = StateRestorationTester(this)

            restorationTester.setContent {
                ShopSearchRoute(search = { newportResult }, cache = cache)
            }

            onNodeWithTag("town_field").performTextInput("Newport")
            onNodeWithTag("town_search_button").performClick()
            waitForIdle()
            cache.shop = null

            restorationTester.emulateSaveAndRestore()

            onNodeWithTag("town_field").assertTextContains("Newport")
            onNodeWithText("Enter a town to find record shops").assertExists()
        }
}