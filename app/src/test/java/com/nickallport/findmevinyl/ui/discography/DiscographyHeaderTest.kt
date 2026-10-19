package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertEquals
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscographyHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the artist name`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithText("Roxy Music").assertExists()
    }

    @Test
    fun `offers Save when the artist is not a favourite`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `shows Saved when the artist is a favourite`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = true, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithText("Saved").assertExists()
    }

    @Test
    fun `tapping the button invokes the callback`() {
        var toggled = false

        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = { toggled = true })
        }

        composeTestRule.onNodeWithTag("favourite_toggle").performClick()

        assertTrue(toggled)
    }

    @Test
    fun `has an Artists button that goes to the favourite artists`() {
        var opened = false
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                onOpenArtists = { opened = true }
            )
        }

        composeTestRule.onNodeWithTag("artists_button").assertTextEquals("Artists")
        composeTestRule.onNodeWithTag("artists_button").performClick()

        assertEquals(true, opened)
    }

    @Test
    fun `has no Artists button when it is not given a place to go`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("artists_button").assertDoesNotExist()
    }

    @Test
    fun `the buttons are on a line below the artist's name, side by side`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {}, onOpenArtists = {}
            )
        }

        fun top(node: androidx.compose.ui.test.SemanticsNodeInteraction) =
            node.fetchSemanticsNode().positionInRoot.y
        val name = top(composeTestRule.onNodeWithText("Roxy Music"))
        val artists = top(composeTestRule.onNodeWithTag("artists_button"))
        val save = top(composeTestRule.onNodeWithTag("favourite_toggle"))

        assertTrue("the buttons are below the name", artists > name && save > name)
        assertEquals(artists, save, 1f)
    }

    @Test
    fun `tapping Artists does not save or unsave the artist`() {
        var toggled = false
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = { toggled = true }, onOpenArtists = {}
            )
        }

        composeTestRule.onNodeWithTag("artists_button").performClick()

        assertEquals(false, toggled)
    }

    // The photo of the artist, which the user can add or change.

    private fun picture() = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)

    @Test
    fun `shows the artist's picture beside the name`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
        val image = composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).fetchSemanticsNode().positionInRoot
        val name = composeTestRule.onNodeWithText("Roxy Music").fetchSemanticsNode().positionInRoot
        assertTrue("the picture is left of the name", image.x < name.x)
    }

    @Test
    fun `shows the record placeholder when the artist has no picture`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { null }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `offers to change the photo when there is a picture, and to add one when there is not`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Change photo").assertExists()
    }

    @Test
    fun `offers to add a photo when there is no picture`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { null }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add photo").assertExists()
        composeTestRule.onNodeWithText("Change photo").assertDoesNotExist()
    }

    @Test
    fun `tapping the photo or its caption asks to choose one`() {
        var chosen = 0
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", loadArtistImage = { picture() }, onChoosePhoto = { chosen++ }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("change_photo").performClick()

        assertEquals(1, chosen)
    }

    @Test
    fun `has no photo or caption when it is not given a way to choose one`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("change_photo").assertDoesNotExist()
        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `loads the picture again when the photo changes`() {
        var version by androidx.compose.runtime.mutableStateOf(0)
        val asked = mutableListOf<String>()
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", photoVersion = version,
                loadArtistImage = { id -> asked.add(id); if (version == 0) null else picture() },
                onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("artist_placeholder", useUnmergedTree = true).assertExists()

        version = 1
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("artist_image", useUnmergedTree = true).assertExists()
        assertEquals(listOf("a", "a"), asked)
    }

    @Test
    fun `tapping the photo does not save the artist or go to the favourites`() {
        var toggled = false
        var opened = false
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = { toggled = true },
                onOpenArtists = { opened = true },
                artistId = "a", loadArtistImage = { null }, onChoosePhoto = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("change_photo").performClick()

        assertEquals(false, toggled)
        assertEquals(false, opened)
    }

    // Joining artists into a group.

    @Test
    fun `has no join button unless it is given somewhere to go`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Nick Cave", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("join_artists_button").assertDoesNotExist()
    }

    @Test
    fun `offers to join with another artist, and tapping it says so`() {
        var joined = 0
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Nick Cave", isFavourite = false, onToggleFavourite = {}, onJoinArtists = { joined++ })
        }

        composeTestRule.onNodeWithTag("join_artists_button").assertTextEquals("Join with another artist")
        composeTestRule.onNodeWithTag("join_artists_button").performClick()

        assertEquals(1, joined)
    }

    @Test
    fun `for an artist already in a group the button edits the group`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Nick Cave", isFavourite = false, onToggleFavourite = {}, onJoinArtists = {}, inGroup = true
            )
        }

        composeTestRule.onNodeWithTag("join_artists_button").assertTextEquals("Edit group")
    }

    @Test
    fun `the join button shares the line with the others when they fit`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Nick Cave", isFavourite = false, onToggleFavourite = {}, onOpenArtists = {}, onJoinArtists = {}
            )
        }

        val save = composeTestRule.onNodeWithTag("favourite_toggle").fetchSemanticsNode().positionInRoot.y
        val join = composeTestRule.onNodeWithTag("join_artists_button").fetchSemanticsNode().positionInRoot.y
        assertEquals(save, join, 1f)
    }

    @Test
    fun `a group page has no Save button, since a group is always kept`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Nick Cave, all of it", isFavourite = true, onToggleFavourite = {}, onOpenArtists = {},
                onJoinArtists = {}, inGroup = true, showFavourite = false
            )
        }

        composeTestRule.onNodeWithTag("favourite_toggle").assertDoesNotExist()
        composeTestRule.onNodeWithTag("artists_button").assertExists()
        composeTestRule.onNodeWithTag("join_artists_button").assertExists()
    }

    @Test
    fun `an artist page still has its Save button`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Nick Cave", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("favourite_toggle").assertExists()
    }

    @Test
    fun `has an Add an album button that reports a tap`() {
        var taps = 0
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Big Country", isFavourite = false, onToggleFavourite = {}, onAddAlbum = { taps++ })
        }

        composeTestRule.onNodeWithTag("add_album_button").performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `has no Add an album button without a way to add one`() {
        composeTestRule.setContent {
            DiscographyHeader(artistName = "Big Country", isFavourite = false, onToggleFavourite = {})
        }

        composeTestRule.onNodeWithTag("add_album_button").assertDoesNotExist()
    }

    // The buttons share a line when they fit, and the photo is bigger.

    private fun y(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
    private fun x(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.x

    @Test
    fun `on a group's page Artists, Edit group and Add an album are on one line, in that order`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Nick Cave, all of it", isFavourite = true, onToggleFavourite = {}, onOpenArtists = {},
                onJoinArtists = {}, inGroup = true, showFavourite = false, onAddAlbum = {}
            )
        }

        assertEquals(y("artists_button"), y("join_artists_button"), 1f)
        assertEquals(y("artists_button"), y("add_album_button"), 1f)
        assertTrue(x("artists_button") < x("join_artists_button"))
        assertTrue(x("join_artists_button") < x("add_album_button"))
    }

    @Test
    fun `the buttons are below the name and the picture, under all of them`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {}, onOpenArtists = {},
                onJoinArtists = {}, onAddAlbum = {}, artistId = "a", onChoosePhoto = {}, loadArtistImage = { picture() }
            )
        }
        composeTestRule.waitForIdle()

        val photoBottom = composeTestRule.onNodeWithTag("change_photo").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(y("artists_button") >= photoBottom - 1f)
        assertTrue(y("artists_button") > composeTestRule.onNodeWithText("Roxy Music").fetchSemanticsNode().positionInRoot.y)
    }

    @Test
    fun `the buttons wrap onto another line when they do not all fit`() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.width(200.dp)) {
                DiscographyHeader(
                    artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {}, onOpenArtists = {},
                    onJoinArtists = {}, onAddAlbum = {}
                )
            }
        }

        assertTrue(y("add_album_button") > y("artists_button"))
        composeTestRule.onNodeWithTag("add_album_button").assertExists()
    }

    @Test
    fun `the photo is bigger than it was`() {
        composeTestRule.setContent {
            DiscographyHeader(
                artistName = "Roxy Music", isFavourite = false, onToggleFavourite = {},
                artistId = "a", onChoosePhoto = {}, loadArtistImage = { picture() }
            )
        }
        composeTestRule.waitForIdle()

        val width = composeTestRule.onNodeWithTag("change_photo").fetchSemanticsNode().boundsInRoot.width
        val density = composeTestRule.density.density
        assertTrue("the photo is ${width / density} dp wide", width / density >= 110f)
    }
}
