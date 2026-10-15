package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DiscographyVisibilityTest {

    private fun album(
        id: String,
        formats: List<String>? = null,
        owned: Boolean = false,
        wanted: Boolean = false
    ) = AlbumUiModel(id = id, title = "Album $id", owned = owned, wanted = wanted, formats = formats)

    private fun grouped(vararg pairs: Pair<DiscographyCategory, List<AlbumUiModel>>) = mapOf(*pairs)

    private fun ids(result: DiscographyVisibility.Result, category: DiscographyCategory) =
        result.shown[category].orEmpty().map { it.id }

    private val studio = DiscographyCategory.STUDIO_ALBUM
    private val live = DiscographyCategory.LIVE_ALBUM
    private val compilation = DiscographyCategory.COMPILATION
    private val ep = DiscographyCategory.EP
    private val single = DiscographyCategory.SINGLE
    private val other = DiscographyCategory.OTHER

    @Test
    fun `with nothing hidden, everything is shown as it is`() {
        val input = grouped(studio to listOf(album("1", listOf("LP", "CD", "Digital"))), live to listOf(album("2")))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything)

        assertEquals(input, result.shown)
        assertEquals(0, result.hiddenCount)
    }

    // Kinds of album

    @Test
    fun `hides live albums when told to, and only them`() {
        val input = grouped(
            studio to listOf(album("1")), live to listOf(album("2")), compilation to listOf(album("3")),
            ep to listOf(album("4")), single to listOf(album("5")), other to listOf(album("6"))
        )

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideLiveAlbums = true))

        assertEquals(listOf("1"), ids(result, studio))
        assertEquals(emptyList<String>(), ids(result, live))
        assertEquals(listOf("3"), ids(result, compilation))
        assertEquals(listOf("4"), ids(result, ep))
        assertEquals(listOf("5"), ids(result, single))
        assertEquals(listOf("6"), ids(result, other))
        assertEquals(1, result.hiddenCount)
    }

    @Test
    fun `hides compilations, singles and other releases when told to`() {
        val input = grouped(
            compilation to listOf(album("1"), album("2")), single to listOf(album("3")), other to listOf(album("4"))
        )

        val result = DiscographyVisibility.apply(
            input, Settings.ShowEverything.copy(hideCompilations = true, hideSingles = true, hideOtherReleases = true)
        )

        assertEquals(emptyMap<DiscographyCategory, List<AlbumUiModel>>(), result.shown)
        assertEquals(4, result.hiddenCount)
    }

    @Test
    fun `studio albums and EPs can never be hidden by a kind`() {
        val input = grouped(studio to listOf(album("1")), ep to listOf(album("2")))
        val everything = Settings(
            hideLiveAlbums = true, hideCompilations = true, hideSingles = true, hideOtherReleases = true,
            hideLps = false, hideCds = false, hideTwelveInches = false, hideSevenInches = false, hideDigital = false
        )

        val result = DiscographyVisibility.apply(input, everything)

        assertEquals(input, result.shown)
    }

    @Test
    fun `an album that is owned is shown even in a kind that is hidden`() {
        val input = grouped(live to listOf(album("1", owned = true), album("2")))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideLiveAlbums = true))

        assertEquals(listOf("1"), ids(result, live))
        assertEquals(1, result.hiddenCount)
    }

    @Test
    fun `an album that is wanted is shown even in a kind that is hidden`() {
        val input = grouped(single to listOf(album("1", wanted = true), album("2")))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideSingles = true))

        assertEquals(listOf("1"), ids(result, single))
    }

    // Formats

    @Test
    fun `a hidden format is left out of the album's formats`() {
        val input = grouped(studio to listOf(album("1", listOf("LP", "CD", "Digital"))))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideDigital = true))

        assertEquals(listOf("LP", "CD"), result.shown.getValue(studio).single().formats)
        assertEquals(0, result.hiddenCount)
    }

    @Test
    fun `an album only on a hidden format is hidden`() {
        val input = grouped(studio to listOf(album("1", listOf("Digital")), album("2", listOf("LP"))))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideDigital = true))

        assertEquals(listOf("2"), ids(result, studio))
        assertEquals(1, result.hiddenCount)
    }

    @Test
    fun `an album is hidden when every format it came out on is hidden`() {
        val input = grouped(studio to listOf(album("1", listOf("CD", "Digital")), album("2", listOf("LP", "CD"))))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideCds = true, hideDigital = true))

        assertEquals(listOf("2"), ids(result, studio))
        assertEquals(listOf("LP"), result.shown.getValue(studio).single().formats)
    }

    @Test
    fun `an album that is on a format that is not hidden is shown, however many are`() {
        val input = grouped(studio to listOf(album("1", listOf("CD", "Digital", "MC"))))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideCds = true, hideDigital = true))

        assertEquals(listOf("MC"), result.shown.getValue(studio).single().formats)
    }

    @Test
    fun `each format has its own setting`() {
        fun hiddenBy(settings: Settings, label: String): Boolean {
            val result = DiscographyVisibility.apply(grouped(studio to listOf(album("1", listOf(label)))), settings)
            return result.hiddenCount == 1
        }
        val none = Settings.ShowEverything

        assertEquals(true, hiddenBy(none.copy(hideLps = true), "LP"))
        assertEquals(true, hiddenBy(none.copy(hideCds = true), "CD"))
        assertEquals(true, hiddenBy(none.copy(hideTwelveInches = true), "12\""))
        assertEquals(true, hiddenBy(none.copy(hideSevenInches = true), "7\""))
        assertEquals(true, hiddenBy(none.copy(hideMcs = true), "MC"))
        assertEquals(true, hiddenBy(none.copy(hideDigital = true), "Digital"))
    }

    @Test
    fun `a setting hides its own format and no other`() {
        fun hiddenBy(settings: Settings, label: String): Boolean {
            val result = DiscographyVisibility.apply(grouped(studio to listOf(album("1", listOf(label)))), settings)
            return result.hiddenCount == 1
        }

        assertFalse(hiddenBy(Settings.ShowEverything.copy(hideLps = true), "CD"))
        assertFalse(hiddenBy(Settings.ShowEverything.copy(hideCds = true), "LP"))
        assertFalse(hiddenBy(Settings.ShowEverything.copy(hideTwelveInches = true), "7\""))
        assertFalse(hiddenBy(Settings.ShowEverything.copy(hideSevenInches = true), "12\""))
        assertFalse(hiddenBy(Settings.ShowEverything.copy(hideMcs = true), "CD"))
        assertFalse(hiddenBy(Settings.ShowEverything.copy(hideCds = true), "MC"))
    }

    @Test
    fun `formats that cannot be hidden are never hidden`() {
        val input = grouped(studio to listOf(album("1", listOf("10\""))))
        val everyFormat = Settings.ShowEverything.copy(
            hideLps = true, hideCds = true, hideTwelveInches = true, hideSevenInches = true, hideMcs = true,
            hideDigital = true
        )

        val result = DiscographyVisibility.apply(input, everyFormat)

        assertEquals(listOf("1"), ids(result, studio))
    }

    @Test
    fun `an album whose formats are not known yet is shown, with no formats`() {
        val input = grouped(studio to listOf(album("1", formats = null)))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideLps = true, hideCds = true, hideDigital = true))

        assertNull(result.shown.getValue(studio).single().formats)
    }

    @Test
    fun `an album that was checked and has no known formats is shown`() {
        val input = grouped(studio to listOf(album("1", formats = emptyList())))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideLps = true, hideDigital = true))

        assertEquals(listOf("1"), ids(result, studio))
    }

    @Test
    fun `an album you own is shown even when all its formats are hidden, and keeps its formats`() {
        val input = grouped(studio to listOf(album("1", listOf("Digital"), owned = true)))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideDigital = true))

        assertEquals(listOf("1"), ids(result, studio))
        assertEquals(listOf("Digital"), result.shown.getValue(studio).single().formats)
        assertEquals(0, result.hiddenCount)
    }

    @Test
    fun `an album you want is shown even when all its formats are hidden`() {
        val input = grouped(studio to listOf(album("1", listOf("CD"), wanted = true)))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideCds = true))

        assertEquals(listOf("1"), ids(result, studio))
    }

    // The count, the order and the empty categories

    @Test
    fun `an album hidden for two reasons is counted once`() {
        val input = grouped(single to listOf(album("1", listOf("Digital"))))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideSingles = true, hideDigital = true))

        assertEquals(1, result.hiddenCount)
    }

    @Test
    fun `a category with nothing left is not in the result`() {
        val input = grouped(live to listOf(album("1")), studio to listOf(album("2")))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideLiveAlbums = true))

        assertEquals(setOf(studio), result.shown.keys)
    }

    @Test
    fun `keeps the order of the albums`() {
        val input = grouped(studio to listOf(album("3"), album("1", listOf("Digital")), album("2"), album("4")))

        val result = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideDigital = true))

        assertEquals(listOf("3", "2", "4"), ids(result, studio))
    }

    @Test
    fun `keeps everything else about an album`() {
        val input = grouped(studio to listOf(AlbumUiModel("1", "Foxtrot", owned = true, wanted = false, formats = listOf("LP"), formatsStale = true)))

        val shown = DiscographyVisibility.apply(input, Settings.ShowEverything.copy(hideDigital = true)).shown.getValue(studio).single()

        assertEquals(AlbumUiModel("1", "Foxtrot", owned = true, wanted = false, formats = listOf("LP"), formatsStale = true), shown)
    }

    // What a row shows when formats come in one at a time

    @Test
    fun `display formats leave out the hidden ones`() {
        val settings = Settings.ShowEverything.copy(hideDigital = true, hideCds = true)

        assertEquals(listOf("LP"), DiscographyVisibility.displayFormats(listOf("LP", "CD", "Digital"), settings))
    }

    @Test
    fun `display formats keep all of them when every one is hidden, so a row you own is not blank`() {
        val settings = Settings.ShowEverything.copy(hideDigital = true)

        assertEquals(listOf("Digital"), DiscographyVisibility.displayFormats(listOf("Digital"), settings))
    }

    @Test
    fun `display formats of an album with none are none`() {
        assertEquals(emptyList<String>(), DiscographyVisibility.displayFormats(emptyList(), Settings.ShowEverything))
    }
}
