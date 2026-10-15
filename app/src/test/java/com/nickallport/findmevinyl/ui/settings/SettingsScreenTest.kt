package com.nickallport.findmevinyl.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.layout.positionInRoot
import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(settings: Settings = Settings(), darkTheme: Boolean = false, onChange: (Settings) -> Unit = {}) {
        composeTestRule.setContent {
            FindMeVinylTheme(darkTheme = darkTheme) { SettingsScreen(settings = settings, onChange = onChange) }
        }
    }

    @Test
    fun `has a location settings section with the charity shops switch`() {
        show()

        composeTestRule.onNodeWithText("Location Settings").assertExists()
        composeTestRule.onNodeWithText("Hide Charity shops").assertExists()
    }

    @Test
    fun `a switch shows what is set, on or off`() {
        show(Settings(hideCharityShops = true))
        composeTestRule.onNodeWithTag("setting_hideCharityShops").assertIsOn()
    }

    @Test
    fun `a switch that is off shows off`() {
        show(Settings(hideCharityShops = false))
        composeTestRule.onNodeWithTag("setting_hideCharityShops").assertIsOff()
    }

    @Test
    fun `tapping the row switches the setting and passes on all the settings with that one changed`() {
        val changed = mutableListOf<Settings>()
        show(Settings(hideCharityShops = true, hideCds = true), onChange = { changed.add(it) })

        composeTestRule.onNodeWithTag("setting_hideCharityShops").performClick()

        assertEquals(listOf(Settings(hideCharityShops = false, hideCds = true)), changed)
    }

    @Test
    fun `tapping a setting that is off switches it on`() {
        val changed = mutableListOf<Settings>()
        show(Settings(hideCharityShops = false), onChange = { changed.add(it) })

        composeTestRule.onNodeWithTag("setting_hideCharityShops").performClick()

        assertEquals(true, changed.single().hideCharityShops)
    }

    @Test
    fun `tapping the words of a setting switches it as well`() {
        val changed = mutableListOf<Settings>()
        show(onChange = { changed.add(it) })

        composeTestRule.onNodeWithText("Hide Charity shops").performClick()

        assertEquals(1, changed.size)
    }

    @Test
    fun `shows the new value when the settings change`() {
        var settings by androidx.compose.runtime.mutableStateOf(Settings(hideCharityShops = true))
        composeTestRule.setContent {
            FindMeVinylTheme { SettingsScreen(settings = settings, onChange = { settings = it }) }
        }

        composeTestRule.onNodeWithTag("setting_hideCharityShops").performClick()

        composeTestRule.onNodeWithTag("setting_hideCharityShops").assertIsOff()
    }

    @Test
    fun `draws in dark mode too`() {
        show(darkTheme = true)

        composeTestRule.onNodeWithText("Hide Charity shops").assertExists()
    }

    // The discography settings.

    private data class Row(val label: String, val key: String, val flip: (Settings) -> Settings, val isOn: (Settings) -> Boolean)

    private val discographyRows = listOf(
        Row("Hide Live Albums", "hideLiveAlbums", { it.copy(hideLiveAlbums = !it.hideLiveAlbums) }, { it.hideLiveAlbums }),
        Row("Hide Compilation Albums", "hideCompilations", { it.copy(hideCompilations = !it.hideCompilations) }, { it.hideCompilations }),
        Row("Hide Singles", "hideSingles", { it.copy(hideSingles = !it.hideSingles) }, { it.hideSingles }),
        Row("Hide Other Releases", "hideOtherReleases", { it.copy(hideOtherReleases = !it.hideOtherReleases) }, { it.hideOtherReleases }),
        Row("Hide LPs", "hideLps", { it.copy(hideLps = !it.hideLps) }, { it.hideLps }),
        Row("Hide CDs", "hideCds", { it.copy(hideCds = !it.hideCds) }, { it.hideCds }),
        Row("Hide 12\"s", "hideTwelveInches", { it.copy(hideTwelveInches = !it.hideTwelveInches) }, { it.hideTwelveInches }),
        Row("Hide 7\"s", "hideSevenInches", { it.copy(hideSevenInches = !it.hideSevenInches) }, { it.hideSevenInches }),
        Row("Hide MCs", "hideMcs", { it.copy(hideMcs = !it.hideMcs) }, { it.hideMcs }),
        Row("Hide Digital", "hideDigital", { it.copy(hideDigital = !it.hideDigital) }, { it.hideDigital }),
        Row("Hide Bootlegs", "hideBootlegs", { it.copy(hideBootlegs = !it.hideBootlegs) }, { it.hideBootlegs }),
        Row("Hide Promos", "hidePromos", { it.copy(hidePromos = !it.hidePromos) }, { it.hidePromos }),
        Row("Hide Unknown Status releases", "hideUnknownStatus", { it.copy(hideUnknownStatus = !it.hideUnknownStatus) }, { it.hideUnknownStatus })
    )

    @Test
    fun `has a discography settings section with a switch for each kind and format`() {
        show()

        composeTestRule.onNodeWithText("Discography Settings").assertExists()
        discographyRows.forEach { composeTestRule.onNodeWithText(it.label).assertExists() }
    }

    @Test
    fun `the discography settings come before the location settings`() {
        show()

        val discography = composeTestRule.onNodeWithText("Discography Settings").fetchSemanticsNode().positionInRoot.y
        val location = composeTestRule.onNodeWithText("Location Settings").fetchSemanticsNode().positionInRoot.y

        assertEquals(true, discography < location)
    }

    @Test
    fun `the rows come in the order the user listed them`() {
        show()

        val tops = discographyRows.map { composeTestRule.onNodeWithText(it.label).fetchSemanticsNode().positionInRoot.y }

        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `each switch shows what is set`() {
        val settings = Settings()
        show(settings)

        discographyRows.forEach { row ->
            val node = composeTestRule.onNodeWithTag("setting_${row.key}")
            if (row.isOn(settings)) node.assertIsOn() else node.assertIsOff()
        }
    }

    @Test
    fun `tapping each row changes that setting and nothing else`() {
        var settings by androidx.compose.runtime.mutableStateOf(Settings())
        composeTestRule.setContent {
            FindMeVinylTheme { SettingsScreen(settings = settings, onChange = { settings = it }) }
        }

        discographyRows.forEach { row ->
            val before = settings

            composeTestRule.onNodeWithTag("setting_${row.key}").performScrollTo().performClick()

            assertEquals(row.label, row.flip(before), settings)
        }
    }

    @Test
    fun `says that what you own or want is always shown, and how formats are hidden`() {
        show()

        composeTestRule.onNodeWithTag("settings_discography_hint").assertExists()
        composeTestRule.onNodeWithText("Albums you own or want are always shown.", substring = true).assertExists()
        composeTestRule.onNodeWithText("every format it came out on", substring = true).assertExists()
        composeTestRule.onNodeWithText("has no official release", substring = true).assertExists()
    }
}
