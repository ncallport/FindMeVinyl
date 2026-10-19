package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumDetailRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val details = AlbumDetails(
        releaseDate = "1970-10", country = "GB", label = "Charisma", catalogNumber = null,
        firstReleaseDate = "1970-10-23", format = "12\" Vinyl", genres = emptyList(),
        discs = listOf(Disc(null, "12\" Vinyl", listOf(Track("A1", "Looking for Someone", 424000))))
    )

    private var album = AlbumUiModel("1", "Trespass", owned = false, wanted = false)

    private fun show(
        loadDetails: suspend (String) -> AlbumDetails?,
        onToggleOwned: suspend (String, Boolean) -> Unit = { _, _ -> },
        onToggleWanted: suspend (String, Boolean) -> Unit = { _, _ -> },
        loadPressing: suspend (String, String) -> AlbumDetails? = { _, _ -> null },
        saveMyPressing: suspend (String, Pressing?) -> Unit = { _, _ -> }
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailRoute(
                    albumId = "1", title = "Trespass", artistName = "Genesis",
                    loadDetails = loadDetails,
                    loadAlbum = { album },
                    onToggleOwned = onToggleOwned,
                    onToggleWanted = onToggleWanted,
                    loadPressing = loadPressing,
                    saveMyPressing = saveMyPressing
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows a spinner while the details load, then the details`() {
        val gate = CompletableDeferred<AlbumDetails?>()
        show(loadDetails = { gate.await() })

        composeTestRule.onNodeWithTag("detail_loading").assertExists()

        gate.complete(details)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("detail_loading").assertDoesNotExist()
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
    }

    @Test
    fun `says so when the details cannot be loaded, and Try again loads them again`() {
        var calls = 0
        show(loadDetails = { if (++calls == 1) null else details })

        composeTestRule.onNodeWithText("Couldn't load the details").assertExists()

        composeTestRule.onNodeWithTag("detail_retry").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, calls)
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
    }

    @Test
    fun `loads the details of the album it was given`() {
        var asked: String? = null
        show(loadDetails = { asked = it; details })

        assertEquals("1", asked)
    }

    @Test
    fun `marking the album as owned tells the caller and shows the new state`() {
        var told: Pair<String, Boolean>? = null
        show(
            loadDetails = { details },
            onToggleOwned = { id, owned -> told = id to owned; album = album.copy(owned = !owned) }
        )

        composeTestRule.onNodeWithTag("detail_owned_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("1" to false, told)
        composeTestRule.onNodeWithTag("detail_owned_toggle").assertTextEquals("Owned")
        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("+ Want")
    }

    @Test
    fun `wanting the album tells the caller and shows the new state`() {
        var told: Pair<String, Boolean>? = null
        show(
            loadDetails = { details },
            onToggleWanted = { id, wanted -> told = id to wanted; album = album.copy(wanted = !wanted) }
        )

        composeTestRule.onNodeWithTag("detail_want_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("1" to false, told)
        composeTestRule.onNodeWithTag("detail_want_toggle").assertTextEquals("Wanted")
    }

    @Test
    fun `a tap does not reload the details`() {
        var calls = 0
        show(
            loadDetails = { calls++; details },
            onToggleOwned = { _, owned -> album = album.copy(owned = !owned) }
        )

        composeTestRule.onNodeWithTag("detail_owned_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, calls)
    }

    // Choosing another pressing.

    private val pressings = listOf(
        Pressing("r1", "1970", "GB", "Charisma", null, "12\" Vinyl"),
        Pressing("r2", "1972", "DE", "Buddah", null, "12\" Vinyl")
    )
    private val twoPressings = details.copy(pressings = pressings, pressingId = "r1")
    private val secondPressing = twoPressings.copy(
        pressingId = "r2", country = "DE",
        discs = listOf(Disc(null, "12\" Vinyl", listOf(Track("A1", "Another Take", 1000))))
    )

    @Test
    fun `choosing a pressing loads it and shows its tracks`() {
        val asked = mutableListOf<Pair<String, String>>()
        show(
            loadDetails = { twoPressings },
            loadPressing = { album, release -> asked.add(album to release); secondPressing }
        )

        composeTestRule.onNodeWithTag("pressing_row_r2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1" to "r2"), asked)
        composeTestRule.onNodeWithText("Another Take").assertExists()
        composeTestRule.onNodeWithText("Looking for Someone").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pressing_shown_r2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `shows a spinner on the pressing while it loads`() {
        val gate = CompletableDeferred<AlbumDetails?>()
        show(loadDetails = { twoPressings }, loadPressing = { _, _ -> gate.await() })

        composeTestRule.onNodeWithTag("pressing_row_r2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pressing_loading_r2", useUnmergedTree = true).assertExists()
        // The tracks of the pressing that is shown stay on screen meanwhile.
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()

        gate.complete(secondPressing)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pressing_loading_r2", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a pressing that cannot be loaded leaves the page as it was and says so, and can be tried again`() {
        var calls = 0
        show(loadDetails = { twoPressings }, loadPressing = { _, _ -> if (++calls == 1) null else secondPressing })

        composeTestRule.onNodeWithTag("pressing_row_r2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Couldn't load that pressing").assertExists()
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()

        composeTestRule.onNodeWithTag("pressing_row_r2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Couldn't load that pressing").assertDoesNotExist()
        composeTestRule.onNodeWithText("Another Take").assertExists()
    }

    @Test
    fun `choosing a pressing does not load the album's details again`() {
        var loads = 0
        show(loadDetails = { loads++; twoPressings }, loadPressing = { _, _ -> secondPressing })

        composeTestRule.onNodeWithTag("pressing_row_r2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, loads)
    }

    @Test
    fun `saving the copy hands it over and the page then shows what is stored`() {
        album = AlbumUiModel("1", "Trespass", owned = true, wanted = false)
        val saves = mutableListOf<Pair<String, List<AlbumCopy>>>()
        composeTestRule.setContent {
            FindMeVinylTheme {
                AlbumDetailRoute(
                    albumId = "1", title = "Trespass", artistName = "Genesis",
                    loadDetails = { details }, loadAlbum = { album },
                    onToggleOwned = { _, _ -> }, onToggleWanted = { _, _ -> },
                    saveCopies = { id, copies ->
                        saves.add(id to copies)
                        album = album.copy(userFormats = copies.map { it.format }, copies = copies)
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_copy_edit").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("my_copy_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1" to listOf(AlbumCopy("LP"))), saves)
        composeTestRule.onNodeWithTag("my_copy_summary").performScrollTo().assertTextEquals("LP")
    }

    // The pressing the user chose as theirs.

    private val ownedWithPressing2 = AlbumUiModel(
        "1", "Trespass", owned = true, wanted = false, myPressingId = "r2", myPressingText = "1972, Germany, Buddah"
    )

    @Test
    fun `the page opens on the pressing the user chose`() {
        album = ownedWithPressing2
        val asked = mutableListOf<Pair<String, String>>()
        show(
            loadDetails = { twoPressings },
            loadPressing = { id, release -> asked.add(id to release); secondPressing }
        )

        assertEquals(listOf("1" to "r2"), asked)
        composeTestRule.onNodeWithText("Another Take").assertExists()
    }

    @Test
    fun `it is not loaded again when it is the one the page opened on`() {
        album = ownedWithPressing2.copy(myPressingId = "r1")
        val asked = mutableListOf<String>()
        show(loadDetails = { twoPressings }, loadPressing = { _, release -> asked.add(release); secondPressing })

        assertEquals(emptyList<String>(), asked)
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
    }

    @Test
    fun `a chosen pressing that is not among the listed ones is left alone`() {
        album = ownedWithPressing2.copy(myPressingId = "gone")
        val asked = mutableListOf<String>()
        show(loadDetails = { twoPressings }, loadPressing = { _, release -> asked.add(release); secondPressing })

        assertEquals(emptyList<String>(), asked)
        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
    }

    @Test
    fun `an album that is not owned does not open on a pressing`() {
        album = ownedWithPressing2.copy(owned = false)
        val asked = mutableListOf<String>()
        show(loadDetails = { twoPressings }, loadPressing = { _, release -> asked.add(release); secondPressing })

        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `if the chosen pressing cannot be loaded the page stays on the first and does not complain`() {
        album = ownedWithPressing2
        show(loadDetails = { twoPressings }, loadPressing = { _, _ -> null })

        composeTestRule.onNodeWithText("Looking for Someone").assertExists()
        composeTestRule.onNodeWithTag("pressing_failed").assertDoesNotExist()
    }

    @Test
    fun `choosing the pressing shown hands it over and the page then says which is chosen`() {
        album = AlbumUiModel("1", "Trespass", owned = true, wanted = false)
        val saved = mutableListOf<Pair<String, Pressing?>>()
        show(
            loadDetails = { twoPressings },
            saveMyPressing = { id, pressing ->
                saved.add(id to pressing)
                album = album.copy(myPressingId = pressing?.releaseId, myPressingText = pressing?.let { "text for ${it.releaseId}" })
            }
        )

        composeTestRule.onNodeWithTag("my_pressing_mark").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1" to pressings[0]), saved)
        composeTestRule.onNodeWithTag("my_pressing_text").performScrollTo().assertTextEquals("My pressing: text for r1")
        composeTestRule.onNodeWithTag("pressing_mine_r1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `clearing the choice hands over nothing and the page forgets it`() {
        album = AlbumUiModel("1", "Trespass", owned = true, wanted = false, myPressingId = "r1", myPressingText = "text")
        val saved = mutableListOf<Pair<String, Pressing?>>()
        show(
            loadDetails = { twoPressings },
            saveMyPressing = { id, pressing ->
                saved.add(id to pressing)
                album = album.copy(myPressingId = null, myPressingText = null)
            }
        )

        composeTestRule.onNodeWithTag("my_pressing_clear").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf<Pair<String, Pressing?>>("1" to null), saved)
        composeTestRule.onNodeWithTag("my_pressing_text").assertDoesNotExist()
    }
}

