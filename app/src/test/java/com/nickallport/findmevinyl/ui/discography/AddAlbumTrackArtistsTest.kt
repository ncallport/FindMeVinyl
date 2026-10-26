package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.discography.AlbumDraft
import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Renaming a track and choosing its artist on the form for an album added by hand. Dialogs, so the default small window. */
@RunWith(RobolectricTestRunner::class)
class AddAlbumTrackArtistsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val saved = mutableListOf<AlbumDraft>()
    private val ub40 = PickedArtist("ub40", "UB40")

    private val earlier = AlbumDraft("Now 4", "1984", DiscographyCategory.COMPILATION, bootleg = false)

    private fun show(initial: AlbumDraft? = earlier, canChoose: Boolean = true) {
        saved.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AddAlbumScreen(
                    artistName = "Various Artists", editing = true, initial = initial, showHiddenNote = false,
                    onSave = { saved.add(it) }, onCancel = {},
                    suggestTrackArtists = if (canChoose) { text ->
                        listOf(ArtistSuggestion("ub40", "UB40", "Band from GB", favourite = false)).filter { text.isNotBlank() }
                    } else null
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun tapInDialog(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    private fun chooseUb40(track: Int) {
        tap("add_album_track_artist_$track")
        composeTestRule.onNodeWithTag("track_artist_search").performTextInput("ub4")
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("track_artist_suggestion_0").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `a track can be given an artist, which goes in the draft beside it`() {
        show(earlier.copy(tracks = listOf("Red Red Wine", "Rebel Yell")))

        chooseUb40(0)
        tap("add_album_save")

        assertEquals(listOf(ub40, null), saved.single().trackArtists)
        composeTestRule.onNodeWithTag("add_album_track_artist_0").performScrollTo().assertTextContains("UB40", substring = true)
    }

    @Test
    fun `a draft with no artists says so with an empty list`() {
        show(earlier.copy(tracks = listOf("One")))

        tap("add_album_save")

        assertEquals(emptyList<PickedArtist?>(), saved.single().trackArtists)
    }

    @Test
    fun `an artist stays with its track when the track moves, and goes when the track is removed`() {
        show(earlier.copy(tracks = listOf("One", "Two", "Three"), trackArtists = listOf(ub40, null, PickedArtist(null, "A friend"))))

        tap("add_album_track_down_0")
        tap("add_album_track_remove_2")
        tap("add_album_save")

        assertEquals(listOf("Two", "One"), saved.single().tracks)
        assertEquals(listOf(null, ub40), saved.single().trackArtists)
    }

    @Test
    fun `the artist chosen can be taken off again`() {
        show(earlier.copy(tracks = listOf("One"), trackArtists = listOf(ub40)))

        tap("add_album_track_artist_0")
        tapInDialog("track_artist_reset")
        tap("add_album_save")

        assertEquals(emptyList<PickedArtist?>(), saved.single().trackArtists)
    }

    @Test
    fun `a track can be renamed, and keeps its artist and place`() {
        show(earlier.copy(tracks = listOf("One", "Two"), trackArtists = listOf(null, ub40)))

        tap("add_album_track_rename_1")
        composeTestRule.onNodeWithTag("add_album_track_rename_input").performTextClearance()
        composeTestRule.onNodeWithTag("add_album_track_rename_input").performTextInput("  Red Red Wine ")
        tapInDialog("add_album_track_rename_save")
        tap("add_album_save")

        assertEquals(listOf("One", "Red Red Wine"), saved.single().tracks)
        assertEquals(listOf(null, ub40), saved.single().trackArtists)
    }

    @Test
    fun `a title cannot be renamed to nothing`() {
        show(earlier.copy(tracks = listOf("One")))

        tap("add_album_track_rename_0")
        composeTestRule.onNodeWithTag("add_album_track_rename_input").performTextClearance()

        composeTestRule.onNodeWithTag("add_album_track_rename_save").assertIsNotEnabled()
    }

    @Test
    fun `cancelling a rename leaves the title`() {
        show(earlier.copy(tracks = listOf("One")))

        tap("add_album_track_rename_0")
        composeTestRule.onNodeWithTag("add_album_track_rename_input").performTextInput("x")
        tapInDialog("add_album_track_rename_cancel")
        tap("add_album_save")

        assertEquals(listOf("One"), saved.single().tracks)
    }

    @Test
    fun `without a way to find artists a track offers none to choose`() {
        show(earlier.copy(tracks = listOf("One")), canChoose = false)

        composeTestRule.onNodeWithTag("add_album_track_artist_0").assertDoesNotExist()
    }
}
