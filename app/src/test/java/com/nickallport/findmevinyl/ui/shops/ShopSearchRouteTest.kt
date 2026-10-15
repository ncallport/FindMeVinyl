package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.SearchResultCache
import com.nickallport.findmevinyl.shops.PlaceCandidate
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    /** A cache that says the nearby search has already been tried, so a test starts from the prompt. */
    private fun triedCache() = SearchResultCache().apply { nearbyTried = true }

    private fun searchFor(town: String) {
        composeTestRule.onNodeWithTag("search_field").performTextReplacement(town)
        composeTestRule.onNodeWithTag("search_field").performImeAction()
        composeTestRule.waitForIdle()
    }

    private fun searchNewport() = searchFor("Newport")

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent { FindMeVinylTheme { content() } }
    }

    @Test
    fun `shows a prompt when nothing has been searched`() {
        show { ShopSearchRoute(search = { oneShop }, cache = triedCache()) }

        composeTestRule.onNodeWithText("Search for a town to find record shops").assertExists()
    }

    @Test
    fun `shows the shops once the search completes`() {
        show { ShopSearchRoute(search = { oneShop }, cache = triedCache()) }

        searchFor("London")

        composeTestRule.onNodeWithText("Sister Ray").assertExists()
    }

    @Test
    fun `passes the trimmed town to the search`() {
        var searchedFor: String? = null
        show { ShopSearchRoute(search = { searchedFor = it; oneShop }, cache = triedCache()) }

        searchFor("  London  ")

        assertEquals("London", searchedFor)
    }

    @Test
    fun `shows a message when the town is not found`() {
        show { ShopSearchRoute(search = { ShopSearchUiState.TownNotFound(it) }, cache = triedCache()) }

        searchFor("Atlantis")

        composeTestRule.onNodeWithText("Couldn't find Atlantis").assertExists()
    }

    @Test
    fun `does not search when the town is blank`() {
        var searchCount = 0
        show { ShopSearchRoute(search = { searchCount++; oneShop }, cache = triedCache()) }

        searchFor("   ")

        assertEquals(0, searchCount)
    }

    @Test
    fun `has no search button, the keyboard searches`() {
        show { ShopSearchRoute(search = { oneShop }, cache = triedCache()) }

        composeTestRule.onNodeWithTag("town_search_button").assertDoesNotExist()
    }

    @Test
    fun `tapping Near me shows the nearby shops`() {
        show { ShopSearchRoute(search = { oneShop }, searchNearby = { nearbyShops }, cache = triedCache()) }

        composeTestRule.onNodeWithTag("use_location_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Near Me Records").assertExists()
        composeTestRule.onNodeWithText("Your location").assertExists()
    }

    @Test
    fun `shows the error message when the nearby search fails`() {
        show {
            ShopSearchRoute(
                search = { oneShop },
                searchNearby = { ShopSearchUiState.Error("Location permission is needed to find shops near you") },
                cache = triedCache()
            )
        }

        composeTestRule.onNodeWithTag("use_location_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Location permission is needed to find shops near you").assertExists()
    }

    @Test
    fun `does not offer Near me when no nearby search is provided`() {
        show { ShopSearchRoute(search = { oneShop }, cache = triedCache()) }

        composeTestRule.onNodeWithTag("use_location_button").assertDoesNotExist()
    }

    @Test
    fun `tapping a found shop invokes the callback with that shop`() {
        var selected: Shop? = null
        show { ShopSearchRoute(search = { oneShop }, onShopSelected = { selected = it }, cache = triedCache()) }

        searchFor("London")
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
        show {
            ShopSearchRoute(search = { newportResult }, searchPlace = { _, _ -> isleResult }, cache = triedCache())
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
        show {
            ShopSearchRoute(
                search = { newportResult },
                searchPlace = { place, others ->
                    passedPlace = place
                    passedOthers = others
                    newportResult.copy(placeName = place.name, otherPlaces = others)
                },
                cache = triedCache()
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
        show {
            ShopSearchRoute(
                search = { newportResult },
                searchPlace = { place, others ->
                    searchedPlaces.add(place.name)
                    newportResult.copy(placeName = place.name, otherPlaces = others)
                },
                cache = triedCache()
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
        show { ShopSearchRoute(search = { newportResult }, cache = triedCache()) }

        searchNewport()

        composeTestRule.onNodeWithText("Welsh Records").assertExists()
        composeTestRule.onNodeWithText("Not the right place?").assertDoesNotExist()
    }

    // --- Searching near you when the page opens ---

    @Test
    fun `searches near you by itself the first time it opens`() {
        var nearbySearches = 0
        val cache = SearchResultCache()
        show { ShopSearchRoute(search = { oneShop }, searchNearby = { nearbySearches++; nearbyShops }, cache = cache) }
        composeTestRule.waitForIdle()

        assertEquals(1, nearbySearches)
        composeTestRule.onNodeWithText("Near Me Records").assertExists()
        assertEquals(nearbyShops, cache.shop)
        assertEquals(true, cache.nearbyTried)
    }

    @Test
    fun `shows a spinner while it looks for shops near you`() {
        val gate = CompletableDeferred<Unit>()
        show { ShopSearchRoute(search = { oneShop }, searchNearby = { gate.await(); nearbyShops }, cache = SearchResultCache()) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertExists()

        gate.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
    }

    @Test
    fun `shows the result kept from an earlier visit without searching again`() {
        var nearbySearches = 0
        val cache = SearchResultCache().apply { shop = oneShop }
        show { ShopSearchRoute(search = { oneShop }, searchNearby = { nearbySearches++; nearbyShops }, cache = cache) }
        composeTestRule.waitForIdle()

        assertEquals(0, nearbySearches)
        composeTestRule.onNodeWithText("Sister Ray").assertExists()
    }

    @Test
    fun `does not search near you by itself again once it has tried`() {
        var nearbySearches = 0
        show { ShopSearchRoute(search = { oneShop }, searchNearby = { nearbySearches++; nearbyShops }, cache = triedCache()) }
        composeTestRule.waitForIdle()

        assertEquals(0, nearbySearches)
        composeTestRule.onNodeWithText("Search for a town to find record shops").assertExists()
    }

    @Test
    fun `shows why when searching near you by itself fails, and Near me tries again`() {
        var attempts = 0
        show {
            ShopSearchRoute(
                search = { oneShop },
                searchNearby = {
                    attempts++
                    if (attempts == 1) ShopSearchUiState.Error("Location permission is needed to find shops near you")
                    else nearbyShops
                },
                cache = SearchResultCache()
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Location permission is needed to find shops near you").assertExists()

        composeTestRule.onNodeWithTag("use_location_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Near Me Records").assertExists()
        assertEquals(2, attempts)
    }

    @Test
    fun `a town search replaces the nearby result and is the one kept`() {
        val cache = SearchResultCache()
        show { ShopSearchRoute(search = { oneShop }, searchNearby = { nearbyShops }, cache = cache) }
        composeTestRule.waitForIdle()

        searchFor("London")

        composeTestRule.onNodeWithText("Sister Ray").assertExists()
        composeTestRule.onNodeWithText("Near Me Records").assertDoesNotExist()
        assertEquals(oneShop, cache.shop)
    }

    @Test
    fun `does not search near you by itself when there is no way to`() {
        show { ShopSearchRoute(search = { oneShop }, cache = SearchResultCache()) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search for a town to find record shops").assertExists()
    }

    // --- Rotation ---

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `keeps the town and results after the state is restored`() = runComposeUiTest {
        val cache = triedCache()
        var searches = 0
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            FindMeVinylTheme { ShopSearchRoute(search = { searches++; newportResult }, cache = cache) }
        }

        onNodeWithTag("search_field").performTextReplacement("Newport")
        onNodeWithTag("search_field").performImeAction()
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithTag("search_field").assertTextContains("Newport")
        onNodeWithText("Welsh Records").assertExists()
        assertEquals(1, searches)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `restoring while searching near you does not repeat a search that finished`() = runComposeUiTest {
        val cache = SearchResultCache()
        var nearbySearches = 0
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            FindMeVinylTheme {
                ShopSearchRoute(search = { oneShop }, searchNearby = { nearbySearches++; nearbyShops }, cache = cache)
            }
        }
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithText("Near Me Records").assertExists()
        assertEquals(1, nearbySearches)
    }

    // Public holidays for the opening hours.

    private val england = com.nickallport.findmevinyl.holidays.HolidayRegion("GB", "GB-ENG")
    private val scotland = com.nickallport.findmevinyl.holidays.HolidayRegion("GB", "GB-SCT")

    private fun resultIn(region: com.nickallport.findmevinyl.holidays.HolidayRegion?) = oneShop.copy(
        shops = listOf(
            Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, "Mo-Su 00:00-24:00; PH off")
        ),
        region = region
    )

    @Test
    fun `looks up the holidays of the region the shops are in`() {
        val asked = mutableListOf<com.nickallport.findmevinyl.holidays.HolidayRegion>()

        show {
            ShopSearchRoute(
                search = { resultIn(england) },
                cache = triedCache(),
                loadPublicHolidays = { region -> asked.add(region); emptySet() }
            )
        }
        searchFor("London")

        assertEquals(listOf(england), asked)
    }

    @Test
    fun `follows the public holiday hours on a public holiday`() {
        show {
            ShopSearchRoute(
                search = { resultIn(england) },
                cache = triedCache(),
                loadPublicHolidays = { setOf(java.time.LocalDate.now()) }
            )
        }
        searchFor("London")

        composeTestRule.onNodeWithText("(public holiday today)", substring = true).assertExists()
    }

    @Test
    fun `drops the note about holidays once the holidays are known`() {
        show {
            ShopSearchRoute(
                search = { resultIn(england) },
                cache = triedCache(),
                loadPublicHolidays = { emptySet() }
            )
        }
        searchFor("London")

        composeTestRule.onNodeWithText("Open now").assertExists()
    }

    @Test
    fun `keeps the note when the holidays could not be found`() {
        show {
            ShopSearchRoute(
                search = { resultIn(england) },
                cache = triedCache(),
                loadPublicHolidays = { null }
            )
        }
        searchFor("London")

        composeTestRule.onNodeWithText("Open now (public holidays may differ)").assertExists()
    }

    @Test
    fun `keeps the note when the region of the shops is not known, and asks nobody`() {
        var asked = 0

        show {
            ShopSearchRoute(
                search = { resultIn(null) },
                cache = triedCache(),
                loadPublicHolidays = { asked++; emptySet() }
            )
        }
        searchFor("London")

        composeTestRule.onNodeWithText("Open now (public holidays may differ)").assertExists()
        assertEquals(0, asked)
    }

    @Test
    fun `asks again when another place with another region is chosen`() {
        val asked = mutableListOf<com.nickallport.findmevinyl.holidays.HolidayRegion>()
        val edinburgh = PlaceCandidate("Edinburgh, Scotland", 55.95, -3.19, scotland)

        show {
            ShopSearchRoute(
                search = { resultIn(england).copy(otherPlaces = listOf(edinburgh)) },
                searchPlace = { _, _ -> resultIn(scotland) },
                cache = triedCache(),
                loadPublicHolidays = { region -> asked.add(region); emptySet() }
            )
        }
        searchFor("London")
        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText("Edinburgh, Scotland").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(england, scotland), asked)
    }

    @Test
    fun `the place shown before is kept with its region among the other places`() {
        val others = mutableListOf<List<PlaceCandidate>>()

        show {
            ShopSearchRoute(
                search = { resultIn(england).copy(otherPlaces = listOf(wales)) },
                searchPlace = { _, rest -> others.add(rest); resultIn(england) },
                cache = triedCache()
            )
        }
        searchFor("London")
        composeTestRule.onNodeWithTag("other_places_toggle").performClick()
        composeTestRule.onNodeWithText(wales.name).performClick()
        composeTestRule.waitForIdle()

        assertEquals(PlaceCandidate("London, UK", 51.5074, -0.1278, england), others.single().first())
    }
}
