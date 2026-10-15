package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals

// A tall window, so the header, the chips and several rows all fit and every row is composed.
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class DiscographyRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun album(
        id: String,
        title: String = "Album $id",
        owned: Boolean = false,
        wanted: Boolean = false,
        formats: List<String>? = null
    ) = AlbumUiModel(id = id, title = title, owned = owned, wanted = wanted, formats = formats)

    private fun success(vararg albums: AlbumUiModel, loadingMore: Boolean = false) =
        DiscographyUiState.Success(mapOf(DiscographyCategory.STUDIO_ALBUM to albums.toList()), loadingMore)

    private fun loading(state: DiscographyUiState): (String) -> Flow<DiscographyUiState> = { flowOf(state) }

    private val emptySuccess = loading(DiscographyUiState.Success(emptyMap()))

    @Test
    fun `loads and shows albums for the given artist`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "some-artist-id", artistName = "Genesis",
                load = loading(success(album("1", "Foxtrot"))),
                onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `asks for the cover of each album and shows it`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "some-artist-id",
                artistName = "Genesis",
                load = loading(success(album("1", "Foxtrot"))),
                onToggleOwned = { _, _ -> },
                loadCover = { id ->
                    asked.add(id)
                    Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_image", useUnmergedTree = true).assertExists()
        assertEquals(listOf("1"), asked)
    }

    @Test
    fun `looks up the formats of albums that have none and shows them`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "some-artist-id",
                artistName = "Genesis",
                load = loading(success(album("1", "Foxtrot"))),
                onToggleOwned = { _, _ -> },
                loadFormats = { id -> asked.add(id); listOf("LP", "CD") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true)
            .assertTextEquals("LP · CD")
        assertEquals(listOf("1"), asked)
    }

    @Test
    fun `shows the error message when loading fails`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "some-artist-id", artistName = "Genesis",
                load = loading(DiscographyUiState.Error("Something went wrong")),
                onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }

    @Test
    fun `shows the albums of the first page while the later pages are still loading`() {
        val rest = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                emit(success(album("1", "Foxtrot"), loadingMore = true))
                rest.await()
                emit(success(album("1", "Foxtrot"), album("2", "Nursery Cryme")))
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
        composeTestRule.onNodeWithText("Nursery Cryme").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loading_indicator").assertDoesNotExist()

        rest.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
        composeTestRule.onNodeWithText("Nursery Cryme").assertExists()
    }

    @Test
    fun `shows a footer while more pages are loading and removes it when they are done`() {
        val rest = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                emit(success(album("1", "Foxtrot"), loadingMore = true))
                rest.await()
                emit(success(album("1", "Foxtrot"), album("2", "Nursery Cryme")))
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_more").assertExists()

        rest.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_more").assertDoesNotExist()
    }

    @Test
    fun `says so when a later page could not be loaded`() {
        val failed = DiscographyUiState.Success(
            mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("1", "Foxtrot"))),
            loadError = "HTTP 503 Server Error"
        )

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(failed), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
        composeTestRule.onNodeWithText("Couldn't load all the albums").assertExists()
        composeTestRule.onNodeWithText("HTTP 503 Server Error").assertExists()
    }

    @Test
    fun `Try again loads the discography again and clears the message`() {
        var loads = 0
        val secondTry = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            loads++
            if (loads == 1) {
                flowOf(
                    DiscographyUiState.Success(
                        mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("1", "Foxtrot"))),
                        loadError = "HTTP 503 Server Error"
                    )
                )
            } else {
                flow {
                    secondTry.await()
                    emit(success(album("1", "Foxtrot"), album("2", "Nursery Cryme")))
                }
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("retry_button").performClick()
        composeTestRule.waitForIdle()

        // The list stays while the second try is waiting, with the message replaced by the footer.
        composeTestRule.onNodeWithText("Foxtrot").assertExists()
        composeTestRule.onNodeWithTag("load_error").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loading_more").assertExists()

        secondTry.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Nursery Cryme").assertExists()
        composeTestRule.onNodeWithTag("loading_more").assertDoesNotExist()
        assertEquals(2, loads)
    }

    @Test
    fun `a tap keeps the footer while more pages are still loading`() {
        val never = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                emit(success(album("1", "Foxtrot"), loadingMore = true))
                never.await()
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = load,
                onToggleOwned = { _, _ -> },
                loadSaved = { success(album("1", "Foxtrot", owned = true)) }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Owned").assertExists()
        composeTestRule.onNodeWithTag("loading_more").assertExists()
    }

    @Test
    fun `shows the spinner until the first state arrives`() {
        val firstPage = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                firstPage.await()
                emit(success(album("1", "Foxtrot")))
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertExists()

        firstPage.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `tapping an album opens it with its id and title`() {
        var opened: Pair<String, String>? = null

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis",
                load = { flowOf(success(album("1", "Foxtrot"))) },
                onToggleOwned = { _, _ -> },
                onOpenAlbum = { id, title -> opened = id to title }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("album_row").performClick()

        assertEquals("1" to "Foxtrot", opened)
    }

    @Test
    fun `tapping Owned updates the album from the saved albums without loading the discography again`() {
        var loads = 0
        var owned = false
        val load: (String) -> Flow<DiscographyUiState> = { loads++; flowOf(success(album("1", "Foxtrot"))) }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = load,
                onToggleOwned = { _, _ -> owned = true },
                loadSaved = { success(album("1", "Foxtrot", owned = owned)) }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Owned").assertExists()
        assertEquals(1, loads)
    }

    @Test
    fun `tapping Want updates the album from the saved albums without loading the discography again`() {
        var loads = 0
        var wanted = false
        val load: (String) -> Flow<DiscographyUiState> = { loads++; flowOf(success(album("1", "Foxtrot"))) }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = load,
                onToggleOwned = { _, _ -> },
                onToggleWanted = { _, _ -> wanted = true },
                loadSaved = { success(album("1", "Foxtrot", wanted = wanted)) }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("wanted_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Wanted").assertExists()
        composeTestRule.onNodeWithTag("wanted_toggle").assertDoesNotExist()
        assertEquals(1, loads)
    }

    @Test
    fun `a tap does not lose albums that arrived after the saved list was read`() {
        val rest = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                emit(success(album("1", "Foxtrot"), loadingMore = true))
                rest.await()
                emit(success(album("1", "Foxtrot"), album("2", "Nursery Cryme")))
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = load,
                onToggleOwned = { _, _ -> },
                // An older read: it has not got album 2 yet.
                loadSaved = { success(album("1", "Foxtrot", owned = true)) }
            )
        }
        composeTestRule.waitForIdle()
        rest.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("owned_toggle")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Owned").assertExists()
        composeTestRule.onNodeWithText("Nursery Cryme").assertExists()
    }

    @Test
    fun `shows the artist name in the header`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
    }

    @Test
    fun `shows Saved when the artist is already a favourite`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `tapping Save saves the artist and then shows Saved`() {
        var saved = false
        var savedWith: Pair<String, String>? = null

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { saved },
                onSaveFavourite = { id, name ->
                    saved = true
                    savedWith = id to name
                }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()
        composeTestRule.waitForIdle()

        assertEquals("id-1" to "Roxy Music", savedWith)
        composeTestRule.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `tapping Saved removes the favourite and then shows Save`() {
        var saved = true

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1",
                artistName = "Roxy Music",
                load = emptySuccess,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { saved },
                onRemoveFavourite = { saved = false }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `shows no format filter until some formats are known`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis",
                load = loading(success(album("A"), album("B"))), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_chip_all").assertDoesNotExist()
    }

    @Test
    fun `shows a chip for each format found with how many albums have it`() {
        val state = success(album("A", formats = listOf("LP", "CD")), album("B", formats = listOf("CD")))

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("LP 1").assertExists()
        composeTestRule.onNodeWithText("CD 2").assertExists()
    }

    @Test
    fun `choosing a format shows the albums that have it and the ones not checked yet`() {
        val state = success(
            album("A", formats = listOf("LP")),
            album("B", formats = listOf("CD")),
            album("C", formats = null)
        )

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album A").assertExists()
        composeTestRule.onNodeWithText("Album B").assertDoesNotExist()
        composeTestRule.onNodeWithText("Album C").assertExists()
    }

    @Test
    fun `All brings every album back`() {
        val state = success(album("A", formats = listOf("LP")), album("B", formats = listOf("CD")))

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Album B").assertDoesNotExist()

        composeTestRule.onNodeWithTag("format_chip_all").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album A").assertExists()
        composeTestRule.onNodeWithText("Album B").assertExists()
    }

    @Test
    fun `says Checking formats for the albums not checked while a format is chosen`() {
        val gate = CompletableDeferred<Unit>()
        val state = success(album("A", formats = listOf("LP")), album("C", formats = null))

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> },
                loadFormats = { gate.await(); null }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checking_formats", useUnmergedTree = true).assertDoesNotExist()

        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("checking_formats", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `says how many albums are not checked yet while a format is chosen`() {
        val gate = CompletableDeferred<Unit>()
        val state = success(
            album("A", formats = listOf("LP")), album("C", formats = null), album("D", formats = null)
        )

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> },
                loadFormats = { gate.await(); null }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("2 not checked yet").assertExists()
    }

    @Test
    fun `an album drops out when its lookup shows it is not on the chosen format`() {
        val gate = CompletableDeferred<Unit>()
        val state = success(album("A", formats = listOf("LP")), album("C", formats = null))

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> },
                loadFormats = { id -> gate.await(); if (id == "C") listOf("CD") else null }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Album C").assertExists()

        gate.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album A").assertExists()
        composeTestRule.onNodeWithText("Album C").assertDoesNotExist()
        composeTestRule.onNodeWithText("CD 1").assertExists()
        composeTestRule.onNodeWithTag("unchecked_note").assertDoesNotExist()
    }

    @Test
    fun `says no albums were found when none has the chosen format any more`() {
        val later = CompletableDeferred<Unit>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                emit(success(album("A", formats = listOf("LP"))))
                later.await()
                emit(success(album("A", formats = listOf("CD"))))
            }
        }

        composeTestRule.setContent {
            DiscographyRoute(artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> })
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()

        later.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("no_matches").assertExists()
        composeTestRule.onNodeWithText("No albums found on LP").assertExists()
        composeTestRule.onNodeWithText("LP 0").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `keeps the chosen format after the state is restored`() = runComposeUiTest {
        val state = success(album("A", formats = listOf("LP")), album("B", formats = listOf("CD")))
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(state), onToggleOwned = { _, _ -> }
            )
        }
        waitForIdle()
        onNodeWithTag("format_chip_LP").performClick()
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithText("Album A").assertExists()
        onNodeWithText("Album B").assertDoesNotExist()
    }

    @Test
    fun `a tap brings in formats that lookups saved but the screen had missed`() {
        val load: (String) -> Flow<DiscographyUiState> = { flowOf(success(album("A", formats = null))) }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> },
                loadSaved = { success(album("A", owned = true, formats = listOf("LP"))) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("format_chip_all").assertDoesNotExist()

        composeTestRule.onNodeWithTag("owned_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("LP 1").assertExists()
    }

    @Test
    fun `saving or removing a favourite does not load the discography again`() {
        var loads = 0
        var saved = false
        val load: (String) -> Flow<DiscographyUiState> = { loads++; flowOf(success(album("1", "Foxtrot"))) }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "id-1", artistName = "Roxy Music", load = load,
                onToggleOwned = { _, _ -> },
                loadIsFavourite = { saved },
                onSaveFavourite = { _, _ -> saved = true },
                onRemoveFavourite = { saved = false },
                loadSaved = { success(album("1", "Foxtrot")) }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("favourite_toggle").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, loads)
    }

    // The view options: owned first, and the way to the favourite artists.

    private fun twoOwned() = success(
        album("A", formats = listOf("LP"), owned = true),
        album("B", formats = listOf("CD"), owned = true),
        album("C", formats = listOf("LP"))
    )

    @Test
    fun `has an Owned first chip, off to begin with`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(twoOwned()), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_first_toggle").assertExists()
        composeTestRule.onNodeWithTag("owned_section_header").assertDoesNotExist()
    }

    @Test
    fun `the Owned first chip groups the owned albums at the top, and again takes the grouping away`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(twoOwned()), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_first_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owned_section_header").assertExists()

        composeTestRule.onNodeWithTag("owned_first_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owned_section_header").assertDoesNotExist()
    }

    @Test
    fun `a chosen format also narrows the owned section`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(twoOwned()), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owned_first_toggle").performClick()
        composeTestRule.onNodeWithTag("format_chip_LP").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album A").assertExists()
        composeTestRule.onNodeWithText("Album B").assertDoesNotExist()
    }

    @Test
    fun `the format chips count every album, however the list is grouped`() {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(twoOwned()), onToggleOwned = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owned_first_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("LP 2").assertExists()
        composeTestRule.onNodeWithText("CD 1").assertExists()
    }

    @Test
    fun `the Artists button goes to the favourite artists`() {
        var opened = 0
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = loading(twoOwned()), onToggleOwned = { _, _ -> },
                onOpenArtists = { opened++ }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artists_button").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `marking an album as owned while owned first is on moves it into the owned section`() {
        var owned = false
        val state = { success(album("A", owned = owned), album("B")) }

        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(state()) },
                onToggleOwned = { _, _ -> owned = true },
                loadSaved = { state() }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owned_first_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owned_section_header").assertDoesNotExist()

        composeTestRule.onAllNodesWithTag("owned_toggle")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_section_header").assertExists()
    }
}
