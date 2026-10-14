package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatFilterTest {

    private fun album(id: String, formats: List<String>?) =
        AlbumUiModel(id = id, title = "Album $id", owned = false, wanted = false, formats = formats)

    private val studio = DiscographyCategory.STUDIO_ALBUM
    private val singles = DiscographyCategory.SINGLE

    @Test
    fun `lists each format found, in a fixed order, with how many albums have it`() {
        val albums = listOf(
            album("1", listOf("CD", "Digital")),
            album("2", listOf("LP", "CD")),
            album("3", listOf("MC")),
            album("4", listOf("LP"))
        )

        assertEquals(
            listOf(FormatOption("LP", 2), FormatOption("CD", 2), FormatOption("MC", 1), FormatOption("Digital", 1)),
            FormatFilter.options(albums)
        )
    }

    @Test
    fun `puts the vinyl sizes between LP and CD`() {
        val albums = listOf(album("1", listOf("7\"", "CD")), album("2", listOf("12\"", "10\"", "LP")))

        assertEquals(
            listOf("LP", "12\"", "10\"", "7\"", "CD"),
            FormatFilter.options(albums).map { it.label }
        )
    }

    @Test
    fun `leaves out formats no album has, and counts nothing for albums not looked up`() {
        val albums = listOf(album("1", null), album("2", emptyList()), album("3", listOf("CD")))

        assertEquals(listOf(FormatOption("CD", 1)), FormatFilter.options(albums))
        assertEquals(emptyList<FormatOption>(), FormatFilter.options(emptyList()))
    }

    @Test
    fun `counts the albums that have not been looked up, but not those looked up with no formats`() {
        val albums = listOf(album("1", null), album("2", null), album("3", emptyList()), album("4", listOf("CD")))

        assertEquals(2, FormatFilter.unchecked(albums))
        assertEquals(0, FormatFilter.unchecked(emptyList()))
    }

    @Test
    fun `with no format chosen every album stays`() {
        val grouped = mapOf(studio to listOf(album("1", listOf("CD")), album("2", null), album("3", emptyList())))

        assertEquals(grouped, FormatFilter.apply(grouped, selected = null))
    }

    @Test
    fun `keeps the albums that have the format and the ones not looked up, and drops the rest`() {
        val grouped = mapOf(
            studio to listOf(
                album("lp", listOf("LP", "CD")),
                album("cd", listOf("CD")),
                album("unknown", null),
                album("none", emptyList())
            )
        )

        val kept = FormatFilter.apply(grouped, selected = "LP").getValue(studio).map { it.id }

        assertEquals(listOf("lp", "unknown"), kept)
    }

    @Test
    fun `keeps the order of the albums within a category`() {
        val grouped = mapOf(studio to listOf(album("c", listOf("CD")), album("a", listOf("CD")), album("b", listOf("CD"))))

        assertEquals(listOf("c", "a", "b"), FormatFilter.apply(grouped, "CD").getValue(studio).map { it.id })
    }

    @Test
    fun `drops a category that has nothing left`() {
        val grouped = mapOf(
            studio to listOf(album("1", listOf("LP"))),
            singles to listOf(album("2", listOf("CD")))
        )

        assertEquals(setOf(studio), FormatFilter.apply(grouped, "LP").keys)
    }

    @Test
    fun `a format nobody has leaves only the albums not looked up`() {
        val grouped = mapOf(studio to listOf(album("1", listOf("CD")), album("2", null)))

        assertEquals(listOf("2"), FormatFilter.apply(grouped, "MC").getValue(studio).map { it.id })
    }

    @Test
    fun `a format nobody has and nothing left to check gives an empty list`() {
        val grouped = mapOf(studio to listOf(album("1", listOf("CD"))))

        assertEquals(emptyMap<DiscographyCategory, List<AlbumUiModel>>(), FormatFilter.apply(grouped, "MC"))
    }
}
