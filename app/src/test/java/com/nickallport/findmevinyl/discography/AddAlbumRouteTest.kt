package com.nickallport.findmevinyl.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddAlbumRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val earlier = AlbumDraft("Live in a Pub", "1983", DiscographyCategory.LIVE_ALBUM, bootleg = true)
    private val saves = mutableListOf<AlbumDraft>()
    private val done = mutableListOf<Pair<AlbumDraft, String>>()
    private var deletes = 0
    private var deletedDone = 0

    private fun show(
        editingAlbumId: String? = null,
        loadDraft: suspend (String) -> AlbumDraft? = { earlier },
        save: suspend (AlbumDraft) -> String? = { saves.add(it); "custom/new" }
    ) {
        saves.clear(); done.clear(); deletes = 0; deletedDone = 0
        composeTestRule.setContent {
            FindMeVinylTheme {
                AddAlbumRoute(
                    artistName = "Big Country", editingAlbumId = editingAlbumId, loadDraft = loadDraft, save = save,
                    delete = { deletes++ }, showHiddenNote = false, onOpenSettings = {},
                    onSaved = { draft, id -> done.add(draft to id) }, onDeleted = { deletedDone++ }, onCancel = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `adding hands the draft over, and tells the page when it is saved`() {
        show()

        composeTestRule.onNodeWithTag("add_album_title").performScrollTo().performTextInput("Live in a Pub")
        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, saves.size)
        assertEquals("Live in a Pub", saves.single().title)
        assertEquals(saves.map { it to "custom/new" }, done)
    }

    @Test
    fun `when it cannot be saved the page says so and stays`() {
        show(save = { null })

        composeTestRule.onNodeWithTag("add_album_title").performScrollTo().performTextInput("Live in a Pub")
        composeTestRule.onNodeWithTag("add_album_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_album_error").performScrollTo().assertTextContains("save", substring = true)
        assertEquals(emptyList<Pair<AlbumDraft, String>>(), done)
    }

    @Test
    fun `changing an album waits for what it has, then shows it`() {
        val gate = CompletableDeferred<AlbumDraft?>()
        show(editingAlbumId = "custom/1", loadDraft = { gate.await() })

        composeTestRule.onNodeWithTag("loading_indicator").assertExists()
        composeTestRule.onNodeWithTag("add_album_title").assertDoesNotExist()

        gate.complete(earlier)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_album_title").performScrollTo().assertTextContains("Live in a Pub")
    }

    @Test
    fun `an album that is not there shows nothing to change`() {
        show(editingAlbumId = "custom/gone", loadDraft = { null })

        composeTestRule.onNodeWithTag("add_album_missing").assertExists()
        composeTestRule.onNodeWithTag("add_album_title").assertDoesNotExist()
    }

    @Test
    fun `deleting asks first, then removes the album and tells the page`() {
        show(editingAlbumId = "custom/1")

        composeTestRule.onNodeWithTag("add_album_delete").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("add_album_delete_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, deletes)
        assertEquals(1, deletedDone)
    }
}
