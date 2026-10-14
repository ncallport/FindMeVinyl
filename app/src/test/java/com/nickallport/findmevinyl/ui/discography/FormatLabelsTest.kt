package com.nickallport.findmevinyl.ui.discography

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormatLabelsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dot = " · "

    @Test
    fun `shows stored formats at once without asking for them`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FormatLabels(
                albumId = "1", formats = listOf("LP", "CD"), stale = false,
                loadFormats = { id -> asked.add(id); null }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels").assertTextEquals("LP${dot}CD")
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `looks up the formats when none are stored and shows what it finds`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FormatLabels(
                albumId = "abc", formats = null, stale = false,
                loadFormats = { id -> asked.add(id); listOf("LP", "MC") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels").assertTextEquals("LP${dot}MC")
        assertEquals(listOf("abc"), asked)
    }

    @Test
    fun `shows nothing when the lookup finds no formats`() {
        composeTestRule.setContent {
            FormatLabels(albumId = "1", formats = null, stale = false, loadFormats = { emptyList() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels").assertDoesNotExist()
    }

    @Test
    fun `shows nothing when the lookup fails`() {
        composeTestRule.setContent {
            FormatLabels(albumId = "1", formats = null, stale = false, loadFormats = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels").assertDoesNotExist()
    }

    @Test
    fun `does not ask again when the stored list is empty and fresh`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FormatLabels(
                albumId = "1", formats = emptyList(), stale = false,
                loadFormats = { id -> asked.add(id); null }
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(emptyList<String>(), asked)
        composeTestRule.onNodeWithTag("format_labels").assertDoesNotExist()
    }

    @Test
    fun `stale formats are shown and then refreshed`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FormatLabels(
                albumId = "1", formats = listOf("CD"), stale = true,
                loadFormats = { id -> asked.add(id); listOf("LP", "CD") }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels").assertTextEquals("LP${dot}CD")
        assertEquals(listOf("1"), asked)
    }

    @Test
    fun `keeps the stale formats when the refresh fails`() {
        composeTestRule.setContent {
            FormatLabels(albumId = "1", formats = listOf("CD"), stale = true, loadFormats = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_labels").assertTextEquals("CD")
    }

    @Test
    fun `looks up the new album when the row is reused for another`() {
        val asked = mutableListOf<String>()
        var albumId by mutableStateOf("first")

        composeTestRule.setContent {
            FormatLabels(
                albumId = albumId, formats = null, stale = false,
                loadFormats = { id -> asked.add(id); listOf(id.take(1).uppercase()) }
            )
        }
        composeTestRule.waitForIdle()

        albumId = "second"
        composeTestRule.waitForIdle()

        assertEquals(listOf("first", "second"), asked)
        composeTestRule.onNodeWithTag("format_labels").assertTextEquals("S")
    }

    @Test
    fun `does not ask a second time when the stored formats arrive after the lookup`() {
        val asked = mutableListOf<String>()
        var stored by mutableStateOf<List<String>?>(null)

        composeTestRule.setContent {
            FormatLabels(
                albumId = "1", formats = stored, stale = false,
                loadFormats = { id -> asked.add(id); listOf("LP") }
            )
        }
        composeTestRule.waitForIdle()

        stored = listOf("LP")
        composeTestRule.waitForIdle()

        assertEquals(listOf("1"), asked)
        composeTestRule.onNodeWithTag("format_labels").assertTextEquals("LP")
    }
}
