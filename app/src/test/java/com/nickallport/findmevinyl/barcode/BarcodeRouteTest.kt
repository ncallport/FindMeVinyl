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
    private val opened = mutableListOf<Pair<BarcodeMatch, String>>()

    private fun show(
        lookUp: suspend (String) -> BarcodeLookup = { BarcodeLookup.Found(listOf(match)) },
        status: BarcodeStatus = BarcodeStatus(),
        addOwned: suspend (BarcodeMatch, String, Boolean) -> BarcodeAdd = { m, f, w -> adds.add(Triple(m, f, w)); BarcodeAdd.Added(m.releaseGroupId) },
        scan: (suspend () -> ScanResult)? = null
    ) {
        looked.clear(); adds.clear(); opened.clear()
        composeTestRule.setContent {
            FindMeVinylTheme {
                BarcodeRoute(
                    lookUp = { looked.add(it); lookUp(it) },
                    statusOf = { status },
                    addOwned = addOwned,
                    onOpenAlbum = { match, albumId -> opened.add(match to albumId) },
                    scan = scan
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
        assertEquals(listOf(match to "rg1"), opened)
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

    // Scanning with the camera.

    @Test
    fun `has no scan button without a camera to use`() {
        show()

        composeTestRule.onNodeWithTag("barcode_scan").assertDoesNotExist()
    }

    @Test
    fun `a scanned number is put in the field and looked up`() {
        show(scan = { ScanResult.Number("5012985305020") })

        composeTestRule.onNodeWithTag("barcode_scan").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("5012985305020"), looked)
        composeTestRule.onNodeWithTag("barcode_input").assertTextContains("5012985305020")
        composeTestRule.onNodeWithTag("barcode_match_title_0").assertTextEquals("Roxy Music - Avalon")
    }

    @Test
    fun `a photo that cannot be read says so and looks nothing up`() {
        show(scan = { ScanResult.Unreadable })

        composeTestRule.onNodeWithTag("barcode_scan").performClick()
        composeTestRule.waitForIdle()

        assertEquals(emptyList<String>(), looked)
        composeTestRule.onNodeWithTag("barcode_message").assertTextContains("could not read", substring = true, ignoreCase = true)
    }

    @Test
    fun `backing out of the camera changes nothing`() {
        show(scan = { ScanResult.Cancelled })

        composeTestRule.onNodeWithTag("barcode_scan").performClick()
        composeTestRule.waitForIdle()

        assertEquals(emptyList<String>(), looked)
        composeTestRule.onNodeWithTag("barcode_hint").assertExists()
    }

    @Test
    fun `a phone with no camera app says so`() {
        show(scan = { ScanResult.NoCamera })

        composeTestRule.onNodeWithTag("barcode_scan").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barcode_message").assertTextContains("camera", substring = true, ignoreCase = true)
    }

    // Found on Discogs.

    private val discogsMatch = BarcodeMatch("198029779813", "ants", "Adam & The Ants", "rg-singles", "The Singles", listOf("LP"), fromDiscogs = true)
    private val byHandMatch = discogsMatch.copy(releaseGroupId = "byhand:ants|singles", byHand = true, year = "2026")

    @Test
    fun `an album found through Discogs says so`() {
        show(lookUp = { BarcodeLookup.Found(listOf(discogsMatch)) })

        lookUpNumber("198029779813")

        composeTestRule.onNodeWithTag("barcode_source_0").assertTextContains("Discogs", substring = true)
        composeTestRule.onNodeWithTag("barcode_source_0").assertTextContains("matched", substring = true)
    }

    @Test
    fun `an album MusicBrainz does not list says it will be added by hand under the artist`() {
        show(lookUp = { BarcodeLookup.Found(listOf(byHandMatch)) })

        lookUpNumber("198029779813")

        composeTestRule.onNodeWithTag("barcode_source_0").assertTextContains("added by hand", substring = true)
        composeTestRule.onNodeWithTag("barcode_source_0").assertTextContains("Adam & The Ants", substring = true)
    }

    @Test
    fun `an album from MusicBrainz's own list has no source line`() {
        show()

        lookUpNumber()

        composeTestRule.onNodeWithTag("barcode_source_0").assertDoesNotExist()
    }

    @Test
    fun `opening an album added by hand opens the album that was made, not the key of the match`() {
        show(lookUp = { BarcodeLookup.Found(listOf(byHandMatch)) }, addOwned = { _, _, _ -> BarcodeAdd.Added("custom/abc") })

        lookUpNumber("198029779813")
        composeTestRule.onNodeWithTag("barcode_add_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("barcode_open_0").performScrollTo().performClick()

        assertEquals(listOf(byHandMatch to "custom/abc"), opened)
    }

    @Test
    fun `a barcode only Discogs knows shows what it found and what to do`() {
        val find = DiscogsFind("Adam & The Ants", "The Singles", "2026", "LP", com.nickallport.findmevinyl.discography.DiscographyCategory.COMPILATION, false)
        show(lookUp = { BarcodeLookup.OnlyOnDiscogs(listOf(find)) })

        lookUpNumber("198029779813")

        val text = "Adam & The Ants - The Singles (LP, 2026)"
        composeTestRule.onNodeWithTag("barcode_message").assertTextContains(text, substring = true)
        composeTestRule.onNodeWithTag("barcode_message").assertTextContains("Search for the artist", substring = true)
    }

    @Test
    fun `a compilation by various artists says it cannot be added from a barcode`() {
        val find = DiscogsFind("Various", "Now 99", "2026", "CD", com.nickallport.findmevinyl.discography.DiscographyCategory.COMPILATION, true)
        show(lookUp = { BarcodeLookup.OnlyOnDiscogs(listOf(find)) })

        lookUpNumber("198029779813")

        composeTestRule.onNodeWithTag("barcode_message").assertTextContains("various artists", substring = true)
    }
}
