package com.nickallport.findmevinyl.wantlist

import com.nickallport.findmevinyl.export.WantListFormatter
import com.nickallport.findmevinyl.export.WantedAlbumExportItem
import com.nickallport.findmevinyl.ordering.Reorder
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.WantedAlbumRow
import kotlin.comparisons.nullsLast

data class WantedAlbumUiModel(
    val albumId: String,
    val artistName: String,
    val albumTitle: String
)

/**
 * The want list is in the user's own order: artists as they are ordered in the favourites, and each
 * artist's albums as ordered here. What has not been ordered yet (equal positions) keeps the old
 * order: artists by name, albums by release date with undated last, then title. This is also the
 * order of the share text.
 */
class WantListViewModel(private val albumDao: AlbumDao) {
    suspend fun loadWantList(): List<WantedAlbumUiModel> {
        return ordered().map { WantedAlbumUiModel(it.albumId, it.artistName, it.albumTitle) }
    }

    suspend fun moveUp(albumId: String) = move(albumId, -1)

    suspend fun moveDown(albumId: String) = move(albumId, 1)

    suspend fun exportText(): String {
        val items = loadWantList().map { WantedAlbumExportItem(it.artistName, it.albumTitle) }
        return WantListFormatter.format(items)
    }

    private suspend fun ordered(): List<WantedAlbumRow> {
        return albumDao.getWantedAlbums()
            .sortedWith(
                compareBy<WantedAlbumRow> { it.artistPosition }
                    .thenBy { it.artistName.lowercase() }
                    .thenBy { it.artistId }
                    .thenBy { it.wantPosition }
                    .thenBy(nullsLast<String>()) { it.firstReleaseDate }
                    .thenBy { it.albumTitle }
            )
    }

    // An album moves among the wanted albums of its own artist. Every one of them is given its own
    // position, so albums that were never ordered stop being ties.
    private suspend fun move(albumId: String, delta: Int) {
        val all = ordered()
        val artistId = all.firstOrNull { it.albumId == albumId }?.artistId ?: return
        val current = all.filter { it.artistId == artistId }
        val moved = Reorder.swapped(current.map { it.albumId }, albumId, delta) ?: return
        val positions = current.associate { it.albumId to it.wantPosition }
        moved.forEachIndexed { index, id ->
            if (positions[id] != index) albumDao.setWantPosition(id, index)
        }
    }
}
