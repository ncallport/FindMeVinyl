package com.nickallport.findmevinyl.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
