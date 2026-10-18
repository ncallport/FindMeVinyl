package com.nickallport.findmevinyl.ui.artist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.favourites.ArtistGroup
import com.nickallport.findmevinyl.favourites.GroupMember
import com.nickallport.findmevinyl.favourites.GroupResult
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The page where artists are joined into a group, or a group is changed. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2400dp")
class JoinArtistsRouteTest {

    @get:Rule
    val rule = createComposeRule()

    private val cave = GroupMember("cave", "Nick Cave")
    private val seeds = GroupMember("seeds", "Nick Cave & The Bad Seeds")
    private val grinderman = GroupMember("grinderman", "Grinderman")

    private val favourites = listOf(
        FavouriteArtistUiModel("dury", "Ian Dury"),
        FavouriteArtistUiModel("group/9", "Ian Dury and the lot", isGroup = true, memberIds = listOf("a", "b")),
        FavouriteArtistUiModel("cave", "Nick Cave")
    )

    private var existing: ArtistGroup? = null
    private var saveResult: (GroupMember, String, List<GroupMember>) -> GroupResult = { _, name, members ->
        GroupResult.Saved(ArtistGroup("group/1", name, members))
    }
    private val saved = mutableListOf<Triple<String?, String, List<GroupMember>>>()
    private val ungrouped = mutableListOf<String>()
    private var done = 0
    private var cancelled = 0

    private fun show(remote: suspend (String) -> List<ArtistSuggestion> = { emptyList() }) {
        saved.clear(); ungrouped.clear(); done = 0; cancelled = 0
        rule.setContent {
            FindMeVinylTheme {
                JoinArtistsRoute(
                    startArtist = cave,
                    loadGroup = { existing },
                    loadFavourites = { favourites },
                    suggestArtists = remote,
                    save = { groupId, name, members ->
                        saved.add(Triple(groupId, name, members))
                        saveResult(cave, name, members)
                    },
                    ungroup = { ungrouped.add(it) },
                    onDone = { done++ },
                    onCancel = { cancelled++ }
                )
            }
        }
        rule.waitForIdle()
    }

    private fun type(text: String) {
        rule.onNodeWithTag("join_search").performTextReplacement(text)
    }

