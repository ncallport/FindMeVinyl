package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.discography.Grouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Owned and Wanted sections at the top of a discography, in either order. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2400dp")
class DiscographyScreenGroupingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val albums = mapOf(
        DiscographyCategory.STUDIO_ALBUM to listOf(
            AlbumUiModel("1", "Studio Owned", owned = true, wanted = false),
            AlbumUiModel("2", "Studio Wanted", owned = false, wanted = true),
            AlbumUiModel("3", "Studio Other", owned = false, wanted = false)
        ),
        DiscographyCategory.LIVE_ALBUM to listOf(
            AlbumUiModel("4", "Live Wanted", owned = false, wanted = true),
            AlbumUiModel("5", "Live Other", owned = false, wanted = false)
        ),
        DiscographyCategory.EP to listOf(
            AlbumUiModel("6", "Only Wanted EP", owned = false, wanted = true)
        )
    )

    private fun show(
        grouping: Grouping,
        data: Map<DiscographyCategory, List<AlbumUiModel>> = albums,
        onWantedFirstChange: (Boolean) -> Unit = {},
        onOpenAlbum: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            DiscographyScreen(
                groupedAlbums = data, onToggleOwned = {}, grouping = grouping,
                onWantedFirstChange = onWantedFirstChange, onOpenAlbum = onOpenAlbum
            )
        }
    }

    private fun top(text: String) = composeTestRule.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y
    private fun tagTop(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `with wanted grouped, the wanted albums are in a section at the top, in the order of the categories`() {
        show(Grouping(wanted = true))

        assertTrue(tagTop("wanted_section_header") < top("Studio Wanted"))
        assertTrue(top("Studio Wanted") < top("Live Wanted"))
        assertTrue(top("Live Wanted") < top("Only Wanted EP"))
        assertTrue(top("Only Wanted EP") < top("Studio Albums"))
    }

    @Test
    fun `a wanted album is in the wanted section and not in its category as well`() {
        show(Grouping(wanted = true))

        composeTestRule.onAllNodesWithText("Studio Wanted").assertCountEquals(1)
        composeTestRule.onNodeWithText("Studio Other").assertExists()
    }

    @Test
    fun `a category whose albums are all wanted disappears while wanted are grouped`() {
        show(Grouping(wanted = true))

        composeTestRule.onNodeWithText("EPs").assertDoesNotExist()
        composeTestRule.onNodeWithText("Studio Albums").assertExists()
    }

    @Test
    fun `with both grouped, owned comes first and wanted below it, then the categories`() {
        show(Grouping(owned = true, wanted = true))

        assertTrue(tagTop("owned_section_header") < top("Studio Owned"))
        assertTrue(top("Studio Owned") < tagTop("wanted_section_header"))
        assertTrue(tagTop("wanted_section_header") < top("Studio Wanted"))
        assertTrue(top("Only Wanted EP") < top("Studio Albums"))
    }

    @Test
    fun `with both grouped and wanted put first, wanted comes before owned`() {
        show(Grouping(owned = true, wanted = true, wantedFirst = true))

        assertTrue(tagTop("wanted_section_header") < top("Studio Wanted"))
        assertTrue(top("Only Wanted EP") < tagTop("owned_section_header"))
        assertTrue(tagTop("owned_section_header") < top("Studio Owned"))
        assertTrue(top("Studio Owned") < top("Studio Albums"))
    }

    @Test
    fun `with only wanted grouped, the owned albums stay in their categories`() {
        show(Grouping(wanted = true))

        composeTestRule.onNodeWithTag("owned_section_header").assertDoesNotExist()
        assertTrue(top("Studio Albums") < top("Studio Owned"))
    }

    @Test
    fun `the section that is not first has an arrow to put it first, and the first has none`() {
        show(Grouping(owned = true, wanted = true))

        composeTestRule.onNodeWithTag("group_first_wanted").assertExists()
        composeTestRule.onNodeWithTag("group_first_owned").assertDoesNotExist()
    }

    @Test
    fun `with wanted first it is the owned section that has the arrow`() {
        show(Grouping(owned = true, wanted = true, wantedFirst = true))

        composeTestRule.onNodeWithTag("group_first_owned").assertExists()
        composeTestRule.onNodeWithTag("group_first_wanted").assertDoesNotExist()
    }

    @Test
    fun `tapping the arrow on wanted puts wanted first, and on owned puts owned first`() {
        val changes = mutableListOf<Boolean>()
        show(Grouping(owned = true, wanted = true), onWantedFirstChange = { changes.add(it) })
        composeTestRule.onNodeWithTag("group_first_wanted").performClick()

        assertEquals(listOf(true), changes)
    }

    @Test
    fun `tapping the arrow on owned says wanted is no longer first`() {
        val changes = mutableListOf<Boolean>()
        show(Grouping(owned = true, wanted = true, wantedFirst = true), onWantedFirstChange = { changes.add(it) })
        composeTestRule.onNodeWithTag("group_first_owned").performClick()

        assertEquals(listOf(false), changes)
    }

    @Test
    fun `there is no arrow when only one group is on`() {
        show(Grouping(wanted = true))

        composeTestRule.onNodeWithTag("group_first_wanted").assertDoesNotExist()
        composeTestRule.onNodeWithTag("group_first_owned").assertDoesNotExist()
    }

    @Test
    fun `there is no arrow when one of the two sections has nothing in it`() {
        val onlyOwned = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "Studio Owned", owned = true, wanted = false),
                AlbumUiModel("3", "Studio Other", owned = false, wanted = false)
            )
        )
        show(Grouping(owned = true, wanted = true), data = onlyOwned)

        composeTestRule.onNodeWithTag("wanted_section_header").assertDoesNotExist()
        composeTestRule.onNodeWithTag("group_first_wanted").assertDoesNotExist()
        composeTestRule.onNodeWithTag("group_first_owned").assertDoesNotExist()
    }

    @Test
    fun `an album both owned and wanted is only in the owned section when both are grouped`() {
        val both = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Both", owned = true, wanted = true))
        )
        show(Grouping(owned = true, wanted = true), data = both)

        composeTestRule.onAllNodesWithText("Both").assertCountEquals(1)
        composeTestRule.onNodeWithTag("wanted_section_header").assertDoesNotExist()
    }

    @Test
    fun `an album in the wanted section can still be opened`() {
        var opened: String? = null
        show(Grouping(wanted = true), onOpenAlbum = { opened = it })

        composeTestRule.onNodeWithText("Studio Wanted").performClick()

        assertEquals("2", opened)
    }

    @Test
    fun `with only wanted grouped, an album both owned and wanted goes in the wanted section`() {
        val both = mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                AlbumUiModel("1", "Both", owned = true, wanted = true),
                AlbumUiModel("2", "Plain", owned = false, wanted = false)
            )
        )
        show(Grouping(wanted = true), data = both)

        composeTestRule.onNodeWithTag("wanted_section_header").assertExists()
        composeTestRule.onAllNodesWithText("Both").assertCountEquals(1)
    }
}
