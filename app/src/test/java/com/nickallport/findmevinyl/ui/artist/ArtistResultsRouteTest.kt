package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.SearchResultCache
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistResultsRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val genesis = ArtistSearchUiState.Success(listOf(ArtistDisplayItem("1", "Genesis", "Band from GB")))

    @Test
    fun `searches for the query when it opens and shows the results`() {
        val searched = mutableListOf<String>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { searched.add(it); genesis },
                    onArtistSelected = {}, onSearch = {}, cache = SearchResultCache()
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("Genesis"), searched)
        composeTestRule.onNodeWithText("Band from GB").assertExists()
    }

    @Test
    fun `shows a spinner while the search runs`() {
        val gate = CompletableDeferred<Unit>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { gate.await(); genesis },
                    onArtistSelected = {}, onSearch = {}, cache = SearchResultCache()
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertExists()

        gate.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
    }

    @Test
    fun `the search field starts with the query`() {
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { genesis },
                    onArtistSelected = {}, onSearch = {}, cache = SearchResultCache()
                )
            }
        }

        composeTestRule.onNodeWithTag("search_field").assertTextContains("Genesis")
    }

    @Test
    fun `searching again from the field asks for a new search`() {
        var next: String? = null
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { genesis },
                    onArtistSelected = {}, onSearch = { next = it }, cache = SearchResultCache()
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_field").performTextReplacement("Roxy Music")
        composeTestRule.onNodeWithTag("search_field").performImeAction()

        assertEquals("Roxy Music", next)
    }

    @Test
    fun `uses the results kept for the same query without searching again`() {
        val cache = SearchResultCache().apply { artist = genesis; artistQuery = "Genesis" }
        var searches = 0
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { searches++; genesis },
                    onArtistSelected = {}, onSearch = {}, cache = cache
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(0, searches)
        composeTestRule.onNodeWithText("Band from GB").assertExists()
    }

    @Test
    fun `searches when the kept results are for a different query`() {
        val cache = SearchResultCache().apply { artist = genesis; artistQuery = "Genesis" }
        val searched = mutableListOf<String>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Roxy Music", search = { searched.add(it); ArtistSearchUiState.Success(emptyList()) },
                    onArtistSelected = {}, onSearch = {}, cache = cache
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("Roxy Music"), searched)
        assertEquals("Roxy Music", cache.artistQuery)
    }

    @Test
    fun `keeps a successful search for next time`() {
        val cache = SearchResultCache()
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { genesis }, onArtistSelected = {}, onSearch = {}, cache = cache
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(genesis, cache.artist)
        assertEquals("Genesis", cache.artistQuery)
    }

    @Test
    fun `does not keep an error, so the search is tried again next time`() {
        val cache = SearchResultCache()
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { ArtistSearchUiState.Error("HTTP 503") },
                    onArtistSelected = {}, onSearch = {}, cache = cache
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HTTP 503").assertExists()
        assertEquals(null, cache.artist)
    }

    @Test
    fun `tapping a result opens it`() {
        var selected: ArtistDisplayItem? = null
        composeTestRule.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { genesis },
                    onArtistSelected = { selected = it }, onSearch = {}, cache = SearchResultCache()
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_result_1").performClick()

        assertEquals("Genesis", selected?.name)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `keeps the results and the typed text after the state is restored, without searching again`() = runComposeUiTest {
        val cache = SearchResultCache()
        var searches = 0
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            FindMeVinylTheme {
                ArtistResultsRoute(
                    query = "Genesis", search = { searches++; genesis },
                    onArtistSelected = {}, onSearch = {}, cache = cache
                )
            }
        }
        waitForIdle()
        onNodeWithTag("search_field").performTextReplacement("Genesis live")

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithText("Band from GB").assertExists()
        onNodeWithTag("search_field").assertTextContains("Genesis live")
        assertEquals(1, searches)
    }
}
