package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.discography.AlbumDraft
import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.favourites.GroupMember
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The form for an album MusicBrainz does not list. */
@RunWith(RobolectricTestRunner::class)
class AddAlbumScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val saved = mutableListOf<AlbumDraft>()
    private var cancelled = 0
    private var deleted = 0
    private var settingsOpened = 0

    private fun show(initial: AlbumDraft? = null, editing: Boolean = false, showHiddenNote: Boolean = false) {
        saved.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AddAlbumScreen(
                    artistName = "Big Country", editing = editing, initial = initial, showHiddenNote = showHiddenNote,
                    onSave = { saved.add(it) }, onCancel = { cancelled++ }, onDelete = { deleted++ }, onOpenSettings = { settingsOpened++ }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun type(tag: String, text: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performTextInput(text)
        composeTestRule.waitForIdle()
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `saving needs a title`() {
        show()

        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().assertIsNotEnabled()
        type("add_album_title", "Live in a Pub")
        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `a blank title does not count`() {
        show()

        type("add_album_title", "   ")

        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `the year is optional, and a wrong one says so and stops saving`() {
        show()
        type("add_album_title", "Live in a Pub")

        type("add_album_year", "83")

        composeTestRule.onNodeWithTag("add_album_year_error").performScrollTo().assertTextContains("four digits", substring = true)
        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().assertIsNotEnabled()

        composeTestRule.onNodeWithTag("add_album_year").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("add_album_year_error").assertDoesNotExist()
        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `it starts on a live album, not a bootleg`() {
        show()

        composeTestRule.onNodeWithTag("add_album_kind_LIVE_ALBUM").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("add_album_bootleg").performScrollTo().assertIsOff()
    }

    @Test
    fun `saving hands over what was typed and chosen`() {
        show()

        type("add_album_title", "  Live in a Pub ")
        type("add_album_year", "1983")
        tap("add_album_kind_OTHER")
        tap("add_album_bootleg")
        tap("add_album_save")

        assertEquals(listOf(AlbumDraft("  Live in a Pub ", "1983", DiscographyCategory.OTHER, bootleg = true)), saved)
    }

    @Test
    fun `there is a kind to choose for each category`() {
        show()

        listOf("STUDIO_ALBUM", "LIVE_ALBUM", "COMPILATION", "EP", "SINGLE", "OTHER").forEach {
            composeTestRule.onNodeWithTag("add_album_kind_$it").performScrollTo().assertExists()
        }
    }

    @Test
    fun `the name of the artist is on the page, and the page is for adding`() {
        show()

        composeTestRule.onNodeWithTag("add_album_artist").assertTextContains("Big Country")
        composeTestRule.onNodeWithTag("add_album_title_text").assertTextContains("Add an album")
        composeTestRule.onNodeWithTag("add_album_delete").assertDoesNotExist()
    }

    @Test
    fun `cancelling saves nothing`() {
        show()
        type("add_album_title", "Live in a Pub")

        tap("add_album_cancel")

        assertEquals(1, cancelled)
        assertEquals(emptyList<AlbumDraft>(), saved)
    }

    @Test
    fun `a note says to check the album is not a hidden bootleg, when bootlegs are hidden`() {
        show(showHiddenNote = true)

        composeTestRule.onNodeWithTag("add_album_hidden_note").performScrollTo().assertTextContains("already listed", substring = true)
        tap("add_album_open_settings")

        assertEquals(1, settingsOpened)
    }

    @Test
    fun `there is no such note when bootlegs are not hidden`() {
        show(showHiddenNote = false)

        composeTestRule.onNodeWithTag("add_album_hidden_note").assertDoesNotExist()
    }

    // Changing an album added earlier.

    private val earlier = AlbumDraft("Live in a Pub", "1983", DiscographyCategory.LIVE_ALBUM, bootleg = true)

    @Test
    fun `changing an album shows what it has, and says it is a change`() {
        show(initial = earlier, editing = true)

        composeTestRule.onNodeWithTag("add_album_title").performScrollTo().assertTextContains("Live in a Pub")
        composeTestRule.onNodeWithTag("add_album_year").performScrollTo().assertTextContains("1983")
        composeTestRule.onNodeWithTag("add_album_bootleg").performScrollTo().assertIsOn()
        composeTestRule.onNodeWithTag("add_album_title_text").assertTextContains("Edit album")
        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().assertTextContains("Save changes")
    }

    @Test
    fun `changing and saving hands over the new values`() {
        show(initial = earlier, editing = true)

        composeTestRule.onNodeWithTag("add_album_title").performScrollTo().performTextClearance()
        type("add_album_title", "Live at the Pub")
        tap("add_album_bootleg")
        tap("add_album_save")

        assertEquals(listOf(AlbumDraft("Live at the Pub", "1983", DiscographyCategory.LIVE_ALBUM, bootleg = false)), saved)
    }

    @Test
    fun `an album can be deleted, after being asked`() {
        show(initial = earlier, editing = true)

        tap("add_album_delete")
        composeTestRule.onNodeWithTag("add_album_delete_confirm").assertExists()
        assertEquals(0, deleted)

        composeTestRule.onNodeWithTag("add_album_delete_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, deleted)
    }

    @Test
    fun `keeping the album closes the question and deletes nothing`() {
        show(initial = earlier, editing = true)

        tap("add_album_delete")
        composeTestRule.onNodeWithTag("add_album_delete_cancel").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_album_delete_confirm").assertDoesNotExist()
        assertEquals(0, deleted)
    }

    // A description and a tracklist.

    private fun addTrack(title: String) {
        type("add_album_track_input", title)
        tap("add_album_track_add")
    }

    @Test
    fun `there is an optional description box`() {
        show()

        type("add_album_title", "Live in a Pub")
        type("add_album_description", "BBC Radio 1, 1983")
        tap("add_album_save")

        assertEquals("BBC Radio 1, 1983", saved.single().description)
    }

    @Test
    fun `tracks are added one at a time, and are listed in order, numbered`() {
        show()

        addTrack("Harvest Home")
        addTrack("Fields of Fire")

        composeTestRule.onNodeWithTag("add_album_track_0").performScrollTo().assertTextContains("1. Harvest Home")
        composeTestRule.onNodeWithTag("add_album_track_1").performScrollTo().assertTextContains("2. Fields of Fire")
        composeTestRule.onNodeWithTag("add_album_track_2").assertDoesNotExist()
    }

    @Test
    fun `the box for the next track is emptied after one is added`() {
        show()

        addTrack("Harvest Home")

        composeTestRule.onNodeWithTag("add_album_track_add").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `a blank track cannot be added`() {
        show()

        composeTestRule.onNodeWithTag("add_album_track_add").performScrollTo().assertIsNotEnabled()
        type("add_album_track_input", "   ")
        composeTestRule.onNodeWithTag("add_album_track_add").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `a track can be removed`() {
        show()
        addTrack("One"); addTrack("Two"); addTrack("Three")

        tap("add_album_track_remove_1")

        composeTestRule.onNodeWithTag("add_album_track_0").performScrollTo().assertTextContains("1. One")
        composeTestRule.onNodeWithTag("add_album_track_1").performScrollTo().assertTextContains("2. Three")
        composeTestRule.onNodeWithTag("add_album_track_2").assertDoesNotExist()
    }

    @Test
    fun `a track can be moved up and down, and the first cannot go up nor the last down`() {
        show()
        addTrack("One"); addTrack("Two"); addTrack("Three")

        composeTestRule.onNodeWithTag("add_album_track_up_0").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("add_album_track_down_2").performScrollTo().assertIsNotEnabled()

        tap("add_album_track_up_2")
        composeTestRule.onNodeWithTag("add_album_track_1").performScrollTo().assertTextContains("2. Three")
        tap("add_album_track_down_0")
        composeTestRule.onNodeWithTag("add_album_track_0").performScrollTo().assertTextContains("1. Three")
        composeTestRule.onNodeWithTag("add_album_track_1").performScrollTo().assertTextContains("2. One")
    }

    @Test
    fun `saving hands over the tracks in order`() {
        show()
        type("add_album_title", "Live in a Pub")
        addTrack("One"); addTrack("Two")
        tap("add_album_track_up_1")

        tap("add_album_save")

        assertEquals(listOf("Two", "One"), saved.single().tracks)
    }

    @Test
    fun `changing an album shows its description and tracks`() {
        show(initial = earlier.copy(description = "BBC", tracks = listOf("One", "Two")), editing = true)

        composeTestRule.onNodeWithTag("add_album_description").performScrollTo().assertTextContains("BBC")
        composeTestRule.onNodeWithTag("add_album_track_1").performScrollTo().assertTextContains("2. Two")
    }

    @Test
    fun `the keyboard's Done adds the track, and a blank one is not added`() {
        show()

        composeTestRule.onNodeWithTag("add_album_track_input").performScrollTo().performImeAction()
        composeTestRule.onNodeWithTag("add_album_track_0").assertDoesNotExist()

        type("add_album_track_input", "Harvest Home")
        composeTestRule.onNodeWithTag("add_album_track_input").performImeAction()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_album_track_0").performScrollTo().assertTextContains("1. Harvest Home")
    }

    // Which artist of a group the album belongs to.

    private val members = listOf(GroupMember("cave", "Nick Cave"), GroupMember("seeds", "The Bad Seeds"), GroupMember("party", "The Birthday Party"))

    private fun showGroup(initial: AlbumDraft? = null, editing: Boolean = false, members: List<GroupMember> = this.members) {
        saved.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AddAlbumScreen(
                    artistName = "Nick Cave, all of it", editing = editing, initial = initial, showHiddenNote = false,
                    onSave = { saved.add(it) }, onCancel = {}, members = members
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `a group lists its artists to choose from, the first chosen to begin with`() {
        showGroup()

        composeTestRule.onNodeWithTag("add_album_member_cave").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("add_album_member_seeds").performScrollTo().assertExists()
        composeTestRule.onNodeWithTag("add_album_member_party").performScrollTo().assertExists()
    }

    @Test
    fun `the artist chosen goes with the draft`() {
        showGroup()

        type("add_album_title", "Prayers on Fire")
        tap("add_album_member_party")
        composeTestRule.onNodeWithTag("add_album_member_party").assertIsSelected()
        tap("add_album_save")

        assertEquals("party", saved.single().memberId)
    }

    @Test
    fun `changing an album starts on the artist it is filed under`() {
        showGroup(initial = AlbumDraft("Henry's Dream", "1992", DiscographyCategory.STUDIO_ALBUM, false, memberId = "seeds"), editing = true)

        composeTestRule.onNodeWithTag("add_album_member_seeds").performScrollTo().assertIsSelected()
        tap("add_album_save")

        assertEquals("seeds", saved.single().memberId)
    }

    @Test
    fun `an artist who is not a group has no choice and no member in the draft`() {
        show()

        composeTestRule.onNodeWithTag("add_album_member_cave").assertDoesNotExist()
        type("add_album_title", "Live in a Pub")
        tap("add_album_save")

        assertEquals(null, saved.single().memberId)
    }
}
