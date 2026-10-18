package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nickallport.findmevinyl.discography.AlbumDetails
import com.nickallport.findmevinyl.discography.Disc
import com.nickallport.findmevinyl.discography.Pressing
import com.nickallport.findmevinyl.discography.Rating
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
        onSetWantFormat: (String?) -> Unit = {},
        onSelectPressing: (String) -> Unit = {},
        loadCover: suspend (String) -> Bitmap? = { null }
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme(darkTheme = darkTheme) {
                AlbumDetailScreen(
                    albumId = "1", title = "Trespass", artistName = "Genesis", album = album, state = state,
                    onRetry = onRetry, onToggleOwned = onToggleOwned, onToggleWanted = onToggleWanted, onSetWantFormat = onSetWantFormat,
                    onSelectPressing = onSelectPressing, loadCover = loadCover
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
    fun `an owned album says so and still offers the Want button`() {
        show(album = notOwned.copy(owned = true))

        composeTestRule.onNodeWithTag("detail_owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("+ Want")
    }

    @Test
    fun `an album both owned and wanted shows both, and Wanted takes the want off`() {
        var wanted = 0
        show(album = notOwned.copy(owned = true, wanted = true), onToggleWanted = { wanted++ })

        composeTestRule.onNodeWithTag("detail_owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("Wanted")
        composeTestRule.onNodeWithTag("detail_want_toggle").performScrollTo().performClick()
        assertEquals(1, wanted)
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

    // The rating and the pressings.

    private val pressings = listOf(
        Pressing("r1", "1970-10", "GB", "Charisma", "CAS 1020", "12\" Vinyl"),
        Pressing("r2", "1972", "DE", "Charisma Records Ltd.", "6369 905", "12\" Vinyl"),
        Pressing("r3", null, null, null, null, null)
    )

    private val withPressings = details.copy(pressings = pressings, pressingId = "r1", rating = Rating(4.3, 27))

    @Test
    fun `shows the rating`() {
        show(state = AlbumDetailState.Loaded(withPressings))

        composeTestRule.onNodeWithTag("detail_rating").assertTextEquals("Rating 4.3 / 5 (27 votes)")
    }

    @Test
    fun `shows no rating when there is none`() {
        show(state = AlbumDetailState.Loaded(withPressings.copy(rating = null)))

        composeTestRule.onNodeWithTag("detail_rating").assertDoesNotExist()
    }

    @Test
    fun `lists the pressings with their date, country, label, catalogue number and format`() {
        show(state = AlbumDetailState.Loaded(withPressings))

        composeTestRule.onAllNodesWithTag("pressing_row", useUnmergedTree = true).assertCountEquals(3)
        composeTestRule.onNodeWithTag("pressing_title_r2", useUnmergedTree = true).assertTextEquals("1972, Germany")
        composeTestRule.onNodeWithTag("pressing_detail_r2", useUnmergedTree = true)
            .assertTextEquals("Charisma Records Ltd., 6369 905, 12\" Vinyl")
        composeTestRule.onNodeWithTag("pressing_title_r1", useUnmergedTree = true).assertTextEquals("1970-10, United Kingdom")
    }

    @Test
    fun `a pressing with nothing known says so`() {
        show(state = AlbumDetailState.Loaded(withPressings))

        composeTestRule.onNodeWithTag("pressing_title_r3", useUnmergedTree = true).assertTextEquals("Date unknown")
        composeTestRule.onNodeWithTag("pressing_detail_r3", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `an album with only one pressing has no list of pressings`() {
        show(state = AlbumDetailState.Loaded(withPressings.copy(pressings = pressings.take(1))))

        composeTestRule.onAllNodesWithTag("pressing_row", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onNodeWithText("Pressings").assertDoesNotExist()
    }

    @Test
    fun `marks the pressing that is shown`() {
        show(state = AlbumDetailState.Loaded(withPressings))

        composeTestRule.onNodeWithTag("pressing_shown_r1", useUnmergedTree = true).assertTextEquals("Showing")
        composeTestRule.onNodeWithTag("pressing_shown_r2", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `tapping another pressing asks for it, and tapping the one shown does nothing`() {
        val asked = mutableListOf<String>()
        show(state = AlbumDetailState.Loaded(withPressings), onSelectPressing = { asked.add(it) })

        composeTestRule.onNodeWithTag("pressing_row_r1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("pressing_row_r2").performScrollTo().performClick()

        assertEquals(listOf("r2"), asked)
    }

    @Test
    fun `shows a spinner on the pressing being loaded, and no more taps start another`() {
        val asked = mutableListOf<String>()
        show(
            state = AlbumDetailState.Loaded(withPressings, switchingTo = "r2"),
            onSelectPressing = { asked.add(it) }
        )

        composeTestRule.onNodeWithTag("pressing_loading_r2", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("pressing_row_r3").performScrollTo().performClick()

        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `says so when another pressing could not be loaded`() {
        show(state = AlbumDetailState.Loaded(withPressings, switchFailed = true))

        composeTestRule.onNodeWithTag("pressing_failed").assertExists()
        composeTestRule.onNodeWithText("Couldn't load that pressing").assertExists()
    }

    @Test
    fun `says nothing about a failed pressing when none failed`() {
        show(state = AlbumDetailState.Loaded(withPressings))

        composeTestRule.onNodeWithTag("pressing_failed").assertDoesNotExist()
    }

    @Test
    fun `the pressings draw in dark mode too`() {
        show(state = AlbumDetailState.Loaded(withPressings), darkTheme = true)

        composeTestRule.onAllNodesWithTag("pressing_row", useUnmergedTree = true).assertCountEquals(3)
    }

    @Test
    fun `a wanted album offers the formats to want it on, with Any chosen to begin with`() {
        show(album = notOwned.copy(owned = true, wanted = true))

        composeTestRule.onNodeWithTag("want_format_any").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("want_format_LP").assertIsNotSelected()
        composeTestRule.onNodeWithTag("want_format_CD").assertExists()
    }

    @Test
    fun `the format the album is wanted on is the one selected`() {
        show(album = notOwned.copy(wanted = true, wantFormat = "LP"))

        composeTestRule.onNodeWithTag("want_format_LP").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("want_format_any").assertIsNotSelected()
    }

    @Test
    fun `choosing a format, or Any, tells the caller`() {
        val chosen = mutableListOf<String?>()
        show(album = notOwned.copy(wanted = true, wantFormat = "LP"), onSetWantFormat = { chosen.add(it) })

        composeTestRule.onNodeWithTag("want_format_CD").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("want_format_any").performScrollTo().performClick()

        assertEquals(listOf<String?>("CD", null), chosen)
    }

    @Test
    fun `an album that is not wanted offers no format to want it on`() {
        show(album = notOwned)

        composeTestRule.onNodeWithTag("want_format_any").assertDoesNotExist()
    }
}