    private fun pause(millis: Long) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    private fun tap(tag: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun addSeeds() {
        type("bad")
        pause(1000)
        tap("join_suggestion_0")
    }

    private val seedsSuggestion = ArtistSuggestion("seeds", "Nick Cave & The Bad Seeds", "Band from AU", favourite = false)

    @Test
    fun `it starts with the artist it was opened for, named after it, and cannot be saved with one`() {
        show()

        rule.onNodeWithTag("join_title").assertTextEquals("Join artists")
        rule.onNodeWithTag("join_name").assertTextContains("Nick Cave")
        rule.onNodeWithTag("join_member_cave").assertExists()
        rule.onNodeWithTag("join_save").assertIsNotEnabled()
        rule.onNodeWithTag("join_ungroup").assertDoesNotExist()
    }

    @Test
    fun `typing offers favourites that match at once, but not groups or the artists already chosen`() {
        show()

        type("ian")
        pause(100)

        rule.onNodeWithTag("join_suggestion_0").assertTextContains("Ian Dury", substring = true)
        rule.onNodeWithTag("join_suggestion_1").assertDoesNotExist()

        type("cave")
        pause(100)

        rule.onNodeWithTag("join_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `artists from MusicBrainz are offered when typing pauses, and tapping one adds it`() {
        show(remote = { listOf(seedsSuggestion) })

        type("bad")
        pause(1000)
        rule.onNodeWithTag("join_suggestion_0").assertTextContains("Nick Cave & The Bad Seeds", substring = true)
        tap("join_suggestion_0")

        rule.onNodeWithTag("join_member_seeds").assertExists()
        rule.onNodeWithTag("join_suggestion_0").assertDoesNotExist()
        rule.onNodeWithTag("join_search").assertTextContains("")
        rule.onNodeWithTag("join_save").assertIsEnabled()
    }

    @Test
    fun `an artist already chosen is not offered again`() {
        show(remote = { listOf(seedsSuggestion) })
        addSeeds()

        type("bad")
        pause(1000)

        rule.onNodeWithTag("join_suggestion_0").assertDoesNotExist()
    }

    @Test
    fun `saving hands over the name the user typed and the members, and then finishes`() {
        show(remote = { listOf(seedsSuggestion) })
        addSeeds()
        rule.onNodeWithTag("join_name").performScrollTo().performTextReplacement("Nick Cave, everything")

        tap("join_save")

        assertEquals(listOf(Triple<String?, String, List<GroupMember>>(null, "Nick Cave, everything", listOf(cave, seeds))), saved)
        assertEquals(1, done)
    }

    @Test
    fun `a member can be taken out again, and made first`() {
        show(remote = { listOf(seedsSuggestion) })
        addSeeds()

        tap("join_member_first_seeds")
        tap("join_save")
        assertEquals(listOf(seeds, cave), saved.single().third)
    }

    @Test
    fun `taking a member out leaves one, which cannot be saved`() {
        show(remote = { listOf(seedsSuggestion) })
        addSeeds()

        tap("join_member_remove_seeds")

        rule.onNodeWithTag("join_member_seeds").assertDoesNotExist()
        rule.onNodeWithTag("join_save").assertIsNotEnabled()
    }

    @Test
    fun `when an artist is already in another group the page says so and stays`() {
        show(remote = { listOf(seedsSuggestion) })
        saveResult = { _, _, _ -> GroupResult.AlreadyInGroup("Nick Cave & The Bad Seeds", "Bad Seeds Family") }
        addSeeds()

        tap("join_save")

        rule.onNodeWithTag("join_error").assertTextEquals("Nick Cave & The Bad Seeds is already in the group Bad Seeds Family")
        assertEquals(0, done)
    }

    @Test
    fun `cancelling leaves without saving`() {
        show()

        tap("join_cancel")

        assertEquals(1, cancelled)
        assertEquals(emptyList<Triple<String?, String, List<GroupMember>>>(), saved)
    }

    @Test
    fun `for an artist already in a group the page edits that group, with its name and members`() {
        existing = ArtistGroup("group/1", "Nick Cave, everything", listOf(cave, seeds))
        show()

        rule.onNodeWithTag("join_title").assertTextEquals("Edit group")
        rule.onNodeWithTag("join_name").assertTextContains("Nick Cave, everything")
        rule.onNodeWithTag("join_member_cave").assertExists()
        rule.onNodeWithTag("join_member_seeds").assertExists()
        rule.onNodeWithTag("join_ungroup").assertExists()
        rule.onNodeWithTag("join_save").assertIsEnabled()
    }

    @Test
    fun `saving an edited group hands over the group id`() {
        existing = ArtistGroup("group/1", "Nick Cave, everything", listOf(cave, seeds))
        show(remote = { listOf(ArtistSuggestion("grinderman", "Grinderman", "Band", favourite = false)) })
        type("grind")
        pause(1000)
        tap("join_suggestion_0")

        tap("join_save")

        assertEquals(listOf(Triple<String?, String, List<GroupMember>>("group/1", "Nick Cave, everything", listOf(cave, seeds, grinderman))), saved)
        assertEquals(1, done)
    }

    @Test
    fun `splitting up a group asks for it to be ungrouped and finishes`() {
        existing = ArtistGroup("group/1", "Nick Cave, everything", listOf(cave, seeds))
        show()

        tap("join_ungroup")

        assertEquals(listOf("group/1"), ungrouped)
        assertEquals(1, done)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the name and the members survive a rotation`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            FindMeVinylTheme {
                JoinArtistsRoute(
                    startArtist = cave, loadGroup = { null }, loadFavourites = { favourites },
                    suggestArtists = { listOf(seedsSuggestion) }, save = { _, n, m -> GroupResult.Saved(ArtistGroup("g", n, m)) },
                    ungroup = {}, onDone = {}, onCancel = {}
                )
            }
        }
        waitForIdle()
        onNodeWithTag("join_search").performTextReplacement("bad")
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithTag("join_suggestion_0").performScrollTo().performClick()
        onNodeWithTag("join_name").performScrollTo().performTextReplacement("My name")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithTag("join_member_seeds").assertExists()
        onNodeWithTag("join_name").assertTextContains("My name")
    }
}
