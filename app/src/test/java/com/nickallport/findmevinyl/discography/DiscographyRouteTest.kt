package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class DiscographyRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun album(id: String, title: String = "Album $id", owned: Boolean = false, wanted: Boolean = false) =
        AlbumUiModel(id = id, title = title, owned = owned, wanted = wanted)

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

        composeTestRule.onNodeWithText("Foxtrot").performClick()
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
    fun `tapping an album updates it from the saved albums without loading the discography again`() {
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

        composeTestRule.onNodeWithTag("album_row").performClick()
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

        composeTestRule.onNodeWithText("Want").assertExists()
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

        composeTestRule.onNodeWithText("Foxtrot").performClick()
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
}
