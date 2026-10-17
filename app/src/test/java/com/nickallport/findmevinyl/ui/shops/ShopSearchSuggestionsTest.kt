package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.nickallport.findmevinyl.SearchResultCache
import com.nickallport.findmevinyl.photon.PlaceSuggestion
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

/** Suggestions of places while typing in the Shops page's search field. */
@RunWith(RobolectricTestRunner::class)
class ShopSearchSuggestionsTest {

    @get:Rule
    val rule = createComposeRule()

    private val caldas = PlaceSuggestion("Caldas da Rainha, Leiria, Portugal", 39.4071857, -9.1346004, "PT")
    private val reis = PlaceSuggestion("Caldas de Reis, Galicia, Spain", 42.6, -8.64, "ES")

    private val caldasResult = ShopSearchUiState.Success(
        shops = listOf(Shop("1", "Discos da Rainha", ShopCategory.RECORD_SHOP, 39.40, -9.13, null)),
        placeName = caldas.label,
        centerLatitude = caldas.latitude,
        centerLongitude = caldas.longitude
    )

    private val asked = mutableListOf<String>()
    private val searched = mutableListOf<String>()
    private val picked = mutableListOf<PlaceSuggestion>()

    private fun show(suggest: suspend (String) -> List<PlaceSuggestion> = { listOf(caldas, reis) }) {
        rule.setContent {
            FindMeVinylTheme {
                ShopSearchRoute(
                    search = { town -> searched.add(town); ShopSearchUiState.TownNotFound(town) },
                    suggestPlaces = { text -> asked.add(text); suggest(text) },
                    searchSuggestion = { s -> picked.add(s); caldasResult },
                    cache = SearchResultCache().apply { nearbyTried = true }
                )
            }
        }
    }

    private fun type(text: String) {
        rule.onNodeWithTag("search_field").performTextReplacement(text)
    }

    private fun pause(millis: Long) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    @Test
    fun `suggestions appear a little after typing stops`() {
        show()

        type("Caldas")
        pause(600)

        rule.onNodeWithTag("place_suggestion_0").assertTextContains(caldas.label)
        rule.onNodeWithTag("place_suggestion_1").assertTextContains(reis.label)
    }

    @Test
    fun `nothing is asked while the typing goes on, only for the last text`() {
        show()

        type("Cal")
        pause(200)
        type("Caldas")
        pause(200)
        type("Caldas da")
        assertEquals(emptyList<String>(), asked)
        pause(600)

        assertEquals(listOf("Caldas da"), asked)
    }

    @Test
    fun `tapping a suggestion searches there and puts its name in the field`() {
        show()
        type("Caldas")
        pause(600)

        rule.onNodeWithTag("place_suggestion_0").performClick()
        rule.waitForIdle()

        assertEquals(listOf(caldas), picked)
        rule.onNodeWithTag("search_field").assertTextContains(caldas.label)
        rule.onNodeWithText("Discos da Rainha").assertExists()
        rule.onNodeWithTag("place_suggestion_0").assertDoesNotExist()
        assertEquals(emptyList<String>(), searched)
    }

    @Test
    fun `the name put in the field by a tap is not suggested for again`() {
        show()
        type("Caldas")
        pause(600)
        rule.onNodeWithTag("place_suggestion_0").performClick()
        pause(1000)

        assertEquals(listOf("Caldas"), asked)
        rule.onNodeWithTag("place_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `the keyboard search still searches for the text and hides the suggestions`() {
        show()
        type("Caldas")
        pause(600)

        rule.onNodeWithTag("search_field").performImeAction()
        rule.waitForIdle()

        assertEquals(listOf("Caldas"), searched)
        assertEquals(emptyList<PlaceSuggestion>(), picked)
        rule.onNodeWithTag("place_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `clearing the field clears the suggestions`() {
        show()
        type("Caldas")
        pause(600)

        type("")
        pause(600)

        rule.onNodeWithTag("place_suggestion_0").assertDoesNotExist()
        assertEquals(listOf("Caldas"), asked)
    }

    @Test
    fun `an answer for text that has been typed over is not shown`() {
        val slow = CompletableDeferred<List<PlaceSuggestion>>()
        show { text -> if (text == "Cal") slow.await() else emptyList() }

        type("Cal")
        pause(600)
        type("Calx")
        pause(600)
        slow.complete(listOf(caldas))
        rule.waitForIdle()

        rule.onNodeWithTag("place_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `no suggestions means the page is as it was`() {
        show { emptyList() }

        type("Zzz")
        pause(600)

        rule.onNodeWithText("Search for a town to find record shops").assertExists()
    }
}
