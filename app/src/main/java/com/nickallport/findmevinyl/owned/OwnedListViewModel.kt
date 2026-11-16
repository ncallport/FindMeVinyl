package com.nickallport.findmevinyl.owned

import com.nickallport.findmevinyl.discography.ReleaseDates
import com.nickallport.findmevinyl.export.OwnedAlbumExportItem
import com.nickallport.findmevinyl.export.OwnedListFormatter
import com.nickallport.findmevinyl.favourites.NameSort
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.OwnedAlbumRow
import com.nickallport.findmevinyl.ui.discography.AlbumCopy

/** One owned album on the Owned list. [artistId] is what the album's page is opened with, [year] the year it came out. */
data class OwnedAlbumUiModel(
    val albumId: String,
    val artistId: String,
    val artistName: String,
    val title: String,
    val year: String? = null,
    /** The copies the user has, in the order they were listed. */
    val copies: List<AlbumCopy> = emptyList()
)

/**
 * The complete Owned list: every album the user owns, whoever it is by, A to Z by artist ([NameSort]), then release date
 * (undated last), then title. The share text is in the same order.
 */
class OwnedListViewModel(private val albumDao: AlbumDao) {

    private val order: Comparator<OwnedAlbumRow> =
        compareBy<OwnedAlbumRow, String>(NameSort) { it.artistName }
            .thenBy(nullsLast<String>()) { ReleaseDates.sortKey(it.firstReleaseDate) }
            .thenBy { it.albumTitle }
            .thenBy { it.albumId }

    suspend fun load(): List<OwnedAlbumUiModel> {
        val copies = albumDao.getAllCopies().groupBy { it.albumId }
        return albumDao.getOwnedAlbums().sortedWith(order).map { row ->
            OwnedAlbumUiModel(
                albumId = row.albumId,
                artistId = row.artistId,
                artistName = row.artistName,
                title = row.albumTitle,
                year = row.firstReleaseDate?.take(4)?.takeIf { year -> year.length == 4 && year.all { it.isDigit() } },
                copies = copies[row.albumId].orEmpty().map { AlbumCopy(it.format, it.note) }
            )
        }
    }

    /** The whole list as text to share, in the order shown. */
    suspend fun exportText(): String =
        OwnedListFormatter.format(load().map { album -> OwnedAlbumExportItem(album.artistName, album.title, album.copies.map { it.format }) })
}
