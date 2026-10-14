package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoverImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun cover(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows the placeholder while the cover is loading`() {
        val pending = CompletableDeferred<Bitmap?>()

        composeTestRule.setContent {
            CoverImage(albumId = "1", loadCover = { pending.await() })
        }

        composeTestRule.onNodeWithTag("cover_placeholder").assertExists()
        composeTestRule.onNodeWithTag("cover_image").assertDoesNotExist()
    }

    @Test
    fun `shows the cover once it has loaded`() {
        composeTestRule.setContent {
            CoverImage(albumId = "1", loadCover = { cover() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_image").assertExists()
        composeTestRule.onNodeWithTag("cover_placeholder").assertDoesNotExist()
    }

    @Test
    fun `keeps the placeholder when there is no cover`() {
        composeTestRule.setContent {
            CoverImage(albumId = "1", loadCover = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_placeholder").assertExists()
        composeTestRule.onNodeWithTag("cover_image").assertDoesNotExist()
    }

    @Test
    fun `asks for the cover of the album it is given`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            CoverImage(albumId = "abc", loadCover = { id -> asked.add(id); null })
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("abc"), asked)
    }

    @Test
    fun `loads the new cover when the album changes`() {
        val asked = mutableListOf<String>()
        var albumId by mutableStateOf("first")

        composeTestRule.setContent {
            CoverImage(albumId = albumId, loadCover = { id -> asked.add(id); cover() })
        }
        composeTestRule.waitForIdle()

        albumId = "second"
        composeTestRule.waitForIdle()

        assertEquals(listOf("first", "second"), asked)
    }

    @Test
    fun `goes back to the placeholder when the new album has no cover`() {
        var albumId by mutableStateOf("first")

        composeTestRule.setContent {
            CoverImage(
                albumId = albumId,
                loadCover = { id -> if (id == "first") cover() else null }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cover_image").assertExists()

        albumId = "second"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cover_placeholder").assertExists()
        composeTestRule.onNodeWithTag("cover_image").assertDoesNotExist()
    }
}
