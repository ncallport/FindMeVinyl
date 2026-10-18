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

/** The My copies part of an album page: the copies the user owns, each with a format and a note. */
@RunWith(RobolectricTestRunner::class)
class MyCopySectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val owned = AlbumUiModel("1", "The Crossing", owned = true, wanted = false, formats = listOf("CD"))
    private val withCopies = owned.copy(
        userFormats = listOf("LP"),
        copies = listOf(AlbumCopy("LP", "green cover"), AlbumCopy("LP", "blue cover")),
        note = "Mercury, 1983"
    )
    private val noDetails = AlbumDetails(null, null, null, null, null, null, emptyList(), emptyList())
    private val saved = mutableListOf<Pair<List<AlbumCopy>, String>>()

    private fun show(album: AlbumUiModel?) {
        saved.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailScreen(
                    albumId = "1", title = "The Crossing", artistName = "Big Country", album = album,
                    state = AlbumDetailState.Loaded(noDetails), onRetry = {}, onToggleOwned = {},
                    onToggleWanted = {}, loadCover = { null },
                    onSaveCopy = { copies, note -> saved.add(copies to note) }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun type(tag: String, text: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `an album you do not own has no My copies section`() {
        show(owned.copy(owned = false))

        composeTestRule.onNodeWithTag("my_copy_section").assertDoesNotExist()
    }

    @Test
    fun `there is none before the album is known`() {
        show(null)

        composeTestRule.onNodeWithTag("my_copy_section").assertDoesNotExist()
    }

    @Test
    fun `an owned album with no copies invites you to add some`() {
        show(owned)

        composeTestRule.onNodeWithTag("my_copy_section").assertExists()
        composeTestRule.onNodeWithTag("my_copy_edit").performScrollTo().assertTextContains("Add my copies")
        composeTestRule.onNodeWithTag("my_copy_summary").assertDoesNotExist()
    }

    @Test
    fun `the summary lists each copy with its note, and the note about the album`() {
        show(withCopies)

        composeTestRule.onNodeWithTag("my_copy_summary").performScrollTo()
            .assertTextContains("LP: green cover", substring = true)
            .assertTextContains("LP: blue cover", substring = true)
            .assertTextContains("Mercury, 1983", substring = true)
        composeTestRule.onNodeWithTag("my_copy_edit").performScrollTo().assertTextContains("Edit my copies")
    }

    @Test
    fun `a copy with no note is listed by its format alone`() {
        show(owned.copy(copies = listOf(AlbumCopy("CD"))))

        composeTestRule.onNodeWithTag("my_copy_summary").performScrollTo().assertTextContains("CD")
    }

    @Test
    fun `editing shows each copy with its format chosen and its note`() {
        show(withCopies)

        tap("my_copy_edit")

        composeTestRule.onNodeWithTag("my_copy_0_format_LP").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("my_copy_0_format_CD").performScrollTo().assertIsNotSelected()
        composeTestRule.onNodeWithTag("my_copy_0_note").performScrollTo().assertTextContains("green cover")
        composeTestRule.onNodeWithTag("my_copy_1_format_LP").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("my_copy_1_note").performScrollTo().assertTextContains("blue cover")
        composeTestRule.onNodeWithTag("my_copy_2_note").assertDoesNotExist()
        composeTestRule.onNodeWithTag("my_copy_note").performScrollTo().assertTextContains("Mercury, 1983")
    }

    @Test
    fun `a copy is added with a format and a note, and saving hands over the copies in order`() {
        show(owned)
        tap("my_copy_edit")

        tap("my_copy_add")
        tap("my_copy_0_format_CD")
        type("my_copy_0_note", "part of Mum's collection")
        tap("my_copy_save")

        assertEquals(listOf(listOf(AlbumCopy("CD", "part of Mum's collection")) to ""), saved)
        composeTestRule.onNodeWithTag("my_copy_save").assertDoesNotExist()
    }

    @Test
    fun `two copies on the same format can be added`() {
        show(owned)
        tap("my_copy_edit")

        tap("my_copy_add")
        type("my_copy_0_note", "green cover")
        tap("my_copy_add")
        type("my_copy_1_note", "blue cover")
        tap("my_copy_save")

        assertEquals(listOf(AlbumCopy("LP", "green cover"), AlbumCopy("LP", "blue cover")), saved.single().first)
    }

    @Test
    fun `a copy can be removed again`() {
        show(withCopies)
        tap("my_copy_edit")

        tap("my_copy_0_remove")
        tap("my_copy_save")

        assertEquals(listOf(AlbumCopy("LP", "blue cover")), saved.single().first)
    }

    @Test
    fun `the note about the album is handed over with the copies`() {
        show(withCopies)
        tap("my_copy_edit")

        type("my_copy_note", "Mercury, 1983, Scotland")
        tap("my_copy_save")

        assertEquals("Mercury, 1983, Scotland", saved.single().second)
    }

    @Test
    fun `cancelling closes the editor and saves nothing`() {
        show(withCopies)
        tap("my_copy_edit")
        tap("my_copy_0_remove")

        tap("my_copy_cancel")

        assertEquals(emptyList<Pair<List<AlbumCopy>, String>>(), saved)
        composeTestRule.onNodeWithTag("my_copy_save").assertDoesNotExist()
        composeTestRule.onNodeWithTag("my_copy_summary").assertExists()
    }

    @Test
    fun `a note longer than the limit is cut as it is typed`() {
        show(owned)
        tap("my_copy_edit")
        tap("my_copy_add")

        type("my_copy_0_note", "x".repeat(600))
        type("my_copy_note", "y".repeat(600))
        tap("my_copy_save")

        assertEquals(500, saved.single().first.single().note!!.length)
        assertEquals(500, saved.single().second.length)
    }

    @Test
    fun `no more than twenty copies can be added`() {
        show(owned)
        tap("my_copy_edit")

        repeat(20) { tap("my_copy_add") }

        composeTestRule.onNodeWithTag("my_copy_add").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the editor and what was typed in it survive a rotation`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            FindMeVinylTheme {
                AlbumDetailScreen(
                    albumId = "1", title = "The Crossing", artistName = "Big Country", album = owned,
                    state = AlbumDetailState.Loaded(noDetails), onRetry = {}, onToggleOwned = {},
                    onToggleWanted = {}, loadCover = { null }
                )
            }
        }
        waitForIdle()
        onNodeWithTag("my_copy_edit").performScrollTo().performClick()
        onNodeWithTag("my_copy_add").performScrollTo().performClick()
        onNodeWithTag("my_copy_0_format_CD").performScrollTo().performClick()
        onNodeWithTag("my_copy_0_note").performScrollTo().performTextReplacement("Portuguese")
        onNodeWithTag("my_copy_note").performScrollTo().performTextReplacement("Album note")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithTag("my_copy_0_format_CD").performScrollTo().assertIsSelected()
        onNodeWithTag("my_copy_0_note").performScrollTo().assertTextContains("Portuguese")
        onNodeWithTag("my_copy_note").performScrollTo().assertTextContains("Album note")
    }
}
