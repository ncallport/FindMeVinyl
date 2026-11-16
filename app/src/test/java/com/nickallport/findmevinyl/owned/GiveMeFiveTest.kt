package com.nickallport.findmevinyl.owned

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GiveMeFiveTest {

    private val albums = (1..30).map { "album$it" }

    @Test
    fun `picks five different albums from the list`() {
        val picked = GiveMeFive.pick(albums, Random(1))

        assertEquals(5, picked.size)
        assertEquals(5, picked.toSet().size)
        assertTrue(albums.containsAll(picked))
    }

    @Test
    fun `with fewer than five it gives them all`() {
        val picked = GiveMeFive.pick(listOf("a", "b", "c"), Random(1))

        assertEquals(setOf("a", "b", "c"), picked.toSet())
        assertEquals(3, picked.size)
    }

    @Test
    fun `with nothing it gives nothing`() {
        assertEquals(emptyList<String>(), GiveMeFive.pick(emptyList<String>(), Random(1)))
    }

    @Test
    fun `different draws give different selections`() {
        val draws = (1..20).map { GiveMeFive.pick(albums, Random(it)).toSet() }.toSet()

        assertTrue(draws.size > 10)
    }

    @Test
    fun `the same seed gives the same selection`() {
        assertEquals(GiveMeFive.pick(albums, Random(7)), GiveMeFive.pick(albums, Random(7)))
    }

    @Test
    fun `a refresh gives five that are all new when there are enough others`() {
        val first = GiveMeFive.pick(albums, Random(1))

        repeat(50) { seed ->
            val again = GiveMeFive.pick(albums, Random(seed), avoid = first.toSet())
            assertEquals(5, again.size)
            assertEquals(emptySet<String>(), again.toSet().intersect(first.toSet()))
        }
    }

    @Test
    fun `when too few are new the rest are filled in from the ones shown before`() {
        val eight = albums.take(8)
        val first = GiveMeFive.pick(eight, Random(1))

        val again = GiveMeFive.pick(eight, Random(2), avoid = first.toSet())

        assertEquals(5, again.size)
        assertEquals(5, again.toSet().size)
        // The three that were not shown are all in it.
        assertTrue(again.containsAll(eight - first.toSet()))
    }

    @Test
    fun `a refresh of exactly five albums gives the same five`() {
        val five = albums.take(5)

        assertEquals(five.toSet(), GiveMeFive.pick(five, Random(3), avoid = five.toSet()).toSet())
    }

    @Test
    fun `the selection is in a random order, not the order of the list`() {
        val orders = (1..20).map { GiveMeFive.pick(albums.take(5), Random(it)) }.toSet()

        assertNotEquals(1, orders.size)
    }
}
