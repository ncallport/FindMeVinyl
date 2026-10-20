package com.nickallport.findmevinyl.wantlist

import com.nickallport.findmevinyl.export.ShareOptions
import com.nickallport.findmevinyl.export.WantListFormatter
import com.nickallport.findmevinyl.export.WantedAlbumExportItem
import com.nickallport.findmevinyl.ordering.Reorder
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.WantedAlbumRow

data class WantedAlbumUiModel(
    val albumId: String,
    val artistName: String,
    val albumTitle: String,
    /** The format it is wanted on (a label such as LP), or null for any. */
    val wantFormat: String? = null
)

/**
 * The want list is one list, whoever the albums are by. It starts A to Z by artist and then release date, and a want the
 * user moves is pinned to the place they put it while the others stay A to Z around it ([WantOrder]). The share text is
 * in the same order.
 */
class WantListViewModel(private val albumDao: AlbumDao) {
    suspend fun loadWantList(): List<WantedAlbumUiModel> {
        return ordered().map { WantedAlbumUiModel(it.albumId, it.artistName, it.albumTitle, it.wantFormat) }
    }

    suspend fun moveUp(albumId: String) = move(albumId, -1)

    suspend fun moveDown(albumId: String) = move(albumId, 1)

    /** Sends the want to the top of the list, which pins it there. The others keep their order below it. */
    suspend fun moveToTop(albumId: String) {
        val current = ordered()
        val at = current.indexOfFirst { it.albumId == albumId }
        if (at <= 0) return
        pin(current, listOf(current[at]) + current.filterIndexed { i, _ -> i != at }, albumId)
    }

    /** The share text: the whole list unless [options] say otherwise, in the order shown. */
    suspend fun exportText(options: ShareOptions = ShareOptions()): String {
        val items = ordered().map { WantedAlbumExportItem(it.artistName, it.albumTitle, it.wantFormat) }
        return WantListFormatter.format(items, options.intro, options.limit)
    }

    private suspend fun ordered(): List<WantedAlbumRow> = WantOrder.arrange(albumDao.getWantedAlbums())

    private suspend fun move(albumId: String, delta: Int) {
        val current = ordered()
        val at = current.indexOfFirst { it.albumId == albumId }
        val to = at + delta
        if (at < 0 || to !in current.indices) return
        pin(current, Reorder.swappedAt(current, at, delta) ?: return, albumId)
    }

    // The list as it should now be shown: the want that was moved and every want already pinned are pinned to the
    // place they have in it, and the rest are left to fall A to Z around them, which gives the same list because their
    // order among themselves has not changed.
    private suspend fun pin(before: List<WantedAlbumRow>, after: List<WantedAlbumRow>, movedId: String) {
        val toPin = before.filter { it.wantPriority != null }.map { it.albumId }.toSet() + movedId
        val previous = before.associate { it.albumId to it.wantPriority }
        after.forEachIndexed { place, row ->
            if (row.albumId in toPin && previous[row.albumId] != place) albumDao.setWantPriority(row.albumId, place)
        }
    }
}
