package com.nickallport.findmevinyl.ui.owned

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.owned.OwnedAlbumUiModel
import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The complete Owned list. A lazy list only composes what fits, so this uses a tall window (the dialog is tested apart). */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class OwnedListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val albums = listOf(
        OwnedAlbumUiModel("1", "a", "Abba", "Arrival", "1976", listOf(AlbumCopy("LP", "Green cover"), AlbumCopy("CD"))),
        OwnedAlbumUiModel("2", "b", "Bob Dylan", "Desire", "1976", emptyList()),
        OwnedAlbumUiModel("3", "c", "Sleep", "Dopesmoker", null, listOf(AlbumCopy("LP")))
    )

    private fun show(
        list: List<OwnedAlbumUiModel> = albums,
        onShare: () -> Unit = {},
        onOpenAlbum: (OwnedAlbumUiModel) -> Unit = {}
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme { OwnedListScreen(owned = list, onShare = onShare, onOpenAlbum = onOpenAlbum) }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `the title says how many albums there are`() {
        show()

        composeTestRule.onNodeWithTag("owned_title").assertTextEquals("My Owned list (3 albums)")
    }

    @Test
    fun `the title says album for one`() {
        show(albums.take(1))

        composeTestRule.onNodeWithTag("owned_title").assertTextEquals("My Owned list (1 album)")
    }

    @Test
    fun `each album is a line with its artist and title`() {
        show()

        composeTestRule.onNodeWithTag("owned_name_1", useUnmergedTree = true).assertTextEquals("Abba - Arrival")
        composeTestRule.onNodeWithTag("owned_name_3", useUnmergedTree = true).assertTextEquals("Sleep - Dopesmoker")
    }

    @Test
    fun `each copy is a line under the album, with its note`() {
        show()

        composeTestRule.onNodeWithTag("owned_copy_1_0", useUnmergedTree = true).assertTextEquals("LP: Green cover")
        composeTestRule.onNodeWithTag("owned_copy_1_1", useUnmergedTree = true).assertTextEquals("CD")
        composeTestRule.onNodeWithTag("owned_copy_2_0", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `tapping an album opens it`() {
        var opened: OwnedAlbumUiModel? = null
        show(onOpenAlbum = { opened = it })

        composeTestRule.onNodeWithTag("owned_2").performClick()

        assertEquals("2", opened?.albumId)
        assertEquals("b", opened?.artistId)
    }

    @Test
    fun `Share asks for the list to be shared`() {
        var shared = 0
        show(onShare = { shared++ })

        composeTestRule.onNodeWithTag("owned_share").performClick()

        assertEquals(1, shared)
    }

    @Test
    fun `with nothing owned it says so and offers neither Share nor Give Me Five`() {
        show(emptyList())

        composeTestRule.onNodeWithTag("owned_empty").assertExists()
        composeTestRule.onNodeWithTag("owned_share").assertDoesNotExist()
        composeTestRule.onNodeWithTag("give_me_five").assertDoesNotExist()
    }

    @Test
    fun `there is a Give Me Five button when something is owned`() {
        show()

        composeTestRule.onNodeWithTag("give_me_five").assertExists()
    }
}
