package com.nickallport.findmevinyl.discography

import android.content.SharedPreferences

/**
 * How one artist's discography is grouped: the albums you [owned] and the ones you [wanted] can each be taken
 * out of their categories into a section of their own at the top. With both on, [wantedFirst] says which
 * section comes first (Owned by default). The order is only about the two sections, so it is kept as it is
 * while one of them is switched off.
 */
data class Grouping(
    val owned: Boolean = false,
    val wanted: Boolean = false,
    val wantedFirst: Boolean = false
) {
    companion object {
        val None = Grouping()
    }
}

/**
 * A [Grouping] as text: the words `owned`, `wanted` and `wantedFirst` separated by commas. It is how the store
 * keeps it, and how a screen keeps it over a rotation. Words it does not know are ignored.
 */
object GroupingText {
    fun write(grouping: Grouping): String = listOfNotNull(
        "owned".takeIf { grouping.owned },
        "wanted".takeIf { grouping.wanted },
        "wantedFirst".takeIf { grouping.wantedFirst }
    ).joinToString(",")

    fun read(text: String): Grouping {
        val words = text.split(',').map { it.trim() }.toSet()
        return Grouping(owned = "owned" in words, wanted = "wanted" in words, wantedFirst = "wantedFirst" in words)
    }
}

/**
 * Remembers each artist's [Grouping]. It is kept in the file the old Owned first switch used, so an artist
 * that had it on keeps it: the old value was a true boolean, which reads as owned. Artists with no group on
 * are not kept.
 */
class GroupingStore(private val prefs: SharedPreferences) {

    fun get(artistId: String): Grouping {
        val stored = try {
            prefs.all[artistId]
        } catch (e: Exception) {
            null
        }
        return when (stored) {
            true -> Grouping(owned = true)
            is String -> GroupingText.read(stored).let { if (it.owned || it.wanted) it else Grouping.None }
            else -> Grouping.None
        }
    }

    fun set(artistId: String, grouping: Grouping) {
        prefs.edit().apply {
            if (grouping.owned || grouping.wanted) putString(artistId, GroupingText.write(grouping)) else remove(artistId)
        }.apply()
    }
}
