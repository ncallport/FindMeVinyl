package com.nickallport.findmevinyl.discography

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.nickallport.findmevinyl.genre.GenreNames
import com.nickallport.findmevinyl.genre.GenreOption
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Choosing a genre on the page of an artist or a group. */
@RunWith(RobolectricTestRunner::class)
class DiscographyRouteGenreTest {

    @get:Rule
    val rule = createComposeRule()

    private var stored: String? = null
    private var suggested: String? = null
    private val chosen = mutableListOf<String?>()
    private var options: suspend () -> List<GenreOption>? = {
        listOf(GenreOption("post-punk", 11), GenreOption("punk blues", 4))
    }

    private fun controls() = GenreControls(
        load = { stored },
        suggested = { suggested },
        options = { options() },
        set = { genre -> chosen.add(genre); stored = GenreNames.normalise(genre) }
    )

    private fun show(
        favourite: Boolean = true,
        inGroup: Boolean = false,
        isGroup: Boolean = false,
        withControls: Boolean = true
    ) {
        chosen.clear()
        rule.setContent {
            FindMeVinylTheme {
                DiscographyRoute(
                    artistId = if (isGroup) "group/1" else "cave", artistName = "Nick Cave",
                    load = { flowOf(DiscographyUiState.Success(emptyMap())) },
                    onToggleOwned = { _, _ -> },
                    loadIsFavourite = { favourite },
                    inGroup = inGroup, isGroup = isGroup,
                    genreControls = if (withControls) controls() else null
                )
            }
        }
        rule.waitForIdle()
    }

    private fun tap(tag: String) {
        rule.onNodeWithTag(tag).performClick()
        rule.waitForIdle()
    }

    // When the genre can be chosen.

    @Test
    fun `a favourite artist shows its genre line, saying none is chosen yet`() {
        show()

        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: none chosen")
        rule.onNodeWithTag("genre_change").assertTextEquals("Choose")
    }

    @Test
    fun `with a genre chosen the line shows it with a capital, and the button says Change`() {
        stored = "post-punk"
        show()

        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Post-punk")
        rule.onNodeWithTag("genre_change").assertTextEquals("Change")
    }

    @Test
    fun `an artist that is not saved has no genre line, since there is nowhere to keep it`() {
        show(favourite = false)

        rule.onNodeWithTag("genre_line").assertDoesNotExist()
    }

    @Test
    fun `an artist that is in a group has no genre line, since the group has the genre`() {
        show(inGroup = true)

        rule.onNodeWithTag("genre_line").assertDoesNotExist()
    }

    @Test
    fun `a group has a genre line`() {
        show(inGroup = true, isGroup = true)

        rule.onNodeWithTag("genre_line").assertExists()
    }

    @Test
    fun `with no way to choose there is no genre line`() {
        show(withControls = false)

        rule.onNodeWithTag("genre_line").assertDoesNotExist()
    }

    // Choosing.

    @Test
    fun `choosing opens the genres to pick from, the best voted first`() {
        show()

        tap("genre_change")

        rule.onNodeWithTag("genre_option_post-punk").assertTextEquals("Post-punk", "11 votes")
        rule.onNodeWithTag("genre_option_punk blues").assertExists()
    }

    @Test
    fun `tapping a genre keeps it, shows it and closes the list`() {
        show()
        tap("genre_change")

        tap("genre_option_post-punk")

        assertEquals(listOf<String?>("post-punk"), chosen)
        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Post-punk")
        rule.onNodeWithTag("genre_option_post-punk").assertDoesNotExist()
    }

    @Test
    fun `while the genres are being fetched a spinner shows`() {
        val gate = CompletableDeferred<List<GenreOption>?>()
        options = { gate.await() }
        show()

        tap("genre_change")
        rule.onNodeWithTag("genre_loading").assertExists()

        gate.complete(listOf(GenreOption("rock", 1)))
        rule.waitForIdle()

        rule.onNodeWithTag("genre_loading").assertDoesNotExist()
        rule.onNodeWithTag("genre_option_rock").assertExists()
    }

    @Test
    fun `when the genres cannot be had it says so, and one can still be typed`() {
        options = { null }
        show()

        tap("genre_change")

        rule.onNodeWithTag("genre_failed").assertExists()
        rule.onNodeWithTag("genre_custom").assertExists()
    }

