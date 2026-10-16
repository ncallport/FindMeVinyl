package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun picture(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows the placeholder while the picture is loading`() {
        val pending = CompletableDeferred<Bitmap?>()

        composeTestRule.setContent {
            ArtistImage(artistId = "1", loadImage = { pending.await() })
        }

        composeTestRule.onNodeWithTag("artist_placeholder").assertExists()
        composeTestRule.onNodeWithTag("artist_image").assertDoesNotExist()
    }

    @Test
    fun `shows the picture once it has loaded`() {
        composeTestRule.setContent {
            ArtistImage(artistId = "1", loadImage = { picture() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_image").assertExists()
        composeTestRule.onNodeWithTag("artist_placeholder").assertDoesNotExist()
    }

    @Test
    fun `keeps the placeholder when there is no picture`() {
        composeTestRule.setContent {
            ArtistImage(artistId = "1", loadImage = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_placeholder").assertExists()
        composeTestRule.onNodeWithTag("artist_image").assertDoesNotExist()
    }

    @Test
    fun `asks for the picture of the artist it is given`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            ArtistImage(artistId = "abc", loadImage = { id -> asked.add(id); null })
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("abc"), asked)
    }

    @Test
    fun `goes back to the placeholder when the row is reused for an artist with no picture`() {
        var artistId by mutableStateOf("first")

        composeTestRule.setContent {
            ArtistImage(artistId = artistId, loadImage = { id -> if (id == "first") picture() else null })
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("artist_image").assertExists()

        artistId = "second"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_placeholder").assertExists()
        composeTestRule.onNodeWithTag("artist_image").assertDoesNotExist()
    }

    @Test
    fun `says when a picture is shown and when it goes away`() {
        val changes = mutableListOf<Boolean>()
        var visible by mutableStateOf(true)

        composeTestRule.setContent {
            if (visible) {
                ArtistImage(artistId = "1", loadImage = { picture() }, onShownChange = { changes.add(it) })
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf(true), changes)

        visible = false
        composeTestRule.waitForIdle()

        assertEquals(listOf(true, false), changes)
    }

    @Test
    fun `says nothing when there is no picture`() {
        val changes = mutableListOf<Boolean>()

        composeTestRule.setContent {
            ArtistImage(artistId = "1", loadImage = { null }, onShownChange = { changes.add(it) })
        }
        composeTestRule.waitForIdle()

        assertEquals(emptyList<Boolean>(), changes)
    }

    @Test
    fun `can fill the space it is given, as a tile in a grid does`() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.size(100.dp)) {
                ArtistImage(artistId = "1", loadImage = { null }, fillSize = true)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true)
            .assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(100.dp)
    }
}
