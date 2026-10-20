package com.nickallport.findmevinyl.wantlist

import com.nickallport.findmevinyl.storage.WantedAlbumRow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The want list starts A to Z by artist and then release date. A want the user has moved is pinned to the place they
 * gave it, and the others fill the places that are left, A to Z, so nothing unmoved is disturbed.
 */
class WantOrderTest {

    private fun row(id: String, artist: String, title: String = "Album $id", date: String? = null, priority: Int? = null) =
        WantedAlbumRow(id, "artist-$artist", artist, title, date, 0, 0, null, priority)

    private fun ids(rows: List<WantedAlbumRow>) = WantOrder.arrange(rows).map { it.albumId }

    @Test
    fun `an empty list stays empty`() {
        assertEquals(emptyList<String>(), ids(emptyList()))
    }

    @Test
    fun `with nothing moved it is A to Z by artist, ignoring capitals`() {
        assertEquals(listOf("2", "3", "1"), ids(listOf(row("1", "roxy music"), row("2", "ABC"), row("3", "Genesis"))))
    }

    @Test
    fun `a leading The is left off the artist, so The Smiths is under S`() {
        val rows = listOf(row("1", "Talk Talk"), row("2", "The Smiths"), row("3", "Simple Minds"))

        assertEquals(listOf("3", "2", "1"), ids(rows))
    }

    @Test
    fun `an artist's albums are by release date, undated last, then by title`() {
        val rows = listOf(
            row("1", "Roxy Music", "Avalon", "1982-05-31"),
            row("2", "Roxy Music", "Undated"),
            row("3", "Roxy Music", "Roxy Music", "1972-06-16"),
            row("4", "Roxy Music", "Another 1982", "1982-05-31")
        )

        assertEquals(listOf("3", "4", "1", "2"), ids(rows))
    }

    @Test
    fun `a month comes before a bare year of the same year`() {
        val rows = listOf(row("1", "A", date = "1972"), row("2", "A", date = "1972-06"))

        assertEquals(listOf("2", "1"), ids(rows))
    }

    @Test
    fun `a want pinned to the top is first, and the others stay A to Z after it`() {
        val rows = listOf(row("a", "Abba"), row("b", "Bowie"), row("c", "Cure", priority = 0), row("d", "Dury"))

        assertEquals(listOf("c", "a", "b", "d"), ids(rows))
    }

    @Test
    fun `a want pinned to a place in the middle holds it, with the others filling the rest A to Z`() {
        val rows = listOf(row("a", "Abba", priority = 3), row("b", "Bowie"), row("c", "Cure"), row("d", "Dury"), row("e", "Eno"))

        assertEquals(listOf("b", "c", "d", "a", "e"), ids(rows))
    }

    @Test
    fun `several pinned wants each keep their place`() {
        val rows = listOf(
            row("a", "Abba"), row("b", "Bowie", priority = 4), row("c", "Cure"),
            row("d", "Dury", priority = 1), row("e", "Eno")
        )

        assertEquals(listOf("a", "d", "c", "e", "b"), ids(rows))
    }

    @Test
    fun `pinned wants are in the order of their places, whatever order they come in`() {
        val rows = listOf(row("x", "Xtc", priority = 1), row("y", "Yello", priority = 0))

        assertEquals(listOf("y", "x"), ids(rows))
    }

    @Test
    fun `a want added later takes its place A to Z among the unmoved, and the pinned ones stay put`() {
        val before = listOf(row("c", "Cure", priority = 0), row("a", "Abba"), row("b", "Bowie"))
        assertEquals(listOf("c", "a", "b"), ids(before))

        val after = before + row("h", "Aha")

        assertEquals(listOf("c", "a", "h", "b"), ids(after))
    }

    @Test
    fun `a place beyond the end of the list, as after wants were removed, goes last`() {
        val rows = listOf(row("a", "Abba", priority = 9), row("b", "Bowie"), row("c", "Cure"))

        assertEquals(listOf("b", "c", "a"), ids(rows))
    }

    @Test
    fun `two wants pinned to the same place do not hide each other`() {
        val rows = listOf(row("a", "Abba", priority = 0), row("b", "Bowie", priority = 0), row("c", "Cure"))

        assertEquals(setOf("a", "b", "c"), ids(rows).toSet())
        assertEquals(3, ids(rows).size)
    }

    @Test
    fun `every want is listed once`() {
        val rows = (1..30).map { row("$it", "Artist ${it % 7}", priority = if (it % 4 == 0) it % 5 else null) }

        val arranged = ids(rows)

        assertEquals(30, arranged.size)
        assertEquals(rows.map { it.albumId }.toSet(), arranged.toSet())
    }
}
