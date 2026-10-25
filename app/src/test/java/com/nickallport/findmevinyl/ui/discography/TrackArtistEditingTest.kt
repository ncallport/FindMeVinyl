package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.discography.AlbumDetails
import com.nickallport.findmevinyl.discography.Disc
import com.nickallport.findmevinyl.discography.Track
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Choosing the artist of a track on an album MusicBrainz lists. A dialog test, so the default small window. */
@RunWith(RobolectricTestRunner::class)
class TrackArtistEditingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val details = AlbumDetails(
        releaseDate = null, country = null, label = null, catalogNumber = null, firstReleaseDate = null, format = null,
        genres = emptyList(),
        discs = listOf(Disc(null, "CD", listOf(Track("1", "Easy Lover", null), Track("2", "Rebel Yell", null, "Billy Idol", listOf("bi")))))
    )

    private val picked = mutableListOf<List<Any?>>()

    private fun show(editable: Boolean = true) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailScreen(
                    albumId = "1", title = "Now", artistName = "Various Artists",
                    album = AlbumUiModel("1", "Now", owned = true, wanted = false),
                    state = AlbumDetailState.Loaded(details),
                    onRetry = {}, onToggleOwned = {}, onToggleWanted = {}, loadCover = { null },
                    suggestTrackArtists = if (editable) { text ->
                        listOf(ArtistSuggestion("ub40", "UB40", "Band from GB", favourite = false)).filter { text.isNotBlank() }
                    } else null,
                    onSetTrackArtist = { disc, track, title, artist -> picked.add(listOf(disc, track, title, artist)) }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun editTrack(index: Int) {
        composeTestRule.onNodeWithTag("track_artists_edit").performScrollTo().performClick()
        composeTestRule.onAllNodesWithTag("track_row")[index].performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun type(text: String) {
        composeTestRule.onNodeWithTag("track_artist_search").performTextInput(text)
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `without a way to find artists there is no edit button`() {
        show(editable = false)

        composeTestRule.onAllNodesWithTag("track_artists_edit").assertCountEqualsZero()
    }

    @Test
    fun `editing shows every track's artist and says where one is missing`() {
        show()
        composeTestRule.onNodeWithTag("track_artists_edit").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Tap to choose the artist").assertExists()
        composeTestRule.onNodeWithText("Billy Idol").assertExists()
    }

    @Test
    fun `choosing a suggested artist sets it for that track`() {
        show()
        editTrack(0)
        type("ub4")

        composeTestRule.onNodeWithTag("track_artist_suggestion_0").performClick()

        assertEquals(listOf(listOf(0, 0, "Easy Lover", PickedArtist("ub40", "UB40"))), picked)
    }

    @Test
    fun `a name typed by hand is set without an id`() {
        show()
        editTrack(0)
        type("A friend")

        composeTestRule.onNodeWithTag("track_artist_typed").performClick()

        assertEquals(listOf(listOf(0, 0, "Easy Lover", PickedArtist(null, "A friend"))), picked)
    }

    @Test
    fun `a track with an artist can have MusicBrainz's put back, and one without cannot`() {
        show()
        editTrack(0)
        assertTrue(composeTestRule.onAllNodesWithTag("track_artist_reset").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithTag("track_artist_cancel").performClick()
        composeTestRule.waitForIdle()
        assertTrue(picked.isEmpty())

        composeTestRule.onAllNodesWithTag("track_row")[1].performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("track_artist_reset").performClick()

        assertEquals(listOf(listOf(0, 1, "Rebel Yell", null)), picked)
        assertFalse(picked.isEmpty())
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEqualsZero() =
        assertEquals(0, fetchSemanticsNodes().size)
}
