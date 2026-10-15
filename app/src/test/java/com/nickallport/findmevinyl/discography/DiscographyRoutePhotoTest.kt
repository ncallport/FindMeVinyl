package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Adding, changing and removing the photo of an artist from the discography. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class DiscographyRoutePhotoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun picture() = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    private val state = DiscographyUiState.Success(
        mapOf(DiscographyCategory.STUDIO_ALBUM to listOf(AlbumUiModel("1", "Foxtrot", owned = false, wanted = false, formats = listOf("LP"))))
    )

    private fun show(
        loadArtistImage: suspend (String) -> Bitmap? = { null },
        onChoosePhoto: (suspend (String) -> Boolean)? = { false }
    ) {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { flowOf(state) }, onToggleOwned = { _, _ -> },
                loadArtistImage = loadArtistImage,
                onChoosePhoto = onChoosePhoto
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows the artist's picture in the header`() {
        show(loadArtistImage = { picture() })

        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `has no photo block when it is not given a way to choose a photo`() {
        show(onChoosePhoto = null)

        composeTestRule.onNodeWithTag("change_photo").assertDoesNotExist()
    }

    @Test
    fun `choosing a photo for this artist loads the picture again`() {
        val chosenFor = mutableListOf<String>()
        var chosen = false
        val asked = mutableListOf<String>()
        show(
            loadArtistImage = { id -> asked.add(id); if (chosen) picture() else null },
            onChoosePhoto = { id -> chosenFor.add(id); chosen = true; true }
        )
        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertExists()

        composeTestRule.onNodeWithTag("change_photo").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("a"), chosenFor)
        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
        assertEquals(2, asked.size)
    }

    @Test
    fun `a photo that was not chosen, or could not be kept, changes nothing`() {
        val asked = mutableListOf<String>()
        show(
            loadArtistImage = { id -> asked.add(id); null },
            onChoosePhoto = { false }
        )

        composeTestRule.onNodeWithTag("change_photo").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertExists()
        assertEquals(1, asked.size)
    }

    @Test
    fun `choosing a photo does not load the discography again`() {
        var loads = 0
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = { loads++; flowOf(state) }, onToggleOwned = { _, _ -> },
                onChoosePhoto = { true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("change_photo").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, loads)
    }
}
