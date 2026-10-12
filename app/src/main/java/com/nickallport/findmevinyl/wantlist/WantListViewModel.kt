package com.nickallport.findmevinyl.wantlist

import com.nickallport.findmevinyl.export.WantListFormatter
import com.nickallport.findmevinyl.export.WantedAlbumExportItem
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.WantedAlbumRow
import kotlin.comparisons.nullsLast

data class WantedAlbumUiModel(
    val albumId: String,
    val artistName: String,
    val albumTitle: String
)

class WantListViewModel(private val albumDao: AlbumDao) {
    suspend fun loadWantList(): List<WantedAlbumUiModel> {
        return albumDao.getWantedAlbums()
            .sortedWith(
                compareBy<WantedAlbumRow> { it.artistName.lowercase() }
                    .thenBy(nullsLast<String>()) { it.firstReleaseDate }
                    .thenBy { it.albumTitle }
            )
            .map { WantedAlbumUiModel(it.albumId, it.artistName, it.albumTitle) }
    }

    suspend fun exportText(): String {
        val items = loadWantList().map { WantedAlbumExportItem(it.artistName, it.albumTitle) }
        return WantListFormatter.format(items)
    }
}