package com.nickallport.findmevinyl.barcode

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BarcodeRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val match = BarcodeMatch("5012985305020", "roxy", "Roxy Music", "rg1", "Avalon", listOf("CD"))
    private val looked = mutableListOf<String>()
    private val adds = mutableListOf<Triple<BarcodeMatch, String, Boolean>>()
    private val opened = mutableListOf<BarcodeMatch>()

    private fun show(
        lookUp: suspend (String) -> BarcodeLookup = { BarcodeLookup.Found(listOf(match)) },
        status: BarcodeStatus = BarcodeStatus(),
        addOwned: suspend (BarcodeMatch, String, Boolean) -> BarcodeAdd = { m, f, w -> adds.add(Triple(m, f, w)); BarcodeAdd.Added(m.releaseGroupId) }
    ) {
        looked.clear(); adds.clear(); opened.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                BarcodeRoute(
                    lookUp = { looked.add(it); lookUp(it) },
                    statusOf = { status },
                    addOwned = addOwned,
                    onOpenAlbum = { opened.add(it) }
                )
            }
        }
    }

    private fun lookUpNumber(number: String = "5012985305020") {
        composeTestRule.onNodeWithTag("barcode_input").performTextInput(number)
        composeTestRule.onNodeWithTag("barcode_lookup").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `opens with a hint and nothing looked up`() {
        show()

        composeTestRule.onNodeWithTag("barcode_hint").assertExists()
        assertEquals(emptyList<String>(), looked)
    }

    @Test
    fun `the look up button waits for a number`() {
        show()

        composeTestRule.onNodeWithTag("barcode_lookup").assertIsNotEnabled()
    }

    @Test
    fun `shows the album found with the artist`() {
        show()

        lookUpNumber()

        assertEquals(listOf("5012985305020"), looked)
        composeTestRule.onNodeWithTag("barcode_match_title_0").assertTextEquals("Roxy Music - Avalon")
    }

    @Test
    fun `says when the artist is new`() {
        show(status = BarcodeStatus())

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_artist_status_0").assertTextContains("new artist", substring = true, ignoreCase = true)
    }

    @Test
    fun `says when the artist is a favourite, or in a group`() {
        show(status = BarcodeStatus(artistIsFavourite = true))
        lookUpNumber()
        composeTestRule.onNodeWithTag("barcode_artist_status_0").assertTextEquals("Already in your favourites.")
    }

    @Test
    fun `names the group when the artist is in one`() {
        show(status = BarcodeStatus(artistIsFavourite = true, groupName = "Roxy and Ferry"))

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_artist_status_0").assertTextEquals("Part of your group Roxy and Ferry.")
    }

    @Test
    fun `says when the album is owned with its copies, and offers another copy`() {
        show(status = BarcodeStatus(artistIsFavourite = true, albumSaved = true, owned = true, copyFormats = listOf("LP", "CD")))

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_album_status_0").assertTextEquals("You already own this album: LP, CD.")
        composeTestRule.onNodeWithTag("barcode_add_0").assertTextEquals("Add another copy")
    }

    @Test
    fun `says when the album is wanted, with the format`() {
        show(status = BarcodeStatus(albumSaved = true, wanted = true, wantFormat = "LP"))

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_album_status_0").assertTextEquals("On your Wants list, wanted on LP.")
        composeTestRule.onNodeWithTag("barcode_take_off_want_0").assertDoesNotExist()
    }

    @Test
    fun `a want on any format offers to take it off, on by default`() {
        show(status = BarcodeStatus(albumSaved = true, wanted = true))

        lookUpNumber()
        composeTestRule.onNodeWithTag("barcode_add_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(Triple(match, "CD", true)), adds)
    }

    @Test
    fun `the format starts as the one of the releases found and can be changed`() {
        show()

        lookUpNumber()
        composeTestRule.onNodeWithTag("barcode_add_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(Triple(match, "CD", false)), adds)
    }

    @Test
    fun `adding with another format passes that format`() {
        show()

        lookUpNumber()
        composeTestRule.onNodeWithTag("barcode_format_0_LP").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("barcode_add_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(Triple(match, "LP", false)), adds)
    }

    @Test
    fun `after adding, says so and offers to open the album`() {
        show()

        lookUpNumber()
        composeTestRule.onNodeWithTag("barcode_add_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barcode_added_0").assertExists()
        composeTestRule.onNodeWithTag("barcode_add_0").assertDoesNotExist()
        composeTestRule.onNodeWithTag("barcode_open_0").performScrollTo().performClick()
        assertEquals(listOf(match), opened)
    }

    @Test
    fun `a failure to add is shown and the button stays`() {
        show(addOwned = { _, _, _ -> BarcodeAdd.Failed("MusicBrainz is busy. Try again in a minute.") })

        lookUpNumber()
        composeTestRule.onNodeWithTag("barcode_add_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barcode_error").assertTextEquals("MusicBrainz is busy. Try again in a minute.")
        composeTestRule.onNodeWithTag("barcode_add_0").assertExists()
    }

    @Test
    fun `a number that is not a barcode says so`() {
        show(lookUp = { BarcodeLookup.BadNumber })

        lookUpNumber("123")

        composeTestRule.onNodeWithTag("barcode_message").assertTextContains("not a barcode", substring = true)
    }

    @Test
    fun `an unknown barcode says MusicBrainz has no album`() {
        show(lookUp = { BarcodeLookup.NotFound })

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_message").assertTextContains("no album with that barcode", substring = true)
    }

    @Test
    fun `a failed look up shows the reason`() {
        show(lookUp = { BarcodeLookup.Failed("MusicBrainz is busy. Try again in a minute.") })

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_message").assertTextEquals("MusicBrainz is busy. Try again in a minute.")
    }
}
