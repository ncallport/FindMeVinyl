package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Suggestions of artists while typing in the Artists page's search field. */
@RunWith(RobolectricTestRunner::class)
class ArtistsScreenSuggestionsTest {

    @get:Rule
    val rule = createComposeRule()

    private val favourites = listOf(
        FavouriteArtistUiModel("1", "Joy Division"),
        FavouriteArtistUiModel("2", "New Order")
    )
    private val joyDivision = ArtistSuggestion("1", "Joy Division", "Band from GB", favourite = false)
    private val joy = ArtistSuggestion("9", "Joy", "Band from AT", favourite = false)

    private val asked = mutableListOf<String>()
    private val searched = mutableListOf<String>()
    private val selected = mutableListOf<FavouriteArtistUiModel>()

    private fun show(suggest: suspend (String) -> List<ArtistSuggestion> = { listOf(joyDivision, joy) }) {
        rule.setContent {
            FindMeVinylTheme {
                ArtistsScreen(
                    onSearch = { searched.add(it) },
                    loadFavourites = { favourites },
                    onArtistSelected = { selected.add(it) },
                    suggestArtists = { text -> asked.add(text); suggest(text) }
                )
            }
        }
        rule.waitForIdle()
    }

    private fun type(text: String) {
        rule.onNodeWithTag("search_field").performTextReplacement(text)
    }

    private fun pause(millis: Long) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    @Test
    fun `a favourite that matches is suggested at once, before anything is asked of MusicBrainz`() {
        show()

        type("joy")
        pause(100)

        rule.onNodeWithTag("artist_suggestion_0").assertTextContains("Joy Division", substring = true)
        rule.onNodeWithTag("artist_suggestion_0").assertTextContains("In your favourites", substring = true)
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `the others follow when typing pauses, without listing a favourite twice`() {
        show()

        type("joy")
        pause(1000)

        rule.onNodeWithTag("artist_suggestion_0").assertTextContains("Joy Division", substring = true)
        rule.onNodeWithTag("artist_suggestion_1").assertTextContains("Joy", substring = true)
        rule.onNodeWithTag("artist_suggestion_1").assertTextContains("Band from AT", substring = true)
        rule.onNodeWithTag("artist_suggestion_2").assertDoesNotExist()
    }

    @Test
    fun `nothing is asked while the typing goes on, only for the last text`() {
        show()

        type("Jo")
        pause(300)
        type("Joy")
        pause(300)
        type("Joy D")
        assertEquals(emptyList<String>(), asked)
        pause(1000)

        assertEquals(listOf("Joy D"), asked)
    }

    @Test
    fun `tapping a suggestion opens that artist`() {
        show()
        type("joy")
        pause(1000)

        rule.onNodeWithTag("artist_suggestion_1").performClick()
        rule.waitForIdle()

        assertEquals(listOf(FavouriteArtistUiModel("9", "Joy")), selected)
        rule.onNodeWithTag("artist_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `the keyboard search still searches for the text and hides the suggestions`() {
        show()
        type("joy")
        pause(1000)

        rule.onNodeWithTag("search_field").performImeAction()
        rule.waitForIdle()

        assertEquals(listOf("joy"), searched)
        rule.onNodeWithTag("artist_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `clearing the field clears the suggestions`() {
        show()
        type("joy")
        pause(1000)

        type("")
        pause(1000)

        rule.onNodeWithTag("artist_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `an answer for text that has been typed over is not shown`() {
        val slow = CompletableDeferred<List<ArtistSuggestion>>()
        show { text -> if (text == "zzz") slow.await() else emptyList() }

        type("zzz")
        pause(1000)
        type("zzzq")
        pause(1000)
        slow.complete(listOf(joy))
        rule.waitForIdle()

        rule.onNodeWithTag("artist_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `with nothing to suggest the favourites are shown as before`() {
        show { emptyList() }

        type("qqq")
        pause(1000)

        rule.onNodeWithTag("artist_suggestion_0").assertDoesNotExist()
        rule.onNodeWithTag("favourites_view_list").assertExists()
    }
}
