package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumDetailsJsonTest {

    private val details = AlbumDetails(
        releaseDate = "1970-10", country = "GB", label = "Charisma", catalogNumber = "CAS 1020",
        firstReleaseDate = "1970-10-23", format = "12\" Vinyl",
        genres = listOf("rock", "progressive rock"),
        discs = listOf(
            Disc(
                title = null, format = "12\" Vinyl",
                tracks = listOf(Track("A1", "Looking for Someone", 424000), Track("B1", "Stagnation, \"live\" ä", null))
            )
        )
    )

    @Test
    fun `stores and reads back everything`() {
        assertEquals(details, AlbumDetailsJson.decode(AlbumDetailsJson.encode(details)))
    }

    @Test
    fun `stores and reads back an album with nothing known`() {
        val empty = AlbumDetails(null, null, null, null, null, null, emptyList(), emptyList())

        assertEquals(empty, AlbumDetailsJson.decode(AlbumDetailsJson.encode(empty)))
    }

    @Test
    fun `reads a stored row that has no genres or discs as empty lists`() {
        val old = """{ "releaseDate": "1970", "label": "Charisma" }"""

        val read = AlbumDetailsJson.decode(old)!!

        assertEquals("Charisma", read.label)
        assertEquals(emptyList<String>(), read.genres)
        assertEquals(emptyList<Disc>(), read.discs)
    }

    @Test
    fun `reads a disc that has no tracks and a track that has no title`() {
        val odd = """{ "discs": [ { "format": "CD" }, { "tracks": [ { "number": "1" } ] } ] }"""

        val read = AlbumDetailsJson.decode(odd)!!

        assertEquals(emptyList<Track>(), read.discs[0].tracks)
        assertEquals("Untitled", read.discs[1].tracks.single().title)
    }

    @Test
    fun `ignores fields it does not know about`() {
        val newer = """{ "label": "Charisma", "somethingNew": [1, 2, 3] }"""

        assertEquals("Charisma", AlbumDetailsJson.decode(newer)?.label)
    }

    @Test
    fun `gives nothing for text that is not stored details`() {
        assertNull(AlbumDetailsJson.decode(""))
        assertNull(AlbumDetailsJson.decode("   "))
        assertNull(AlbumDetailsJson.decode("not json"))
        assertNull(AlbumDetailsJson.decode("[1, 2"))
        assertNull(AlbumDetailsJson.decode("null"))
    }
}