    @Test
    fun `when MusicBrainz lists none it says so, and one can still be typed`() {
        options = { emptyList() }
        show()

        tap("genre_change")

        rule.onNodeWithTag("genre_none_listed").assertExists()
        rule.onNodeWithTag("genre_custom").assertExists()
    }

    @Test
    fun `a genre of the user's own can be typed and kept, shown as it was tidied`() {
        show()
        tap("genre_change")

        rule.onNodeWithTag("genre_custom").performTextReplacement("Gothic  Rock")
        tap("genre_custom_use")

        assertEquals(listOf<String?>("Gothic  Rock"), chosen)
        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Gothic rock")
    }

    @Test
    fun `the button to use a typed genre waits until something is typed`() {
        show()
        tap("genre_change")

        rule.onNodeWithTag("genre_custom_use").performClick()

        assertEquals(emptyList<String?>(), chosen)
    }

    @Test
    fun `a genre that was chosen can be taken away`() {
        stored = "post-punk"
        show()
        tap("genre_change")

        tap("genre_clear")

        assertEquals(listOf<String?>(null), chosen)
        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: none chosen")
    }

    @Test
    fun `there is nothing to take away when no genre is chosen`() {
        show()
        tap("genre_change")

        rule.onNodeWithTag("genre_clear").assertDoesNotExist()
    }

    @Test
    fun `cancelling keeps what was there`() {
        stored = "post-punk"
        show()
        tap("genre_change")

        tap("genre_cancel")

        assertEquals(emptyList<String?>(), chosen)
        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Post-punk")
        rule.onNodeWithTag("genre_option_post-punk").assertDoesNotExist()
    }

    @Test
    fun `the genre that is chosen is marked in the list, and the others show their votes`() {
        stored = "post-punk"
        show()

        tap("genre_change")

        rule.onNodeWithTag("genre_option_post-punk").assertTextEquals("Post-punk", "Chosen")
        rule.onNodeWithTag("genre_option_punk blues").assertTextEquals("Punk blues", "4 votes")
    }

    // The genre suggested from the votes on MusicBrainz.

    @Test
    fun `with only a suggestion the line shows it and says it is a suggestion, and the button says Change`() {
        suggested = "post-punk"
        show()

        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Post-punk (suggested)")
        rule.onNodeWithTag("genre_change").assertTextEquals("Change")
    }

    @Test
    fun `a genre the user chose wins over the suggestion, and is not called a suggestion`() {
        stored = "jazz"
        suggested = "post-punk"
        show()

        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Jazz")
    }

    @Test
    fun `in the list the suggestion is marked when nothing is chosen`() {
        suggested = "post-punk"
        show()

        tap("genre_change")

        rule.onNodeWithTag("genre_option_post-punk").assertTextEquals("Post-punk", "Suggested")
        rule.onNodeWithTag("genre_option_punk blues").assertTextEquals("Punk blues", "4 votes")
    }

    @Test
    fun `once a genre is chosen it is the one marked, and the suggestion shows its votes again`() {
        stored = "punk blues"
        suggested = "post-punk"
        show()

        tap("genre_change")

        rule.onNodeWithTag("genre_option_punk blues").assertTextEquals("Punk blues", "Chosen")
        rule.onNodeWithTag("genre_option_post-punk").assertTextEquals("Post-punk", "11 votes")
    }

    @Test
    fun `when there is a suggestion, taking the choice away goes back to it`() {
        stored = "jazz"
        suggested = "post-punk"
        show()
        tap("genre_change")

        rule.onNodeWithTag("genre_clear").assertTextEquals("Use the suggested genre")
        tap("genre_clear")

        assertEquals(listOf<String?>(null), chosen)
        rule.onNodeWithTag("genre_line").assertTextEquals("Genre: Post-punk (suggested)")
    }

    @Test
    fun `with no suggestion the button to take the choice away says Clear genre`() {
        stored = "jazz"
        show()
        tap("genre_change")

        rule.onNodeWithTag("genre_clear").assertTextEquals("Clear genre")
    }

    @Test
    fun `with only a suggestion there is no choice to take away`() {
        suggested = "post-punk"
        show()
        tap("genre_change")

        rule.onNodeWithTag("genre_clear").assertDoesNotExist()
    }
}
