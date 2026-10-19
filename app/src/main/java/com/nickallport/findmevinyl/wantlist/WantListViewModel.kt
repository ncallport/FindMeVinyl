package com.nickallport.findmevinyl.wantlist

import com.nickallport.findmevinyl.discography.ReleaseDates
import com.nickallport.findmevinyl.export.ShareOptions
import com.nickallport.findmevinyl.export.WantListFormatter
import com.nickallport.findmevinyl.export.WantedAlbumExportItem
import com.nickallport.findmevinyl.ordering.Reorder
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.WantedAlbumRow
import kotlin.comparisons.nullsLast

data class WantedAlbumUiModel(
    val albumId: String,
    val artistName: String,
    val albumTitle: String,
    /** The format it is wanted on (a label such as LP), or null for any. */
    val wantFormat: String? = null
)

/**
 * The want list is one list in the user's own order, whoever the albums are by. What has not been
 * ordered yet (equal positions) keeps an older order: release date with undated last, then title. This
 * is also the order of the share text.
 */
class WantListViewModel(private val albumDao: AlbumDao) {
    suspend fun loadWantList(): List<WantedAlbumUiModel> {
        return ordered().map { WantedAlbumUiModel(it.albumId, it.artistName, it.albumTitle, it.wantFormat) }
    }

    /** Takes an album off the want list. The album itself stays, and shows as not wanted in its discography. */
    suspend fun remove(albumId: String) {
        albumDao.setWanted(albumId, false)
    }

    suspend fun moveUp(albumId: String) = move(albumId, -1)

    suspend fun moveDown(albumId: String) = move(albumId, 1)

    /** The share text: the whole list unless [options] say otherwise, in the user's own order. */
    suspend fun exportText(options: ShareOptions = ShareOptions()): String {
        val items = loadWantList().map { WantedAlbumExportItem(it.artistName, it.albumTitle, it.wantFormat) }
        return WantListFormatter.format(items, options.intro, options.limit)
    }

    private suspend fun ordered(): List<WantedAlbumRow> {
        return albumDao.getWantedAlbums()
            .sortedWith(
                compareBy<WantedAlbumRow> { it.wantPosition }
                    .thenBy(nullsLast<String>()) { ReleaseDates.sortKey(it.firstReleaseDate) }
                    .thenBy { it.albumTitle }
                    .thenBy { it.albumId }
            )
    }

    // An album moves a place in the whole list. Every album is given its own position, so albums that
    // were never ordered stop being ties.
    private suspend fun move(albumId: String, delta: Int) {
        val current = ordered()
        val moved = Reorder.swapped(current.map { it.albumId }, albumId, delta) ?: return
        val positions = current.associate { it.albumId to it.wantPosition }
        moved.forEachIndexed { index, id ->
            if (positions[id] != index) albumDao.setWantPosition(id, index)
        }
    }
}
