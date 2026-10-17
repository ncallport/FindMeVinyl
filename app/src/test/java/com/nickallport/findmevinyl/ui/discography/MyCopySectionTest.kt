package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.discography.AlbumDetails
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The My copy part of an album page: what the user says about their own copy. */
@RunWith(RobolectricTestRunner::class)
class MyCopySectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val owned = AlbumUiModel("1", "Olympia", owned = true, wanted = false, formats = listOf("CD"))
    private val withCopy = owned.copy(userFormats = listOf("LP"), note = "BMG vinyl, 2018")
    private val noDetails = AlbumDetails(null, null, null, null, null, null, emptyList(), emptyList())
    private val saved = mutableListOf<Pair<List<String>, String>>()

    private fun show(album: AlbumUiModel?) {
        saved.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailScreen(
                    albumId = "1", title = "Olympia", artistName = "Bryan Ferry", album = album,
                    state = AlbumDetailState.Loaded(noDetails), onRetry = {}, onToggleOwned = {},
                    onToggleWanted = {}, loadCover = { null },
                    onSaveCopy = { formats, note -> saved.add(formats to note) }
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
    fun `an album you do not own has no My copy section`() {
        show(owned.copy(owned = false))

        composeTestRule.onNodeWithTag("my_copy_section").assertDoesNotExist()
    }

    @Test
    fun `there is none before the album is known`() {
        show(null)

        composeTestRule.onNodeWithTag("my_copy_section").assertDoesNotExist()
    }

    @Test
    fun `an owned album with no details invites you to add some`() {
        show(owned)

        composeTestRule.onNodeWithTag("my_copy_section").assertExists()
        composeTestRule.onNodeWithTag("my_copy_edit").performScrollTo().assertTextContains("Add details of my copy")
        composeTestRule.onNodeWithTag("my_copy_summary").assertDoesNotExist()
    }

    @Test
    fun `an owned album with details shows them`() {
        show(withCopy)

        composeTestRule.onNodeWithTag("my_copy_summary").performScrollTo()
            .assertTextContains("LP", substring = true)
            .assertTextContains("BMG vinyl, 2018", substring = true)
        composeTestRule.onNodeWithTag("my_copy_edit").performScrollTo().assertTextContains("Edit my copy")
    }

    @Test
    fun `editing shows a chip for every format, with the ones already added chosen, and the note`() {
        show(withCopy)

        tap("my_copy_edit")

        composeTestRule.onNodeWithTag("my_copy_format_LP").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("my_copy_format_CD").performScrollTo().assertIsNotSelected()
        composeTestRule.onNodeWithTag("my_copy_format_7\"").assertExists()
        composeTestRule.onNodeWithTag("my_copy_note").performScrollTo().assertTextContains("BMG vinyl, 2018")
    }

    @Test
    fun `saving hands over the chosen formats and the note, and closes the editor`() {
        show(owned)
        tap("my_copy_edit")

        tap("my_copy_format_LP")
        tap("my_copy_format_CD")
        composeTestRule.onNodeWithTag("my_copy_note").performScrollTo().performTextReplacement("BMG vinyl")
        tap("my_copy_save")

        assertEquals(listOf(listOf("LP", "CD") to "BMG vinyl"), saved)
        composeTestRule.onNodeWithTag("my_copy_save").assertDoesNotExist()
    }

    @Test
    fun `a format can be taken off again`() {
        show(withCopy)
        tap("my_copy_edit")

        tap("my_copy_format_LP")
        tap("my_copy_save")

        assertEquals(listOf(emptyList<String>() to "BMG vinyl, 2018"), saved)
    }

    @Test
    fun `cancelling closes the editor and saves nothing`() {
        show(withCopy)
        tap("my_copy_edit")
        tap("my_copy_format_CD")

        tap("my_copy_cancel")

        assertEquals(emptyList<Pair<List<String>, String>>(), saved)
        composeTestRule.onNodeWithTag("my_copy_save").assertDoesNotExist()
        composeTestRule.onNodeWithTag("my_copy_summary").assertExists()
    }

    @Test
    fun `a note longer than the limit is cut as it is typed`() {
        show(owned)
        tap("my_copy_edit")

        composeTestRule.onNodeWithTag("my_copy_note").performScrollTo().performTextReplacement("x".repeat(600))
        tap("my_copy_save")

        assertEquals(500, saved.single().second.length)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the editor and what was typed in it survive a rotation`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            FindMeVinylTheme {
                AlbumDetailScreen(
                    albumId = "1", title = "Olympia", artistName = "Bryan Ferry", album = owned,
                    state = AlbumDetailState.Loaded(noDetails), onRetry = {}, onToggleOwned = {},
                    onToggleWanted = {}, loadCover = { null }
                )
            }
        }
        waitForIdle()
        onNodeWithTag("my_copy_edit").performScrollTo().performClick()
        onNodeWithTag("my_copy_format_LP").performScrollTo().performClick()
        onNodeWithTag("my_copy_note").performScrollTo().performTextReplacement("Portuguese")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithTag("my_copy_format_LP").performScrollTo().assertIsSelected()
        onNodeWithTag("my_copy_note").performScrollTo().assertTextContains("Portuguese")
    }
}
