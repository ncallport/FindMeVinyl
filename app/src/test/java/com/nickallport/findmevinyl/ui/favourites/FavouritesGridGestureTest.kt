package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.test.click
import androidx.compose.ui.test.center
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Touching, holding and dragging the pictures of the favourites grid. */
@RunWith(RobolectricTestRunner::class)
class FavouritesGridGestureTest {

    @get:Rule
    val rule = createComposeRule()

    private val three = listOf(
        FavouriteArtistUiModel("1", "Roxy Music"),
        FavouriteArtistUiModel("2", "The Smiths"),
        FavouriteArtistUiModel("3", "Talk Talk")
    )

    private fun picture(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    private var selected: FavouriteArtistUiModel? = null
    private var reordered: List<String>? = null

    private fun show() {
        selected = null
        reordered = null
        rule.setContent {
            FavouritesScreen(
                artists = three, onArtistSelected = { selected = it }, grid = true,
                loadArtistImage = { picture() }, onReorder = { reordered = it }
            )
        }
        rule.waitForIdle()
    }

    // Presses a picture and waits until the long press has been recognised.
    private fun hold(id: String) {
        rule.onNodeWithTag("favourite_artist_$id").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(700)
    }

    private fun xOf(id: String) = rule.onNodeWithTag("favourite_artist_$id").fetchSemanticsNode().positionInRoot.x

    @Test
    fun `a quick touch on a picture opens the artist`() {
        show()

        rule.onNodeWithTag("favourite_artist_2").performTouchInput { click() }

        assertEquals(three[1], selected)
    }

    @Test
    fun `holding a picture and lifting the finger in the same place does not open the artist or move it`() {
        show()

        hold("1")
        rule.onNodeWithTag("favourite_artist_1").performTouchInput { up() }
        rule.waitForIdle()

        assertNull(selected)
        assertNull(reordered)
    }

    @Test
    fun `holding a picture and dragging it to another place moves it there`() {
        show()
        val dx = xOf("3") - xOf("1")

        hold("1")
        rule.onNodeWithTag("favourite_artist_1").performTouchInput { moveBy(Offset(dx, 0f)); up() }
        rule.waitForIdle()

        assertEquals(listOf("2", "3", "1"), reordered)
        assertNull(selected)
    }

    @Test
    fun `dragging a picture back where it started changes nothing`() {
        show()
        val dx = xOf("2") - xOf("1")

        hold("1")
        rule.onNodeWithTag("favourite_artist_1").performTouchInput { moveBy(Offset(dx, 0f)); moveBy(Offset(-dx, 0f)); up() }
        rule.waitForIdle()

        assertNull(reordered)
    }

    @Test
    fun `a screen reader can move a picture earlier or later`() {
        show()

        fun act(id: String, label: String) {
            val actions = rule.onNodeWithTag("favourite_artist_$id").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            rule.runOnUiThread { actions.firstOrNull { it.label == label }?.action?.invoke() }
            rule.waitForIdle()
        }

        act("2", "Move earlier")
        assertEquals(listOf("2", "1", "3"), reordered)

        reordered = null
        act("3", "Move later") // the last one has no such action
        assertNull(reordered)
    }

    @Test
    fun `the hint tells how to move a picture`() {
        show()

        rule.onNodeWithTag("favourites_grid_hint").assertExists()
        rule.onNodeWithText("Touch and hold a picture, then drag it to move it.").assertExists()
    }

    @Test
    fun `the route passes a drag on and shows the order that is stored afterwards`() {
        var stored = three
        val passed = mutableListOf<List<String>>()
        rule.setContent {
            FavouritesRoute(
                load = { stored }, onArtistSelected = {}, grid = true, loadArtistImage = { picture() },
                onReorder = { ids -> passed.add(ids); stored = ids.map { id -> three.first { it.id == id } } }
            )
        }
        rule.waitForIdle()
        val dx = xOf("3") - xOf("1")

        hold("1")
        rule.onNodeWithTag("favourite_artist_1").performTouchInput { moveBy(Offset(dx, 0f)); up() }
        rule.waitForIdle()

        assertEquals(listOf(listOf("2", "3", "1")), passed)
        assertEquals(true, xOf("1") > xOf("3"))
    }

    @Test
    fun `a second drag still moves the other pictures aside while the finger is down`() {
        var stored = three
        rule.setContent {
            FavouritesRoute(
                load = { stored }, onArtistSelected = {}, grid = true, loadArtistImage = { picture() },
                onReorder = { ids -> stored = ids.map { id -> three.first { it.id == id } } }
            )
        }
        rule.waitForIdle()

        // First drag: 1 to the end, and the route reads the list again.
        hold("1")
        rule.onNodeWithTag("favourite_artist_1").performTouchInput { moveBy(Offset(xOf("3") - xOf("1"), 0f)); up() }
        rule.waitForIdle()

        // Second drag: 2 (now first) to the end. Before the finger is lifted, 3 must already have moved to its place.
        val third = xOf("1")
        hold("2")
        rule.onNodeWithTag("favourite_artist_2").performTouchInput { moveBy(Offset(third - xOf("2"), 0f)) }
        rule.waitForIdle()

        assertEquals(true, xOf("2") > xOf("3"))
        rule.onNodeWithTag("favourite_artist_2").performTouchInput { up() }
    }
}
