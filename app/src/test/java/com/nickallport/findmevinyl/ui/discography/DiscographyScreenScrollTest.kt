package com.nickallport.findmevinyl.ui.discography

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.nickallport.findmevinyl.discography.DiscographyCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs in the usual small test window, so the list really has to scroll. */
@RunWith(RobolectricTestRunner::class)
class DiscographyScreenScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val many = mapOf(
        DiscographyCategory.STUDIO_ALBUM to (1..40).map {
            AlbumUiModel("$it", "Album $it", owned = it % 10 == 0, wanted = false)
        }
    )

    private var ownedFirst by mutableStateOf(false)

    private fun show() {
        composeTestRule.setContent {
            DiscographyScreen(groupedAlbums = many, onToggleOwned = {}, ownedFirst = ownedFirst)
        }
    }

    @Test
    fun `switching owned first on brings the list back to the top, where the owned section is`() {
        show()
        composeTestRule.onNodeWithTag("discography_list").performScrollToIndex(30)
        composeTestRule.waitForIdle()

        ownedFirst = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("owned_section_header").assertIsDisplayed()
    }

    @Test
    fun `switching owned first off brings the list back to the top too`() {
        ownedFirst = true
        show()
        composeTestRule.onNodeWithTag("discography_list").performScrollToIndex(30)
        composeTestRule.waitForIdle()

        ownedFirst = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Studio Albums").assertIsDisplayed()
    }

    @Test
    fun `a list that is scrolled and not switched stays where it is`() {
        show()
        composeTestRule.onNodeWithTag("discography_list").performScrollToIndex(30)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Studio Albums").assertIsNotDisplayed()
    }
}
