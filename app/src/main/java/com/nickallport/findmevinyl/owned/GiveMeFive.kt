package com.nickallport.findmevinyl.owned

import kotlin.random.Random

/**
 * Give Me Five: five records chosen at random from the ones the user owns, for when they cannot think what to play.
 * With fewer than five, all of them. A refresh passes the ones just shown as [avoid], so it gives five new ones while
 * there are that many others, and fills the rest from the ones shown before when there are not.
 */
object GiveMeFive {
    const val COUNT = 5

    fun <T> pick(albums: List<T>, random: Random, count: Int = COUNT, avoid: Set<T> = emptySet()): List<T> {
        val fresh = albums.filter { it !in avoid }.shuffled(random)
        if (fresh.size >= count) return fresh.take(count)
        val shownBefore = albums.filter { it in avoid }.shuffled(random)
        return (fresh + shownBefore).take(count).shuffled(random)
    }
}
