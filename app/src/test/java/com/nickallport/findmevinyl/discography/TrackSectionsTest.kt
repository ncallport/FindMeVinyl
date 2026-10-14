package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackSectionsTest {

    private fun track(number: String?, title: String = "t", length: Long? = null) = Track(number, title, length)

    @Test
    fun `splits a vinyl disc into its sides`() {
        val disc = Disc(null, "12\" Vinyl", listOf(track("A1", "a1"), track("A2", "a2"), track("B1", "b1"), track("B2", "b2")))

        val sections = TrackSections.of(disc, multipleDiscs = false)

        assertEquals(listOf("Side A", "Side B"), sections.map { it.title })
        assertEquals(listOf(listOf("a1", "a2"), listOf("b1", "b2")), sections.map { s -> s.tracks.map { it.title } })
    }

    @Test
    fun `sides can have more than one letter and track numbers can be just a letter`() {
        val disc = Disc(null, "12\" Vinyl", listOf(track("A", "one"), track("B", "two"), track("C1", "three"), track("D2", "four")))

        assertEquals(listOf("Side A", "Side B", "Side C", "Side D"), TrackSections.of(disc, false).map { it.title })
    }

    @Test
    fun `keeps the order the sides first appear in`() {
        val disc = Disc(null, "12\" Vinyl", listOf(track("B1"), track("A1")))

        assertEquals(listOf("Side B", "Side A"), TrackSections.of(disc, false).map { it.title })
    }

    @Test
    fun `a disc with plain numbers is one section with no title`() {
        val disc = Disc(null, "CD", listOf(track("1"), track("2"), track("3")))

        val sections = TrackSections.of(disc, multipleDiscs = false)

        assertEquals(1, sections.size)
        assertNull(sections.single().title)
        assertEquals(3, sections.single().tracks.size)
    }

    @Test
    fun `a CD in a set is titled with its number`() {
        val disc = Disc(null, "CD", listOf(track("1")))

        assertEquals("Disc 2", TrackSections.of(disc, multipleDiscs = true, discNumber = 2).single().title)
    }

    @Test
    fun `a disc in a set uses its own title when it has one`() {
        val disc = Disc("Live in Paris", "CD", listOf(track("1")))

        assertEquals("Disc 2: Live in Paris", TrackSections.of(disc, true, discNumber = 2).single().title)
    }

    @Test
    fun `the sides of a disc in a set say which disc they are on`() {
        val disc = Disc(null, "12\" Vinyl", listOf(track("A1"), track("B1")))

        assertEquals(
            listOf("Disc 2, Side A", "Disc 2, Side B"),
            TrackSections.of(disc, multipleDiscs = true, discNumber = 2).map { it.title }
        )
    }

    @Test
    fun `a single disc with its own title shows it`() {
        val disc = Disc("The Early Years", "CD", listOf(track("1")))

        assertEquals("The Early Years", TrackSections.of(disc, multipleDiscs = false).single().title)
    }

    @Test
    fun `tracks with no numbers are one section`() {
        val disc = Disc(null, "CD", listOf(track(null), track(null)))

        assertEquals(1, TrackSections.of(disc, false).size)
    }

    @Test
    fun `mixed numbering is not split into sides`() {
        val disc = Disc(null, "CD", listOf(track("A1"), track("2"), track("3")))

        assertEquals(1, TrackSections.of(disc, false).size)
    }

    @Test
    fun `adds up the length of a section when every track has one`() {
        val disc = Disc(null, "CD", listOf(track("1", length = 60_000), track("2", length = 125_000)))

        assertEquals("3:05", TrackSections.of(disc, false).single().totalText())
    }

    @Test
    fun `gives no total when a track length is not known`() {
        val disc = Disc(null, "CD", listOf(track("1", length = 60_000), track("2", length = null)))

        assertNull(TrackSections.of(disc, false).single().totalText())
    }

    @Test
    fun `gives no sections for a disc with no tracks`() {
        assertEquals(emptyList<TrackSection>(), TrackSections.of(Disc(null, "CD", emptyList()), false))
    }
}
