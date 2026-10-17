package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nickallport.findmevinyl.discography.AlbumDetails
import com.nickallport.findmevinyl.discography.Pressing
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Choosing one of the album's MusicBrainz pressings as the user's own copy. */
@RunWith(RobolectricTestRunner::class)
class MyPressingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val us = Pressing("p1", "1962-03-19", "US", "Columbia", "CS 8579", "12\" Vinyl")
    private val portugal = Pressing("p2", "1963-01-01", "PT", "CBS", "BPG 62", "12\" Vinyl")

    private fun details(shown: String? = "p1", pressings: List<Pressing> = listOf(us, portugal)) = AlbumDetails(
        "1962-03-19", "US", "Columbia", "CS 8579", "1962-03-19", "12\" Vinyl", emptyList(), emptyList(),
        pressings = pressings, pressingId = shown
    )

    private val owned = AlbumUiModel("1", "Bob Dylan", owned = true, wanted = false)
    private val marked = mutableListOf<Pressing?>()

    private fun show(album: AlbumUiModel?, state: AlbumDetailState = AlbumDetailState.Loaded(details())) {
        marked.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailScreen(
                    albumId = "1", title = "Bob Dylan", artistName = "Bob Dylan", album = album, state = state,
                    onRetry = {}, onToggleOwned = {}, onToggleWanted = {}, loadCover = { null },
                    onMarkMyPressing = { marked.add(it) }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `an owned album can take the pressing shown as its own`() {
        show(owned)

        tap("my_pressing_mark")

        assertEquals(listOf<Pressing?>(us), marked)
    }

    @Test
    fun `an album you do not own has no such button`() {
        show(owned.copy(owned = false))

        composeTestRule.onNodeWithTag("my_pressing_mark").assertDoesNotExist()
    }

    @Test
    fun `there is no button while the details load, or when they could not be loaded`() {
        show(owned, AlbumDetailState.Loading)
        composeTestRule.onNodeWithTag("my_pressing_mark").assertDoesNotExist()
    }

    @Test
    fun `there is no button when the pressing shown is not one of those listed`() {
        show(owned, AlbumDetailState.Loaded(details(shown = "other")))

        composeTestRule.onNodeWithTag("my_pressing_mark").assertDoesNotExist()
    }

    @Test
    fun `the chosen pressing is described, and the button is gone while that one is shown`() {
        show(owned.copy(myPressingId = "p1", myPressingText = "1962-03-19, United States, Columbia, CS 8579"))

        composeTestRule.onNodeWithTag("my_pressing_text").performScrollTo()
            .assertTextContains("My pressing: 1962-03-19, United States, Columbia, CS 8579")
        composeTestRule.onNodeWithTag("my_pressing_mark").assertDoesNotExist()
    }

    @Test
    fun `while another pressing is shown, the button can move the choice to it`() {
        show(owned.copy(myPressingId = "p2", myPressingText = "1963-01-01, Portugal, CBS, BPG 62"))

        composeTestRule.onNodeWithTag("my_pressing_text").performScrollTo().assertTextContains("Portugal", substring = true)
        tap("my_pressing_mark")

        assertEquals(listOf<Pressing?>(us), marked)
    }

    @Test
    fun `the choice can be taken away`() {
        show(owned.copy(myPressingId = "p1", myPressingText = "1962-03-19, United States"))

        tap("my_pressing_clear")

        assertEquals(listOf<Pressing?>(null), marked)
    }

    @Test
    fun `there is no clear button when nothing is chosen`() {
        show(owned)

        composeTestRule.onNodeWithTag("my_pressing_clear").assertDoesNotExist()
        composeTestRule.onNodeWithTag("my_pressing_text").assertDoesNotExist()
    }

    @Test
    fun `the chosen pressing is marked Mine in the list of pressings, and the others are not`() {
        show(owned.copy(myPressingId = "p2", myPressingText = "text"))

        composeTestRule.onNodeWithTag("pressing_mine_p2", useUnmergedTree = true).assertTextContains("Mine")
        composeTestRule.onNodeWithTag("pressing_mine_p1", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a pressing is not marked Mine on an album with none chosen`() {
        show(owned)

        composeTestRule.onNodeWithTag("pressing_mine_p1", useUnmergedTree = true).assertDoesNotExist()
    }
}
