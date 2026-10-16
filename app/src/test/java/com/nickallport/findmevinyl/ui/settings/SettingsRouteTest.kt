package com.nickallport.findmevinyl.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.layout.positionInRoot
import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Your data section of the settings page: back up, restore, and what it says afterwards. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class SettingsRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        backUp: (suspend () -> String?)? = { null },
        restore: (suspend () -> String?)? = { null }
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                SettingsRoute(settings = Settings(), onChange = {}, backUp = backUp, restore = restore)
            }
        }
    }

    @Test
    fun `has a Your data section with Back up and Restore buttons`() {
        show()

        composeTestRule.onNodeWithText("Your data").assertExists()
        composeTestRule.onNodeWithTag("backup_button").performScrollTo().assertExists()
        composeTestRule.onNodeWithTag("restore_button").performScrollTo().assertExists()
    }

    @Test
    fun `the section comes after the location settings`() {
        show()

        val location = composeTestRule.onNodeWithText("Location Settings").fetchSemanticsNode().positionInRoot.y
        val data = composeTestRule.onNodeWithText("Your data").fetchSemanticsNode().positionInRoot.y

        assertEquals(true, location < data)
    }

    @Test
    fun `without a way to back up, the section is not shown`() {
        show(backUp = null, restore = null)

        composeTestRule.onNodeWithText("Your data").assertDoesNotExist()
    }

    @Test
    fun `explains that a restore only adds`() {
        show()

        composeTestRule.onNodeWithTag("data_hint").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("never removes", substring = true).assertExists()
    }

    @Test
    fun `tapping Back up asks for a backup and shows what it says`() {
        var asked = 0
        show(backUp = { asked++; "Backed up 2 artists." })

        composeTestRule.onNodeWithTag("backup_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, asked)
        composeTestRule.onNodeWithText("Backed up 2 artists.").assertExists()
    }

    @Test
    fun `tapping Restore asks for a restore and shows what it says`() {
        var asked = 0
        show(restore = { asked++; "Restored 1 artist." })

        composeTestRule.onNodeWithTag("restore_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, asked)
        composeTestRule.onNodeWithText("Restored 1 artist.").assertExists()
    }

    @Test
    fun `nothing is said when the user backs out of the file picker`() {
        show(backUp = { null })

        composeTestRule.onNodeWithTag("backup_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("data_message").assertDoesNotExist()
    }

    @Test
    fun `a new message replaces the old one`() {
        var next = "First."
        show(backUp = { next })

        composeTestRule.onNodeWithTag("backup_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        next = "Second."
        composeTestRule.onNodeWithTag("backup_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("First.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Second.").assertExists()
    }

    @Test
    fun `both buttons wait while one is working, so two cannot run at once`() {
        val gate = CompletableDeferred<String?>()
        show(backUp = { gate.await() })

        composeTestRule.onNodeWithTag("backup_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("backup_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("restore_button").assertIsNotEnabled()

        gate.complete("Done.")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("backup_button").assertIsEnabled()
        composeTestRule.onNodeWithText("Done.").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the message is still there after the phone is turned`() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            FindMeVinylTheme {
                SettingsRoute(settings = Settings(), onChange = {}, backUp = { "Backed up 2 artists." }, restore = { null })
            }
        }
        onNodeWithTag("backup_button").performScrollTo().performClick()
        waitForIdle()

        restoration.emulateSaveAndRestore()
        waitForIdle()

        onNodeWithText("Backed up 2 artists.").assertExists()
    }
}
