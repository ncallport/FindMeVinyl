package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows each favourite artist's name`() {
        val artists = listOf(
            FavouriteArtistUiModel("1", "Roxy Music"),
            FavouriteArtistUiModel("2", "The Smiths")
        )

        composeTestRule.setContent {
            FavouritesScreen(artists = artists, onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("The Smiths").assertExists()
    }

    private val twoArtists = listOf(
        FavouriteArtistUiModel("1", "Roxy Music"),
        FavouriteArtistUiModel("2", "The Smiths")
    )

    private fun picture(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows a placeholder picture for each artist before any picture has loaded`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("artist_placeholder", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `asks for the picture of each artist and shows the ones that come back`() {
        val asked = mutableListOf<String>()

        composeTestRule.setContent {
            FavouritesScreen(
                artists = twoArtists,
                onArtistSelected = {},
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
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { picture() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertTextEquals("Artist photos: Wikimedia Commons")
    }

    @Test
    fun `shows no credit while there is no picture`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, loadArtistImage = { null })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("photo_credit").assertDoesNotExist()
    }

    @Test
    fun `shows a message when there are no favourites`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = emptyList(), onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("No favourite artists yet").assertExists()
    }

    @Test
    fun `tapping an artist invokes the callback with that artist`() {
        var selected: FavouriteArtistUiModel? = null
        val artists = listOf(FavouriteArtistUiModel("1", "Roxy Music"))

        composeTestRule.setContent {
            FavouritesScreen(artists = artists, onArtistSelected = { selected = it })
        }

        composeTestRule.onNodeWithTag("favourite_artist_1").performClick()

        assertEquals("1", selected?.id)
        assertEquals("Roxy Music", selected?.name)
    }

    private val threeArtists = listOf(
        FavouriteArtistUiModel("1", "Roxy Music"),
        FavouriteArtistUiModel("2", "The Smiths"),
        FavouriteArtistUiModel("3", "Ramones")
    )

    @Test
    fun `each artist has buttons to move it up and down`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = threeArtists, onArtistSelected = {})
        }

        composeTestRule.onNodeWithTag("favourite_up_2").assertIsEnabled()
        composeTestRule.onNodeWithTag("favourite_down_2").assertIsEnabled()
    }

    @Test
    fun `the first artist cannot move up and the last cannot move down`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = threeArtists, onArtistSelected = {})
        }

        composeTestRule.onNodeWithTag("favourite_up_1").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("favourite_down_1").assertIsEnabled()
        composeTestRule.onNodeWithTag("favourite_up_3").assertIsEnabled()
        composeTestRule.onNodeWithTag("favourite_down_3").assertIsNotEnabled()
    }

    @Test
    fun `a single favourite has nothing to move past, so no move buttons`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = listOf(FavouriteArtistUiModel("1", "Roxy Music")), onArtistSelected = {})
        }

        composeTestRule.onNodeWithTag("favourite_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourite_down_1").assertDoesNotExist()
    }

    @Test
    fun `the move buttons report the artist and do not open it`() {
        val up = mutableListOf<String>()
        val down = mutableListOf<String>()
        val opened = mutableListOf<String>()

        composeTestRule.setContent {
            FavouritesScreen(
                artists = threeArtists,
                onArtistSelected = { opened.add(it.id) },
                onMoveUp = { up.add(it) },
                onMoveDown = { down.add(it) }
            )
        }

        composeTestRule.onNodeWithTag("favourite_up_2").performClick()
        composeTestRule.onNodeWithTag("favourite_down_2").performClick()

        assertEquals(listOf("2"), up)
        assertEquals(listOf("2"), down)
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun `the move buttons say which artist they move`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = threeArtists, onArtistSelected = {})
        }

        composeTestRule.onNodeWithContentDescription("Move The Smiths up").assertExists()
        composeTestRule.onNodeWithContentDescription("Move The Smiths down").assertExists()
    }

    // The grid: pictures only, so a lot more fit on the screen.

    @Test
    fun `in the grid each artist is a tile, described by the name, that opens the artist`() {
        var selected: FavouriteArtistUiModel? = null
        composeTestRule.setContent {
            FavouritesScreen(
                artists = twoArtists, onArtistSelected = { selected = it }, grid = true,
                loadArtistImage = { picture() }
            )
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
            FavouritesScreen(
                artists = twoArtists, onArtistSelected = {}, grid = true,
                loadArtistImage = { id -> if (id == "1") picture() else null }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
        composeTestRule.onNodeWithText("The Smiths").assertExists()
    }

    @Test
    fun `the grid has no move buttons, and says where the order can be changed`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, grid = true)
        }

        composeTestRule.onNodeWithTag("favourite_up_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourite_down_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("favourites_grid_hint").assertExists()
    }

    @Test
    fun `the list has no such hint`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {})
        }

        composeTestRule.onNodeWithTag("favourites_grid_hint").assertDoesNotExist()
    }

    @Test
    fun `the grid keeps the picture credit while a picture is shown`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = twoArtists, onArtistSelected = {}, grid = true, loadArtistImage = { picture() })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Artist photos: Wikimedia Commons").assertExists()
    }

    @Test
    fun `the grid holds many artists in a small space, several in each row`() {
        val many = (1..40).map { FavouriteArtistUiModel("$it", "Artist $it") }
        composeTestRule.setContent {
            FavouritesScreen(artists = many, onArtistSelected = {}, grid = true)
        }
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
        memberIds = listOf("amt", "amt-two", "amt-three"), imageId = "group/1"
    )

    @Test
    fun `a group in the list shows its own name and how many artists it joins`() {
        composeTestRule.setContent {
            FavouritesScreen(artists = listOf(group, twoArtists[0]), onArtistSelected = {})
        }

        composeTestRule.onNodeWithText("Acid Mothers Temple Family").assertExists()
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
        composeTestRule.setContent {
            FavouritesScreen(artists = listOf(group), onArtistSelected = {}, grid = true)
        }

        composeTestRule.onNodeWithText("Acid Mothers Temple Family").assertExists()
    }
}

