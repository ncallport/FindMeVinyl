package com.nickallport.findmevinyl.ui.owned

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.owned.OwnedAlbumUiModel
import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Give Me Five is a dialog, so this uses the default small window: an AlertDialog never gets idle in a tall one. */
@RunWith(RobolectricTestRunner::class)
class GiveMeFiveDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun albums(n: Int) = (1..n).map {
        OwnedAlbumUiModel("id$it", "artist$it", "Artist $it", "Album $it", null, listOf(AlbumCopy("LP")))
    }

    private fun show(list: List<OwnedAlbumUiModel>, seed: Int = 1, onOpenAlbum: (OwnedAlbumUiModel) -> Unit = {}) {
        composeTestRule.setContent {
            FindMeVinylTheme { OwnedListScreen(owned = list, onShare = {}, onOpenAlbum = onOpenAlbum, random = Random(seed)) }
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    /** The ids of the albums in the dialog, in id order. */
    private fun shown(n: Int) = (1..n).filter {
        composeTestRule.onAllNodes(hasTestTag("five_id$it"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }.map { "id$it" }

    @Test
    fun `is not shown until the button is tapped`() {
        show(albums(20))

        composeTestRule.onNodeWithTag("five_dialog").assertDoesNotExist()
    }

    @Test
    fun `shows five of the owned albums`() {
        show(albums(20))

        tap("give_me_five")

        composeTestRule.onNodeWithTag("five_dialog").assertExists()
        val picked = shown(20)
        assertEquals(5, picked.size)
    }

    @Test
    fun `with fewer than five owned it shows them all, and offers no refresh`() {
        show(albums(3))

        tap("give_me_five")

        assertEquals(3, shown(3).size)
        composeTestRule.onNodeWithTag("five_refresh").assertDoesNotExist()
    }

    @Test
    fun `Refresh shows five different ones`() {
        show(albums(30))
        tap("give_me_five")
        val first = shown(30)

        tap("five_refresh")
        val second = shown(30)

        assertEquals(5, second.size)
        assertEquals(emptySet<String>(), first.toSet().intersect(second.toSet()))
    }

    @Test
    fun `Refresh can be tapped again and again`() {
        show(albums(30))
        tap("give_me_five")

        repeat(5) {
            val before = shown(30)
            tap("five_refresh")
            assertEquals(5, shown(30).size)
            assertNotEquals(before, shown(30))
        }
    }

    @Test
    fun `Close puts the dialog away and the list is as it was`() {
        show(albums(20))
        tap("give_me_five")

        tap("five_close")

        composeTestRule.onNodeWithTag("five_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("owned_title").assertExists()
    }

    @Test
    fun `tapping one of the five opens that album`() {
        var opened: OwnedAlbumUiModel? = null
        show(albums(20), onOpenAlbum = { opened = it })
        tap("give_me_five")
        val first = shown(20).first()

        composeTestRule.onNodeWithTag("five_$first", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        assertEquals(first, opened?.albumId)
    }

    @Test
    fun `each of the five says its artist and album and its copies`() {
        show(albums(6))
        tap("give_me_five")
        val id = shown(6).first()
        val n = id.removePrefix("id")

        composeTestRule.onNodeWithTag("five_name_$id", useUnmergedTree = true)
            .assertTextEquals("Artist $n - Album $n")
        composeTestRule.onNodeWithTag("five_copies_$id", useUnmergedTree = true).assertTextEquals("LP")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the five are still the same after the phone is turned`() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            FindMeVinylTheme { OwnedListScreen(owned = albums(20), onShare = {}, onOpenAlbum = {}, random = Random(4)) }
        }
        waitForIdle()
        onNodeWithTag("give_me_five").performClick()
        waitForIdle()
        val before = (1..20).filter { onAllNodes(hasTestTag("five_id$it"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

        restoration.emulateSaveAndRestore()
        waitForIdle()

        val after = (1..20).filter { onAllNodes(hasTestTag("five_id$it"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(before.isNotEmpty())
        assertEquals(before, after)
    }
}
