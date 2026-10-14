package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nickallport.findmevinyl.discography.AlbumDetails
import com.nickallport.findmevinyl.discography.Disc
import com.nickallport.findmevinyl.discography.Track
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val details = AlbumDetails(
        releaseDate = "1970-10", country = "GB", label = "Charisma", catalogNumber = "CAS 1020",
        firstReleaseDate = "1970-10-23", format = "12\" Vinyl",
        genres = listOf("rock", "progressive rock"),
        discs = listOf(
            Disc(
                null, "12\" Vinyl",
                listOf(
                    Track("A1", "Looking for Someone", 424000), Track("A2", "White Mountain", 400000),
                    Track("B1", "Stagnation", null)
                )
            )
        )
    )

    private val notOwned = AlbumUiModel("1", "Trespass", owned = false, wanted = false)

    private fun show(
        state: AlbumDetailState = AlbumDetailState.Loaded(details),
        album: AlbumUiModel? = notOwned,
        darkTheme: Boolean = false,
        onRetry: () -> Unit = {},
        onToggleOwned: () -> Unit = {},
        onToggleWanted: () -> Unit = {},
        loadCover: suspend (String) -> Bitmap? = { null }
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme(darkTheme = darkTheme) {
                AlbumDetailScreen(
                    albumId = "1", title = "Trespass", artistName = "Genesis", album = album, state = state,
                    onRetry = onRetry, onToggleOwned = onToggleOwned, onToggleWanted = onToggleWanted, loadCover = loadCover
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows the title and the artist`() {
        show()

        composeTestRule.onNodeWithTag("detail_title").assertTextEquals("Trespass")
        composeTestRule.onNodeWithTag("detail_artist").assertTextEquals("Genesis")
    }

    @Test
    fun `shows the title and artist while the details are still loading`() {
        show(state = AlbumDetailState.Loading)

        composeTestRule.onNodeWithTag("detail_title").assertTextEquals("Trespass")
        composeTestRule.onNodeWithTag("detail_loading").assertExists()
    }

    @Test
    fun `shows the record placeholder until the cover arrives and the cover once it has`() {
        show()
        composeTestRule.onNodeWithTag("detail_cover_placeholder", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `shows the cover when it loads`() {
        show(loadCover = { Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888) })

        composeTestRule.onNodeWithTag("detail_cover_image", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `shows when it was first released`() {
        show()

        composeTestRule.onNodeWithText("First released 1970-10-23").assertExists()
    }

    @Test
    fun `shows the details of the pressing`() {
        show()

        composeTestRule.onNodeWithTag("info_Released").assertTextEquals("Released", "1970-10")
        composeTestRule.onNodeWithTag("info_Country").assertTextEquals("Country", "United Kingdom")
        composeTestRule.onNodeWithTag("info_Label").assertTextEquals("Label", "Charisma, CAS 1020")
        composeTestRule.onNodeWithTag("info_Format").assertTextEquals("Format", "12\" Vinyl")
    }

    @Test
    fun `leaves out details that are not known`() {
        show(
            state = AlbumDetailState.Loaded(
                details.copy(releaseDate = null, country = null, label = null, catalogNumber = null, format = null)
            )
        )

        composeTestRule.onNodeWithTag("info_Released").assertDoesNotExist()
        composeTestRule.onNodeWithTag("info_Country").assertDoesNotExist()
        composeTestRule.onNodeWithTag("info_Label").assertDoesNotExist()
        composeTestRule.onNodeWithTag("info_Format").assertDoesNotExist()
    }

    @Test
    fun `shows a label with no catalogue number`() {
        show(state = AlbumDetailState.Loaded(details.copy(catalogNumber = null)))

        composeTestRule.onNodeWithTag("info_Label").assertTextEquals("Label", "Charisma")
    }

    @Test
    fun `shows the genres`() {
        show()

        composeTestRule.onAllNodesWithTag("genre_chip").assertCountEquals(2)
        composeTestRule.onNodeWithText("progressive rock").assertExists()
    }

    @Test
    fun `shows the tracks with their numbers, titles and lengths, in sides`() {
        show()

        composeTestRule.onNodeWithText("Side A").assertExists()
        composeTestRule.onNodeWithText("Side B").assertExists()
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
        composeTestRule.onNodeWithText("7:04").assertExists()
        composeTestRule.onNodeWithText("A1").assertExists()
        composeTestRule.onAllNodesWithTag("track_row").assertCountEquals(3)
    }

    @Test
    fun `a track with no known length shows no length`() {
        show(state = AlbumDetailState.Loaded(details.copy(discs = listOf(Disc(null, "CD", listOf(Track("1", "Only", null)))))))

        composeTestRule.onNodeWithText("Only").assertExists()
        composeTestRule.onNodeWithTag("track_length").assertDoesNotExist()
    }

    @Test
    fun `shows the running time of a side when every track has a length`() {
        show(state = AlbumDetailState.Loaded(details.copy(discs = listOf(Disc(null, "CD", listOf(Track("1", "a", 60_000), Track("2", "b", 125_000)))))))

        composeTestRule.onNodeWithTag("section_total").assertTextEquals("3:05")
    }

    @Test
    fun `labels each disc of a set`() {
        val set = details.copy(
            discs = listOf(
                Disc(null, "CD", listOf(Track("1", "One", null))),
                Disc("Live", "CD", listOf(Track("1", "Two", null)))
            )
        )
        show(state = AlbumDetailState.Loaded(set))

        composeTestRule.onNodeWithText("Disc 1").assertExists()
        composeTestRule.onNodeWithText("Disc 2: Live").assertExists()
    }

    @Test
    fun `says there are no details when MusicBrainz has none`() {
        show(state = AlbumDetailState.Loaded(AlbumDetails(null, null, null, null, null, null, emptyList(), emptyList())))

        composeTestRule.onNodeWithTag("detail_empty").assertExists()
        composeTestRule.onNodeWithText("MusicBrainz has no details for this album yet").assertExists()
    }

    @Test
    fun `says so when the details could not be loaded, and tries again`() {
        var retries = 0
        show(state = AlbumDetailState.Failed, onRetry = { retries++ })

        composeTestRule.onNodeWithText("Couldn't load the details").assertExists()
        composeTestRule.onNodeWithTag("detail_retry").performScrollTo().performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `an album that is not owned offers Owned and Want`() {
        show()

        composeTestRule.onNodeWithTag("detail_owned_toggle").assertTextEquals("Mark as owned")
        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("+ Want")
    }

    @Test
    fun `tapping the buttons toggles owned and wanted`() {
        var owned = 0
        var wanted = 0
        show(onToggleOwned = { owned++ }, onToggleWanted = { wanted++ })

        composeTestRule.onNodeWithTag("detail_owned_toggle").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("detail_want_toggle").performScrollTo().performClick()

        assertEquals(1, owned)
        assertEquals(1, wanted)
    }

    @Test
    fun `an owned album says so and has no Want button`() {
        show(album = notOwned.copy(owned = true))

        composeTestRule.onNodeWithTag("detail_owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("detail_want_toggle").assertDoesNotExist()
    }

    @Test
    fun `a wanted album says so`() {
        show(album = notOwned.copy(wanted = true))

        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("Wanted")
    }

    @Test
    fun `has no buttons until the album is known`() {
        show(album = null)

        composeTestRule.onNodeWithTag("detail_owned_toggle").assertDoesNotExist()
    }

    @Test
    fun `draws in dark mode too`() {
        show(darkTheme = true)

        composeTestRule.onNodeWithTag("detail_title").assertTextEquals("Trespass")
        composeTestRule.onAllNodesWithTag("track_row").assertCountEquals(3)
    }
}
