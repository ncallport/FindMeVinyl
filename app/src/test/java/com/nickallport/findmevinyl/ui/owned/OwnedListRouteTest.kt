package com.nickallport.findmevinyl.ui.owned

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.owned.OwnedAlbumUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class OwnedListRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val albums = listOf(OwnedAlbumUiModel("1", "a", "Abba", "Arrival"), OwnedAlbumUiModel("2", "b", "Bob Dylan", "Desire"))

    @Test
    fun `shows a spinner until the list has loaded, then the list`() {
        val gate = CompletableDeferred<List<OwnedAlbumUiModel>>()
        composeTestRule.setContent {
            FindMeVinylTheme { OwnedListRoute(load = { gate.await() }, exportText = { "" }, onShareText = {}, onOpenAlbum = {}) }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("loading_indicator").assertExists()

        gate.complete(albums)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
        composeTestRule.onNodeWithTag("owned_1").assertExists()
    }

    @Test
    fun `Share hands the text of the whole list on`() {
        val shared = mutableListOf<String>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                OwnedListRoute(load = { albums }, exportText = { "the list" }, onShareText = { shared.add(it) }, onOpenAlbum = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_share").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("the list"), shared)
    }

    @Test
    fun `loads the list once when it opens`() {
        var loads = 0
        composeTestRule.setContent {
            FindMeVinylTheme {
                OwnedListRoute(load = { loads++; albums }, exportText = { "" }, onShareText = {}, onOpenAlbum = {})
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(1, loads)
    }
}
