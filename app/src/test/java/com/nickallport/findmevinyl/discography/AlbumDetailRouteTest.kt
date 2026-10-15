package com.nickallport.findmevinyl.discography

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumDetailRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val details = AlbumDetails(
        releaseDate = "1970-10", country = "GB", label = "Charisma", catalogNumber = null,
        firstReleaseDate = "1970-10-23", format = "12\" Vinyl", genres = emptyList(),
        discs = listOf(Disc(null, "12\" Vinyl", listOf(Track("A1", "Looking for Someone", 424000))))
    )

    private var album = AlbumUiModel("1", "Trespass", owned = false, wanted = false)

    private fun show(
        loadDetails: suspend (String) -> AlbumDetails?,
        onToggleOwned: suspend (String, Boolean) -> Unit = { _, _ -> },
        onToggleWanted: suspend (String, Boolean) -> Unit = { _, _ -> }
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailRoute(
                    albumId = "1", title = "Trespass", artistName = "Genesis",
                    loadDetails = loadDetails,
                    loadAlbum = { album },
                    onToggleOwned = onToggleOwned,
                    onToggleWanted = onToggleWanted
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows a spinner while the details load, then the details`() {
        val gate = CompletableDeferred<AlbumDetails?>()
        show(loadDetails = { gate.await() })

        composeTestRule.onNodeWithTag("detail_loading").assertExists()

        gate.complete(details)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("detail_loading").assertDoesNotExist()
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
    }

    @Test
    fun `says so when the details cannot be loaded, and Try again loads them again`() {
        var calls = 0
        show(loadDetails = { if (++calls == 1) null else details })

        composeTestRule.onNodeWithText("Couldn't load the details").assertExists()

        composeTestRule.onNodeWithTag("detail_retry").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, calls)
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
    }

    @Test
    fun `loads the details of the album it was given`() {
        var asked: String? = null
        show(loadDetails = { asked = it; details })

        assertEquals("1", asked)
    }

    @Test
    fun `marking the album as owned tells the caller and shows the new state`() {
        var told: Pair<String, Boolean>? = null
        show(
            loadDetails = { details },
            onToggleOwned = { id, owned -> told = id to owned; album = album.copy(owned = !owned) }
        )

        composeTestRule.onNodeWithTag("detail_owned_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("1" to false, told)
        composeTestRule.onNodeWithTag("detail_owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("detail_want_toggle").assertDoesNotExist()
    }

    @Test
    fun `wanting the album tells the caller and shows the new state`() {
        var told: Pair<String, Boolean>? = null
        show(
            loadDetails = { details },
            onToggleWanted = { id, wanted -> told = id to wanted; album = album.copy(wanted = !wanted) }
        )

        composeTestRule.onNodeWithTag("detail_want_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("1" to false, told)
        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("Wanted")
    }

    @Test
    fun `a tap does not reload the details`() {
        var calls = 0
        show(
            loadDetails = { calls++; details },
            onToggleOwned = { _, owned -> album = album.copy(owned = !owned) }
        )

        composeTestRule.onNodeWithTag("detail_owned_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, calls)
    }
}
