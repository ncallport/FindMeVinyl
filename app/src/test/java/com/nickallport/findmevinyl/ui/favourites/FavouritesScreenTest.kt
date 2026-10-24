package com.nickallport.findmevinyl.ui.favourites

import org.robolectric.annotation.Config
import android.graphics.Bitmap
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The favourite artists, as blocks in a list or as a grid of pictures, in the order given (A to Z). */
@RunWith(RobolectricTestRunner::class)
class FavouritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val twoArtists = listOf(
        FavouriteArtistUiModel("1", "Roxy Music", ownedCount = 4, wantedCount = 3),
        FavouriteArtistUiModel("2", "The Smiths", ownedCount = 0, wantedCount = 1)
    )

    private fun picture(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows each favourite artist's name`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}) }

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("The Smiths").assertExists()
    }

    @Test
    fun `each artist in the list is a block with how many albums are owned and wanted under the name`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}) }

        composeTestRule.onNodeWithTag("favourite_counts_1", useUnmergedTree = true).assertTextEquals("4 owned, 3 wanted")
        composeTestRule.onNodeWithTag("favourite_counts_2", useUnmergedTree = true).assertTextEquals("0 owned, 1 wanted")
    }

    @Test
    fun `the counts are under the name, and the picture is to the left of both`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { picture() }) }
        composeTestRule.waitForIdle()

        fun pos(tag: String) = composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)[0].fetchSemanticsNode().positionInRoot
        val picture = pos("artist_image")
        val name = composeTestRule.onNodeWithText("Roxy Music", useUnmergedTree = true).fetchSemanticsNode().positionInRoot
        val counts = pos("favourite_counts_1")

        assertTrue("the counts are below the name", counts.y > name.y)
        assertTrue("the picture is left of the name", picture.x < name.x)
    }

    @Test
    fun `the list has no move buttons and no hint about the order`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}) }

        composeTestRule.onNodeWithTag("favourite_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourite_down_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourites_grid_hint").assertDoesNotExist()
    }

    @Test
    fun `the list shows the artists in the order given`() {
        val sorted = listOf(FavouriteArtistUiModel("2", "ABC"), FavouriteArtistUiModel("1", "Zebra"), FavouriteArtistUiModel("3", "Madness"))
        composeTestRule.setContent { FavouritesScreen(artists = sorted, onArtistSelected = {}) }

        fun y(id: String) = composeTestRule.onNodeWithTag("favourite_artist_$id").fetchSemanticsNode().positionInRoot.y
        assertTrue(y("2") < y("1") && y("1") < y("3"))
    }

    @Test
    fun `shows a placeholder picture for each artist before any picture has loaded`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}) }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("artist_placeholder", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `asks for the picture of each artist and shows the ones that come back`() {
        val asked = mutableListOf<String>()
        composeTestRule.setContent {
            FavouritesScreen(
                artists = twoArtists, onArtistSelected = {},
                loadArtistImage = { id -> asked.add(id); if (id == "1") picture() else null }
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(setOf("1", "2"), asked.toSet())
        composeTestRule.onAllNodesWithTag("artist_image", useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("artist_placeholder", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `credits Wikimedia Commons once a picture is shown`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { picture() }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertTextEquals("Artist photos: Wikimedia Commons")
    }

    @Test
    fun `shows no credit while there is no picture`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { null }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertDoesNotExist()
    }

    @Test
    fun `shows a message when there are no favourites`() {
        composeTestRule.setContent { FavouritesScreen(artists = emptyList(), onArtistSelected = {}) }

        composeTestRule.onNodeWithText("No favourite artists yet").assertExists()
    }

    @Test
    fun `tapping an artist invokes the callback with that artist`() {
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = { selected = it }) }

        composeTestRule.onNodeWithTag("favourite_artist_1").performClick()

        assertEquals(twoArtists[0], selected)
    }

    // The grid.

    @Test
    fun `in the grid each artist is a tile, described by the name, that opens the artist`() {
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = { selected = it }, grid = true, loadArtistImage = { picture() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_artist_1").assertExists()
        composeTestRule.onNodeWithContentDescription("Roxy Music").assertExists()
        composeTestRule.onNodeWithTag("favourite_artist_2").performClick()

        assertEquals(twoArtists[1], selected)
    }

    @Test
    fun `every tile shows the name of its artist, whether or not it has a picture`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, grid = true, loadArtistImage = { id -> if (id == "1") picture() else null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("The Smiths").assertExists()
    }

    @Test
    fun `the grid has no move buttons and no hint about dragging`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}, grid = true) }

        composeTestRule.onNodeWithTag("favourite_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourite_down_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourites_grid_hint").assertDoesNotExist()
    }

    @Test
    fun `the grid keeps the picture credit while a picture is shown`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}, grid = true, loadArtistImage = { picture() }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Artist photos: Wikimedia Commons").assertExists()
    }

    @Test
    fun `the grid holds many artists in a small space, several in each row, in order`() {
        val many = (1..40).map { FavouriteArtistUiModel("$it", "Artist $it") }
        composeTestRule.setContent { FavouritesScreen(artists = many, onArtistSelected = {}, grid = true) }
        composeTestRule.waitForIdle()

        val first = composeTestRule.onNodeWithTag("favourite_artist_1").fetchSemanticsNode().positionInRoot
        val second = composeTestRule.onNodeWithTag("favourite_artist_2").fetchSemanticsNode().positionInRoot
        val third = composeTestRule.onNodeWithTag("favourite_artist_3").fetchSemanticsNode().positionInRoot

        assertEquals(first.y, second.y, 0.5f)
        assertEquals(first.y, third.y, 0.5f)
        assertEquals(true, second.x > first.x && third.x > second.x)
    }

    // Groups of artists.

    private val group = FavouriteArtistUiModel(
        id = "group/1", name = "Acid Mothers Temple Family", isGroup = true,
        memberIds = listOf("amt", "amt-two", "amt-three"), imageId = "group/1", ownedCount = 2, wantedCount = 5
    )

    @Test
    fun `a group in the list shows its own name, its counts and how many artists it joins`() {
        composeTestRule.setContent { FavouritesScreen(artists = listOf(group, twoArtists[0]), onArtistSelected = {}) }

        composeTestRule.onNodeWithText("Acid Mothers Temple Family").assertExists()
        composeTestRule.onNodeWithTag("favourite_counts_group/1", useUnmergedTree = true).assertTextEquals("2 owned, 5 wanted")
        composeTestRule.onNodeWithTag("favourite_group_note_group/1", useUnmergedTree = true).assertTextEquals("3 artists joined")
        composeTestRule.onNodeWithTag("favourite_group_note_1", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a group in the list asks for the picture of the group`() {
        val asked = mutableListOf<String>()
        composeTestRule.setContent {
            FavouritesScreen(artists = listOf(group), onArtistSelected = {}, loadArtistImage = { id -> asked.add(id); picture() })
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("group/1"), asked)
    }

    @Test
    fun `a group in the grid asks for the picture of the group, and opens as the group`() {
        val asked = mutableListOf<String>()
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent {
            FavouritesScreen(
                artists = listOf(group), onArtistSelected = { selected = it }, grid = true,
                loadArtistImage = { id -> asked.add(id); picture() }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_artist_group/1").performClick()

        assertEquals(listOf("group/1"), asked)
        assertEquals(group, selected)
    }

    @Test
    fun `a group in the grid shows its name on the tile`() {
        composeTestRule.setContent { FavouritesScreen(artists = listOf(group), onArtistSelected = {}, grid = true) }

        composeTestRule.onNodeWithText("Acid Mothers Temple Family").assertExists()
    }

    // Various Artists and Soundtracks, below the favourites.

    private val specials = listOf(
        FavouriteArtistUiModel("various", "Various Artists", ownedCount = 3, wantedCount = 1),
        FavouriteArtistUiModel("special/soundtracks", "Soundtracks", ownedCount = 0, wantedCount = 2)
    )

    @Test
    @Config(qualifiers = "w360dp-h2400dp")
    fun `below the artists is a title and a block for Various Artists and one for Soundtracks`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, specials = specials, onArtistSelected = {}) }

        composeTestRule.onNodeWithTag("favourites_specials_title").assertTextEquals("Various Artists and Soundtracks")
        composeTestRule.onNodeWithTag("favourite_counts_various", useUnmergedTree = true).assertTextEquals("3 owned, 1 wanted")
        composeTestRule.onNodeWithTag("favourite_counts_special/soundtracks", useUnmergedTree = true).assertTextEquals("0 owned, 2 wanted")
        fun y(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
        assertTrue(y("favourite_artist_2") < y("favourites_specials_title"))
        assertTrue(y("favourites_specials_title") < y("favourite_artist_various"))
        assertTrue(y("favourite_artist_various") < y("favourite_artist_special/soundtracks"))
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp")
    fun `a block opens its page`() {
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, specials = specials, onArtistSelected = { selected = it }) }

        composeTestRule.onNodeWithTag("favourite_artist_special/soundtracks").performClick()

        assertEquals(specials[1], selected)
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp")
    fun `without the blocks there is no title`() {
        composeTestRule.setContent { FavouritesScreen(artists = twoArtists, onArtistSelected = {}) }

        composeTestRule.onNodeWithTag("favourites_specials_title").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp")
    fun `with no favourites yet the blocks are still shown, under the message`() {
        composeTestRule.setContent { FavouritesScreen(artists = emptyList(), specials = specials, onArtistSelected = {}) }

        composeTestRule.onNodeWithText("No favourite artists yet").assertExists()
        composeTestRule.onNodeWithTag("favourites_specials_title").assertExists()
        composeTestRule.onNodeWithTag("favourite_artist_various").assertExists()
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp")
    fun `the blocks ask for their own pictures`() {
        val asked = mutableListOf<String>()
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, specials = specials, onArtistSelected = {}, loadArtistImage = { id -> asked.add(id); null })
        }
        composeTestRule.waitForIdle()

        assertTrue(asked.toSet().containsAll(listOf("various", "special/soundtracks")))
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp")
    fun `in the grid the title spans the width and the blocks are tiles`() {
        val many = (1..9).map { FavouriteArtistUiModel("a$it", "Artist $it") }
        composeTestRule.setContent { FavouritesScreen(artists = many, specials = specials, onArtistSelected = {}, grid = true) }

        composeTestRule.onNodeWithTag("favourites_specials_title").assertExists()
        composeTestRule.onNodeWithTag("favourite_artist_various").assertExists()
        composeTestRule.onNodeWithContentDescription("Soundtracks").assertExists()
        val title = composeTestRule.onNodeWithTag("favourites_specials_title").fetchSemanticsNode().positionInRoot.y
        assertTrue(title > composeTestRule.onNodeWithTag("favourite_artist_a9").fetchSemanticsNode().positionInRoot.y)
    }
}
