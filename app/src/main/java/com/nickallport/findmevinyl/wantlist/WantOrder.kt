package com.nickallport.findmevinyl.wantlist

import com.nickallport.findmevinyl.discography.ReleaseDates
import com.nickallport.findmevinyl.favourites.NameSort
import com.nickallport.findmevinyl.storage.WantedAlbumRow

/**
 * The order of the want list. To begin with it is A to Z by artist ([NameSort]) and then release date, undated last, then
 * title. A want the user has moved is pinned to the place they gave it ([WantedAlbumRow.wantPriority], counting from 0
 * at the top), and the wants not moved fill the places that are left, A to Z, so nothing unmoved is disturbed. A pin
 * beyond the end of the list, as after wants were taken off, or one already taken, goes last.
 */
object WantOrder {
    private val looseOrder: Comparator<WantedAlbumRow> =
        compareBy<WantedAlbumRow, String>(NameSort) { it.artistName }
            .thenBy(nullsLast<String>()) { ReleaseDates.sortKey(it.firstReleaseDate) }
            .thenBy { it.albumTitle }
            .thenBy { it.albumId }

    fun arrange(rows: List<WantedAlbumRow>): List<WantedAlbumRow> {
        val size = rows.size
        val slots = arrayOfNulls<WantedAlbumRow>(size)
        val extra = mutableListOf<WantedAlbumRow>()
        for (pinned in rows.filter { it.wantPriority != null }.sortedWith(compareBy<WantedAlbumRow> { it.wantPriority }.thenBy { it.albumId })) {
            val place = pinned.wantPriority!!
            if (place in 0 until size && slots[place] == null) slots[place] = pinned else extra.add(pinned)
        }
        val loose = rows.filter { it.wantPriority == null }.sortedWith(looseOrder).iterator()
        for (place in 0 until size) {
            if (slots[place] == null && loose.hasNext()) slots[place] = loose.next()
        }
        val rest = extra.iterator()
        for (place in 0 until size) {
            if (slots[place] == null && rest.hasNext()) slots[place] = rest.next()
        }
        return slots.map { it!! }
    }
}
