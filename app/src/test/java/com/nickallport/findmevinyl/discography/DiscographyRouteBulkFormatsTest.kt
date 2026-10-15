package com.nickallport.findmevinyl.discography

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The discography checks the formats of all its albums by itself, once, when it has loaded. */
@RunWith(RobolectricTestRunner::class)
class DiscographyRouteBulkFormatsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun album(id: String, formats: List<String>? = null, stale: Boolean = false, statuses: List<String>? = null) =
        AlbumUiModel(
            id = id, title = "Album $id", owned = false, wanted = false, formats = formats, formatsStale = stale,
            statuses = statuses
        )

    private fun success(vararg albums: AlbumUiModel, loadingMore: Boolean = false, loadError: String? = null) =
        DiscographyUiState.Success(mapOf(DiscographyCategory.STUDIO_ALBUM to albums.toList()), loadingMore, loadError)

    private fun loading(state: DiscographyUiState): (String) -> Flow<DiscographyUiState> = { flowOf(state) }

    private fun show(
        state: DiscographyUiState,
        loadSaved: (suspend (String) -> DiscographyUiState)? = null,
        loadFormats: suspend (String) -> List<String>? = { null },
        load: (String) -> Flow<DiscographyUiState> = loading(state),
        check: (suspend (String, (Int, Int) -> Unit) -> FormatCheckResult)?
    ) {
        composeTestRule.setContent {
            DiscographyRoute(
                artistId = "a", artistName = "Genesis", load = load, onToggleOwned = { _, _ -> },
                loadSaved = loadSaved, loadFormats = loadFormats, checkAllFormats = check
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `checks the formats of the artist once the discography has loaded`() {
        val asked = mutableListOf<String>()

        show(success(album("A"), album("B", listOf("LP")))) { artist, _ -> asked.add(artist); FormatCheckResult.Done(2) }

        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `does not start while more pages are still loading`() {
        val rest = CompletableDeferred<Unit>()
        val asked = mutableListOf<String>()
        val load: (String) -> Flow<DiscographyUiState> = {
            flow {
                emit(success(album("A"), loadingMore = true))
                rest.await()
                emit(success(album("A"), album("B")))
            }
        }

        show(success(), load = load) { artist, _ -> asked.add(artist); FormatCheckResult.Done(2) }
        assertEquals(emptyList<String>(), asked)

        rest.complete(Unit)
        composeTestRule.waitForIdle()

        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `still starts when some pages could not be loaded, since the saved albums are what is shown`() {
        val asked = mutableListOf<String>()

        show(success(album("A"), loadError = "HTTP 503")) { artist, _ -> asked.add(artist); FormatCheckResult.Done(1) }

        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `does not start when every album is checked and up to date`() {
        val asked = mutableListOf<String>()

        show(
            success(album("A", listOf("LP"), statuses = listOf("Official")), album("B", emptyList(), statuses = emptyList()))
        ) { artist, _ -> asked.add(artist); FormatCheckResult.Done(0) }

        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `starts when the formats are known but the release statuses are not, as for albums saved before statuses`() {
        val asked = mutableListOf<String>()

        show(success(album("A", listOf("LP")))) { artist, _ -> asked.add(artist); FormatCheckResult.Done(1) }

        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `checks once a visit even when some albums stay without statuses`() {
        val asked = mutableListOf<String>()

        show(success(album("A"))) { artist, _ -> asked.add(artist); FormatCheckResult.Done(0) }

        assertEquals(1, asked.size)
    }

    @Test
    fun `starts when an album's formats are old enough to look up again`() {
        val asked = mutableListOf<String>()

        show(success(album("A", listOf("LP"), stale = true))) { artist, _ -> asked.add(artist); FormatCheckResult.Done(1) }

        assertEquals(listOf("a"), asked)
    }

    @Test
    fun `does not start when it has not been given a way to check`() {
        show(success(album("A")), check = null)

        composeTestRule.onNodeWithTag("format_check_progress").assertDoesNotExist()
        composeTestRule.onNodeWithTag("format_check_failed").assertDoesNotExist()
    }

    @Test
    fun `says how far the check has got while it runs, and stops saying it when it is done`() {
        val gate = CompletableDeferred<Unit>()

        show(success(album("A"))) { _, progress ->
            progress(1, 3)
            gate.await()
            FormatCheckResult.Done(1)
        }

        composeTestRule.onNodeWithTag("format_check_progress").assertExists()
        composeTestRule.onNodeWithText("Checking formats… 1 of 3").assertExists()

        gate.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("format_check_progress").assertDoesNotExist()
    }

    @Test
    fun `shows the formats the check found, as chips with counts`() {
        var checked = false
        val before = success(album("A"), album("B"))
        val after = success(album("A", listOf("LP")), album("B", listOf("LP", "CD")))

        show(
            before,
            check = { _, _ -> checked = true; FormatCheckResult.Done(2) },
            loadSaved = { if (checked) after else before }
        )

        composeTestRule.onNodeWithText("LP 2").assertExists()
        composeTestRule.onNodeWithText("CD 1").assertExists()
    }

    @Test
    fun `albums the check found formats for are not looked up one by one`() {
        var checked = false
        val lookups = mutableListOf<String>()
        val before = success(album("A"), album("B"))
        val after = success(album("A", listOf("LP")), album("B", listOf("CD")))

        show(
            before,
            check = { _, _ -> checked = true; FormatCheckResult.Done(2) },
            loadSaved = { if (checked) after else before },
            loadFormats = { id -> lookups.add(id); listOf("MC") }
        )

        assertEquals(emptyList<String>(), lookups)
    }

    @Test
    fun `rows wait while the check runs instead of looking up their album`() {
        val gate = CompletableDeferred<Unit>()
        val lookups = mutableListOf<String>()

        show(
            success(album("A")),
            check = { _, _ -> gate.await(); FormatCheckResult.TooBig },
            loadFormats = { id -> lookups.add(id); listOf("LP") }
        )
        assertEquals(emptyList<String>(), lookups)

        gate.complete(Unit)
        composeTestRule.waitForIdle()

        // The artist was too big to check at once, so the row looks up its own album as before.
        assertEquals(listOf("A"), lookups)
    }

    @Test
    fun `an artist that is too big says nothing about it`() {
        show(success(album("A")), check = { _, _ -> FormatCheckResult.TooBig })

        composeTestRule.onNodeWithTag("format_check_progress").assertDoesNotExist()
        composeTestRule.onNodeWithTag("format_check_failed").assertDoesNotExist()
    }

    @Test
    fun `the check runs only once a visit, even after a tap changes the albums`() {
        var runs = 0
        val state = success(album("A"))

        show(state, check = { _, _ -> runs++; FormatCheckResult.TooBig }, loadSaved = { state })
        composeTestRule.onNodeWithTag("owned_toggle").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, runs)
    }

    @Test
    fun `says so when the check failed, and Try again runs it again`() {
        var runs = 0

        show(success(album("A"))) { _, _ -> if (++runs == 1) FormatCheckResult.Failed else FormatCheckResult.TooBig }

        composeTestRule.onNodeWithTag("format_check_failed").assertExists()
        composeTestRule.onNodeWithText("Couldn't check all the formats").assertExists()

        composeTestRule.onNodeWithTag("format_check_retry").performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, runs)
        composeTestRule.onNodeWithTag("format_check_failed").assertDoesNotExist()
    }

    @Test
    fun `rows go back to looking up their own album when the check failed`() {
        val lookups = mutableListOf<String>()

        show(
            success(album("A")),
            check = { _, _ -> FormatCheckResult.Failed },
            loadFormats = { id -> lookups.add(id); listOf("LP") }
        )

        assertEquals(listOf("A"), lookups)
    }
}
