package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the album title`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `shows an Owned label when the album is owned`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = true, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithText("Owned").assertExists()
    }

    @Test
    fun `does not show an Owned label when the album is not owned`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithText("Owned").assertDoesNotExist()
    }

    @Test
    fun `tapping the row opens the album and does not change owned`() {
        var opened = false
        var ownedToggled = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = { ownedToggled = true },
                onOpen = { opened = true }
            )
        }

        composeTestRule.onNodeWithTag("album_row").performClick()

        assertTrue(opened)
        assertFalse(ownedToggled)
    }

    @Test
    fun `an album that is not owned has an Owned pill that toggles owned without opening the album`() {
        var opened = false
        var ownedToggled = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = { ownedToggled = true },
                onOpen = { opened = true }
            )
        }

        composeTestRule.onNodeWithTag("owned_toggle").assertTextEquals("+ Owned")
        composeTestRule.onNodeWithTag("owned_toggle").performClick()

        assertTrue(ownedToggled)
        assertFalse(opened)
    }

    @Test
    fun `an owned album shows the Owned pill, which takes the ownership off`() {
        var ownedToggled = false
        var opened = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = true, wanted = false),
                onToggleOwned = { ownedToggled = true },
                onOpen = { opened = true }
            )
        }

        composeTestRule.onNodeWithTag("owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("owned_toggle").performClick()

        assertTrue(ownedToggled)
        assertFalse(opened)
    }

    @Test
    fun `shows a Wanted label when the album is wanted`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = true),
                onToggleOwned = {}
            )
        }
        composeTestRule.onNodeWithText("Wanted").assertExists()
    }

    @Test
    fun `does not show a Wanted label when the album is not wanted`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }
        composeTestRule.onNodeWithText("Wanted").assertDoesNotExist()
    }

    @Test
    fun `tapping the Want label invokes the wanted callback without triggering the owned callback`() {
        var ownedToggled = false
        var wantedToggled = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = { ownedToggled = true },
                onToggleWanted = { wantedToggled = true }
            )
        }

        composeTestRule.onNodeWithTag("wanted_toggle").performClick()

        assertTrue(wantedToggled)
        assertFalse(ownedToggled)
    }

    @Test
    fun `offers the Want toggle when the album is not owned`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithTag("wanted_toggle").assertExists()
    }

    @Test
    fun `shows the placeholder cover when no cover loader is given`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_placeholder", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `shows the cover that the loader returns for the album`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "abc", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {},
                loadCover = { id ->
                    asked.add(id)
                    Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_image", useUnmergedTree = true).assertExists()
        assertEquals(listOf("abc"), asked)
    }

    @Test
    fun `tapping the cover opens the album like tapping the rest of the row`() {
        var opened = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {},
                onOpen = { opened = true }
            )
        }

        composeTestRule.onNodeWithTag("cover_placeholder", useUnmergedTree = true).performClick()

        assertTrue(opened)
    }

    @Test
    fun `shows the formats the album has stored`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(
                    id = "1", title = "Foxtrot", owned = false, wanted = false,
                    formats = listOf("LP", "CD")
                ),
                onToggleOwned = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true)
            .assertTextEquals("LP · CD")
    }

    @Test
    fun `looks up the formats of an album that has none stored`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "abc", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {},
                loadFormats = { id -> asked.add(id); listOf("MC") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true).assertTextEquals("MC")
        assertEquals(listOf("abc"), asked)
    }

    @Test
    fun `a wanted album has no Want pill, because there is nothing more to want`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = true),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithTag("wanted_toggle").assertDoesNotExist()
        composeTestRule.onNodeWithText("+ Want").assertDoesNotExist()
    }

    @Test
    fun `a wanted album can still be marked as owned from its row`() {
        var ownedToggled = false

        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = true),
                onToggleOwned = { ownedToggled = true }
            )
        }

        composeTestRule.onNodeWithTag("owned_toggle").assertTextEquals("+ Owned")
        composeTestRule.onNodeWithTag("owned_toggle").performClick()

        assertTrue(ownedToggled)
    }

    @Test
    fun `an album that is neither owned nor wanted still offers both pills`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = false, wanted = false),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithTag("owned_toggle").assertExists()
        composeTestRule.onNodeWithTag("wanted_toggle").assertExists()
    }

    @Test
    fun `an album that is both owned and wanted says so, and stays the owned colour`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Foxtrot", owned = true, wanted = true),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithTag("album_wanted_label", useUnmergedTree = true).assertTextEquals("Wanted")
        composeTestRule.onNodeWithTag("owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("wanted_toggle").assertDoesNotExist()
    }

    @Test
    fun `an owned album that is not wanted offers the Want pill`() {
        var wantedToggled = false
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Flesh + Blood", owned = true, wanted = false),
                onToggleOwned = {},
                onToggleWanted = { wantedToggled = true }
            )
        }

        composeTestRule.onNodeWithTag("album_wanted_label", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("wanted_toggle").performClick()

        assertTrue(wantedToggled)
    }

    @Test
    fun `an album wanted on a format says which`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(id = "1", title = "Desire", owned = true, wanted = true, wantFormat = "LP"),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithTag("album_wanted_label", useUnmergedTree = true).assertTextEquals("Wanted on LP")
    }

    // The copies the user owns, one to a line.

    @Test
    fun `each copy is on its own line, as format, a colon and the description`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(
                    id = "1", title = "The Crossing", owned = true, wanted = false, userFormats = listOf("LP"),
                    copies = listOf(AlbumCopy("LP", "Green cover"), AlbumCopy("LP", "Blue cover, torn spine"), AlbumCopy("CD"))
                ),
                onToggleOwned = {}
            )
        }

        composeTestRule.onNodeWithTag("album_copy_0", useUnmergedTree = true).assertTextEquals("LP: Green cover")
        composeTestRule.onNodeWithTag("album_copy_1", useUnmergedTree = true).assertTextEquals("LP: Blue cover, torn spine")
        composeTestRule.onNodeWithTag("album_copy_2", useUnmergedTree = true).assertTextEquals("CD")
    }

    @Test
    fun `an album with no copies has no copy lines`() {
        composeTestRule.setContent {
            AlbumRow(album = AlbumUiModel(id = "1", title = "The Crossing", owned = true, wanted = false), onToggleOwned = {})
        }

        composeTestRule.onNodeWithTag("album_copy_0", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `the copies come under the formats, and the want under the copies`() {
        composeTestRule.setContent {
            AlbumRow(
                album = AlbumUiModel(
                    id = "1", title = "The Crossing", owned = true, wanted = true, wantFormat = "CD",
                    formats = listOf("LP"), copies = listOf(AlbumCopy("LP", "Green cover"))
                ),
                onToggleOwned = {}
            )
        }

        val formats = composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        val copy = composeTestRule.onNodeWithTag("album_copy_0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        val wanted = composeTestRule.onNodeWithTag("album_wanted_label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue(formats < copy)
        assertTrue(copy < wanted)
    }
}
