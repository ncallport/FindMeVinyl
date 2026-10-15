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
        ),
        pressings = listOf(
            Pressing("r1", "1970-10", "GB", "Charisma", "CAS 1020", "12\" Vinyl"),
            Pressing("r2", null, null, null, null, null)
        ),
        pressingId = "r1",
        rating = Rating(4.3, 27)
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
        val old = """{ "version": 3, "releaseDate": "1970", "label": "Charisma" }"""

        val read = AlbumDetailsJson.decode(old)!!

        assertEquals("Charisma", read.label)
        assertEquals(emptyList<String>(), read.genres)
        assertEquals(emptyList<Disc>(), read.discs)
        assertEquals(emptyList<Pressing>(), read.pressings)
        assertNull(read.pressingId)
        assertNull(read.rating)
    }

    @Test
    fun `a row stored before pressings and ratings were kept reads as missing, so it is fetched again`() {
        val before = """{ "releaseDate": "1970", "label": "Charisma", "genres": ["rock"], "discs": [] }"""

        assertNull(AlbumDetailsJson.decode(before))
    }

    @Test
    fun `a row stored with pressings in the old date order reads as missing, so they are sorted again`() {
        assertNull(AlbumDetailsJson.decode("""{ "version": 2, "pressings": [ { "releaseId": "r1" } ] }"""))
    }

    @Test
    fun `a row from a newer version of the app also reads as missing`() {
        assertNull(AlbumDetailsJson.decode("""{ "version": 4, "label": "Charisma" }"""))
    }

    @Test
    fun `leaves out a stored pressing that has no release id and a rating that makes no sense`() {
        val odd = """{ "version": 3, "pressings": [ { "country": "GB" }, null, { "releaseId": "r1" } ],
                       "rating": { "value": 0, "votes": 0 } }"""

        val read = AlbumDetailsJson.decode(odd)!!

        assertEquals(listOf("r1"), read.pressings.map { it.releaseId })
        assertNull(read.rating)
    }

    @Test
    fun `reads a disc that has no tracks and a track that has no title`() {
        val odd = """{ "version": 3, "discs": [ { "format": "CD" }, { "tracks": [ { "number": "1" } ] } ] }"""

        val read = AlbumDetailsJson.decode(odd)!!

        assertEquals(emptyList<Track>(), read.discs[0].tracks)
        assertEquals("Untitled", read.discs[1].tracks.single().title)
    }

    @Test
    fun `ignores fields it does not know about`() {
        val newer = """{ "version": 3, "label": "Charisma", "somethingNew": [1, 2, 3] }"""

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
