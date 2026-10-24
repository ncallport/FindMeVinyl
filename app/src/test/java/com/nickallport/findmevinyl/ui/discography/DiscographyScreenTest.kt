package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.discography.Grouping
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.nickallport.findmevinyl.discography.DiscographyCategory
import org.junit.Assert.assertEquals
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A tall window, so the header, the chips and several rows all fit and every row is composed.
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class DiscographyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows a header and the albums for each category present`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            ),
            DiscographyCategory.LIVE_ALBUM to listOf(
                AlbumUiModel("2", "Live at Pompeii", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {})
        }

        composeTestRule.onNodeWithText("Studio Albums").assertExists()
        composeTestRule.onNodeWithText("The Dark Side of the Moon").assertExists()
        composeTestRule.onNodeWithText("Live Albums").assertExists()
        composeTestRule.onNodeWithText("Live at Pompeii").assertExists()
    }

    @Test
    fun `categories with no albums are not shown`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {})
        }

        composeTestRule.onNodeWithText("Compilations").assertDoesNotExist()
    }

    private val oneAlbum = mapOf(
        DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Foxtrot", owned = false, wanted = false))
    )

    @Test
    fun `shows a footer while more albums are still loading`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = oneAlbum, onToggleOwned = {}, loadingMore = true)
        }

        composeTestRule.onNodeWithTag("loading_more").assertExists()
        composeTestRule.onNodeWithText("Loading more albums…").assertExists()
    }

    @Test
    fun `has no footer once everything has loaded`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = oneAlbum, onToggleOwned = {})
        }

        composeTestRule.onNodeWithTag("loading_more").assertDoesNotExist()
        composeTestRule.onNodeWithTag("load_error").assertDoesNotExist()
    }

    @Test
    fun `says some albums could not be loaded and offers to try again`() {
        var retries = 0

        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = oneAlbum, onToggleOwned = {},
                loadError = "HTTP 503 Server Error", onRetry = { retries++ }
            )
        }

        composeTestRule.onNodeWithTag("load_error").assertExists()
        composeTestRule.onNodeWithText("Couldn't load all the albums").assertExists()
        composeTestRule.onNodeWithText("HTTP 503 Server Error").assertExists()

        composeTestRule.onNodeWithTag("retry_button").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `keeps its place when albums are added above the ones on screen`() {
        val original = (10..40).map { AlbumUiModel("$it", "Album $it", owned = false, wanted = false) }
        var albums by mutableStateOf(original)

        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = mapOf(DiscographyCategory.STUDIO_ALBUM to albums),
                onToggleOwned = {}
            )
        }
        composeTestRule.onNodeWithTag("discography_list").performScrollToIndex(20)
        composeTestRule.onNodeWithText("Album 29").assertIsDisplayed()

        albums = (1..9).map { AlbumUiModel("$it", "Album $it", owned = false, wanted = false) } + original
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album 29").assertIsDisplayed()
    }

    @Test
    fun `tapping an album opens it with its id`() {
        var openedId: String? = null
        var toggledId: String? = null
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = grouped,
                onToggleOwned = { toggledId = it },
                onOpenAlbum = { openedId = it }
            )
        }

        composeTestRule.onNodeWithTag("album_row").performClick()

        assertEquals("1", openedId)
        assertEquals(null, toggledId)
    }

    @Test
    fun `tapping the Owned pill invokes the callback with the album id`() {
        var toggledId: String? = null
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "The Dark Side of the Moon", owned = false, wanted = false)
            )
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = { toggledId = it })
        }

        composeTestRule.onNodeWithTag("owned_toggle").performClick()

        assertEquals("1", toggledId)
    }

    // Owned albums grouped at the top.

    private val mixed = mapOf(
        DiscographyCategory.STUDIO_ALBUM to listOf(
            AlbumUiModel("1", "Studio Owned", owned = true, wanted = false),
            AlbumUiModel("2", "Studio Other", owned = false, wanted = false)
        ),
        DiscographyCategory.LIVE_ALBUM to listOf(
            AlbumUiModel("3", "Live Owned", owned = true, wanted = false),
            AlbumUiModel("4", "Live Other", owned = false, wanted = false)
        ),
        DiscographyCategory.EP to listOf(
            AlbumUiModel("5", "Only Owned EP", owned = true, wanted = false)
        )
    )

    private fun top(text: String) = composeTestRule.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `the categories are in the order studio albums, compilations, soundtracks, live albums, EPs, singles, other`() {
        val grouped = mapOf(
            DiscographyCategory.OTHER to listOf(AlbumUiModel("6", "An Other", owned = false, wanted = false)),
            DiscographyCategory.SINGLE to listOf(AlbumUiModel("5", "A Single", owned = false, wanted = false)),
            DiscographyCategory.EP to listOf(AlbumUiModel("4", "An EP", owned = false, wanted = false)),
            DiscographyCategory.LIVE_ALBUM to listOf(AlbumUiModel("3", "A Live", owned = false, wanted = false)),
            DiscographyCategory.SOUNDTRACK to listOf(AlbumUiModel("7", "A Soundtrack", owned = false, wanted = false)),
            DiscographyCategory.COMPILATION to listOf(AlbumUiModel("2", "A Compilation", owned = false, wanted = false)),
            DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "A Studio", owned = false, wanted = false))
        )

        composeTestRule.setContent { DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {}) }

        val headers = listOf("Studio Albums", "Compilations", "Soundtracks", "Live Albums", "EPs", "Singles", "Other Releases")
        headers.zipWithNext().forEach { (before, after) -> assertTrue("$before before $after", top(before) < top(after)) }
    }

    @Test
    fun `with owned grouped, the owned albums are in a section at the top`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = mixed, onToggleOwned = {}, grouping = Grouping(owned = true))
        }

        composeTestRule.onNodeWithTag("owned_section_header").assertExists()
        val header = composeTestRule.onNodeWithTag("owned_section_header").fetchSemanticsNode().positionInRoot.y
        assertTrue(header < top("Studio Owned"))
        assertTrue(top("Studio Owned") < top("Live Owned"))
        assertTrue(top("Live Owned") < top("Only Owned EP"))
        assertTrue(top("Only Owned EP") < top("Studio Albums"))
    }

    @Test
    fun `an owned album is in the owned section and not in its category as well`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = mixed, onToggleOwned = {}, grouping = Grouping(owned = true))
        }

        composeTestRule.onAllNodesWithText("Studio Owned").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Live Owned").assertCountEquals(1)
        composeTestRule.onNodeWithText("Studio Other").assertExists()
        composeTestRule.onNodeWithText("Live Other").assertExists()
    }

    @Test
    fun `a category whose albums are all owned disappears while owned come first`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = mixed, onToggleOwned = {}, grouping = Grouping(owned = true))
        }

        composeTestRule.onNodeWithText("EPs").assertDoesNotExist()
        composeTestRule.onNodeWithText("Studio Albums").assertExists()
        composeTestRule.onNodeWithText("Live Albums").assertExists()
    }

    @Test
    fun `without grouping, there is no owned section and each album stays in its category`() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = mixed, onToggleOwned = {})
        }

        composeTestRule.onNodeWithTag("owned_section_header").assertDoesNotExist()
        composeTestRule.onNodeWithText("EPs").assertExists()
        assertTrue(top("Studio Albums") < top("Studio Owned"))
    }

    @Test
    fun `with owned grouped but nothing owned, there is no owned section`() {
        val none = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Studio Other", owned = false, wanted = false))
        )
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = none, onToggleOwned = {}, grouping = Grouping(owned = true))
        }

        composeTestRule.onNodeWithTag("owned_section_header").assertDoesNotExist()
        composeTestRule.onNodeWithText("Studio Albums").assertExists()
    }

    @Test
    fun `the owned section's albums can still be opened and toggled`() {
        var opened: String? = null
        var toggled: String? = null
        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = mixed, onToggleOwned = { toggled = it }, onOpenAlbum = { opened = it }, grouping = Grouping(owned = true)
            )
        }

        composeTestRule.onNodeWithText("Studio Owned").performClick()
        composeTestRule.onAllNodesWithTag("owned_toggle")[0].performClick()

        assertEquals("1", opened)
        assertEquals("1", toggled)
    }

    @Test
    fun `a row shows the name of the artist the album is filed under`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "Carnage", owned = false, wanted = false, artistName = "Nick Cave")
            )
        )

        composeTestRule.setContent { DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {}) }

        composeTestRule.onNodeWithTag("album_artist", useUnmergedTree = true).assertTextEquals("Nick Cave")
    }

    @Test
    fun `an album with no name of its own takes the name given for the page`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Desire", owned = false, wanted = false))
        )

        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {}, defaultArtistName = "Bob Dylan")
        }

        composeTestRule.onNodeWithTag("album_artist", useUnmergedTree = true).assertTextEquals("Bob Dylan")
    }

    @Test
    fun `with no name anywhere the row shows no artist line`() {
        val grouped = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Desire", owned = false, wanted = false))
        )

        composeTestRule.setContent { DiscographyScreen(groupedAlbums = grouped, onToggleOwned = {}) }

        composeTestRule.onNodeWithTag("album_artist", useUnmergedTree = true).assertDoesNotExist()
    }
}
