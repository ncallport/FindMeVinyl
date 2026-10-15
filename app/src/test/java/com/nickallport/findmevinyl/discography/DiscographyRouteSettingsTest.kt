package com.nickallport.findmevinyl.discography

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The discography follows what the settings hide. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class DiscographyRouteSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun album(id: String, formats: List<String>? = null, owned: Boolean = false, wanted: Boolean = false) =
        AlbumUiModel(id, "Album $id", owned = owned, wanted = wanted, formats = formats)

    private val state = DiscographyUiState.Success(
        mapOf(
            DiscographyCategory.STUDIO_ALBUM to listOf(
                album("S1", listOf("LP", "CD", "Digital")),
                album("S2", listOf("Digital"))
            ),
            DiscographyCategory.LIVE_ALBUM to listOf(album("L1", listOf("LP")), album("L2", listOf("LP"), owned = true))
        )
    )

    private fun show(
        settings: Settings,
        onOpenSettings: (() -> Unit)? = null,
        loads: () -> Unit = {},
        loadFormats: suspend (String) -> List<String>? = { null }
    ) {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { loads(); flowOf(state) }, onToggleOwned = { _, _ -> },
                settings = settings, onOpenSettings = onOpenSettings, loadFormats = loadFormats
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `with nothing hidden, everything is shown and nothing is said`() {
        show(Settings.ShowEverything)

        composeTestRule.onNodeWithText("Album S1").assertExists()
        composeTestRule.onNodeWithText("Album S2").assertExists()
        composeTestRule.onNodeWithText("Album L1").assertExists()
        composeTestRule.onNodeWithTag("hidden_albums_note").assertDoesNotExist()
    }

    @Test
    fun `leaves out the kinds of album the settings hide, but not an album you own`() {
        show(Settings.ShowEverything.copy(hideLiveAlbums = true))

        composeTestRule.onNodeWithText("Album L1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Album L2").assertExists()
        composeTestRule.onNodeWithText("Album S1").assertExists()
    }

    @Test
    fun `leaves out an album that is only on hidden formats`() {
        show(Settings.ShowEverything.copy(hideDigital = true))

        composeTestRule.onNodeWithText("Album S2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Album S1").assertExists()
    }

    @Test
    fun `says how many albums were left out, and tapping it goes to the settings`() {
        var opened = 0
        show(Settings.ShowEverything.copy(hideLiveAlbums = true, hideDigital = true), onOpenSettings = { opened++ })

        composeTestRule.onNodeWithText("2 albums hidden by your settings").assertExists()
        composeTestRule.onNodeWithTag("hidden_albums_note").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `says it in the singular for one album`() {
        show(Settings.ShowEverything.copy(hideDigital = true))

        composeTestRule.onNodeWithText("1 album hidden by your settings").assertExists()
    }

    @Test
    fun `a hidden format is left out of the row`() {
        val one = DiscographyUiState.Success(
            mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("S1", listOf("LP", "CD", "Digital"))))
        )
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(one) }, onToggleOwned = { _, _ -> },
                settings = Settings.ShowEverything.copy(hideDigital = true)
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true).assertTextEquals("LP · CD")
    }

    @Test
    fun `the format chips leave out a hidden format and count only the albums shown`() {
        show(Settings.ShowEverything.copy(hideDigital = true, hideLiveAlbums = true))

        composeTestRule.onNodeWithTag("format_chip_Digital").assertDoesNotExist()
        composeTestRule.onNodeWithText("LP 2").assertExists()
        composeTestRule.onNodeWithText("CD 1").assertExists()
    }

    @Test
    fun `changing the settings changes the list without loading the discography again`() {
        var settings by mutableStateOf(Settings.ShowEverything)
        var loads = 0
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { loads++; flowOf(state) }, onToggleOwned = { _, _ -> },
                settings = settings
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Album L1").assertExists()

        settings = Settings.ShowEverything.copy(hideLiveAlbums = true)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album L1").assertDoesNotExist()
        assertEquals(1, loads)
    }

    @Test
    fun `an album whose formats arrive from a lookup is left out when they are all hidden`() {
        val unchecked = DiscographyUiState.Success(
            mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("N1", formats = null), album("N2", listOf("LP"))))
        )
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(unchecked) }, onToggleOwned = { _, _ -> },
                settings = Settings.ShowEverything.copy(hideDigital = true), loadFormats = { listOf("Digital") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album N1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Album N2").assertExists()
        composeTestRule.onNodeWithText("1 album hidden by your settings").assertExists()
    }

    @Test
    fun `a lookup that finds a hidden format along with others shows only the others`() {
        val unchecked = DiscographyUiState.Success(
            mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("N1", formats = null)))
        )
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(unchecked) }, onToggleOwned = { _, _ -> },
                settings = Settings.ShowEverything.copy(hideDigital = true), loadFormats = { listOf("CD", "Digital") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels", useUnmergedTree = true).assertTextEquals("CD")
    }

    @Test
    fun `an album whose formats are not known yet is not left out`() {
        val unchecked = DiscographyUiState.Success(
            mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("N1", formats = null)))
        )
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(unchecked) }, onToggleOwned = { _, _ -> },
                settings = Settings.ShowEverything.copy(hideLps = true, hideCds = true, hideDigital = true)
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album N1").assertExists()
    }

    @Test
    fun `an album whose lookup saved only bootleg statuses is left out when bootlegs are hidden`() {
        val unchecked = DiscographyUiState.Success(
            mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(album("N1", formats = null), album("N2", listOf("LP"))))
        )
        var lookedUp = false
        val saved = DiscographyUiState.Success(
            mapOf(
                DiscographyCategory.STUDIO_ALBUM to listOf(
                    AlbumUiModel("N1", "Album N1", owned = false, wanted = false, formats = listOf("LP"), statuses = listOf("Bootleg")),
                    album("N2", listOf("LP"))
                )
            )
        )
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(unchecked) }, onToggleOwned = { _, _ -> },
                settings = Settings.ShowEverything.copy(hideBootlegs = true),
                loadFormats = { lookedUp = true; listOf("LP") },
                loadSaved = { if (lookedUp) saved else unchecked }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Album N1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Album N2").assertExists()
        composeTestRule.onNodeWithText("1 album hidden by your settings").assertExists()
    }

    @Test
    fun `the discography loads once however the settings are`() {
        var count = 0
        show(Settings(), loads = { count++ })

        assertEquals(1, count)
    }
}
