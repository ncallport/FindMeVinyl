package com.nickallport.findmevinyl.ui.brand

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.width
import com.nickallport.findmevinyl.discography.DiscographyRoute
import com.nickallport.findmevinyl.ui.artist.ArtistSearchResultsScreen
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import com.nickallport.findmevinyl.ui.favourites.FavouritesRoute
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import com.nickallport.findmevinyl.ui.wantlist.WantListRoute
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** While a page loads, the record is in the middle of the width of the screen, as it is on the Shops page. */
@RunWith(RobolectricTestRunner::class)
class SpinnerCentringTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // A page sits in a column as wide as the screen, as in the app.
    private fun inScreen(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            FindMeVinylTheme { Column(Modifier.fillMaxSize()) { content() } }
        }
    }

    private fun assertCentred(tag: String) {
        composeTestRule.waitForIdle()
        val screen = composeTestRule.onRoot().getBoundsInRoot()
        val spinner = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()

        assertEquals(screen.width.value / 2, (spinner.left + spinner.width / 2).value, 1f)
    }

    @Test
    fun `the record is centred while artists are searched for`() {
        inScreen { ArtistSearchResultsScreen(state = ArtistSearchUiState.Loading, onArtistSelected = {}) }

        assertCentred("loading_indicator")
    }

    @Test
    fun `the record is centred while the favourite artists load`() {
        inScreen { FavouritesRoute(load = { awaitCancellation() }, onArtistSelected = {}) }

        assertCentred("loading_indicator")
    }

    @Test
    fun `the record is centred while the want list loads`() {
        inScreen { WantListRoute(load = { awaitCancellation() }, exportText = { "" }, onShareText = {}) }

        assertCentred("loading_indicator")
    }

    @Test
    fun `the record is centred while a discography loads`() {
        inScreen {
                DiscographyRoute(
                    artistId = "a", artistName = "Genesis", load = { flow { awaitCancellation() } },
                    onToggleOwned = { _, _ -> }
                )
            }

        assertCentred("loading_indicator")
    }
}
