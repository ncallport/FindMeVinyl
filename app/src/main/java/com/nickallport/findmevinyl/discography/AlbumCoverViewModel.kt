package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nickallport.findmevinyl.artist.ArtistPhotoStore
import com.nickallport.findmevinyl.artist.PhotoScaler
import com.nickallport.findmevinyl.artist.safeFileName
import com.nickallport.findmevinyl.storage.AlbumDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The cover images the user chooses for albums they added by hand, kept on the phone. An image is scaled down
 * ([PhotoScaler]) and kept in a file of its own, and the album records which file. Albums from MusicBrainz have
 * their covers from the Cover Art Archive and take none. Each image gets a new file name, so a picture shown from
 * the old file is not mistaken for the new, and the old file is deleted.
 */
class AlbumCoverViewModel(
    private val albumDao: AlbumDao,
    private val store: ArtistPhotoStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    /** Keeps [imageBytes] as the cover. False when the album is not one added by hand, or the bytes are not a picture. */
    suspend fun saveCover(albumId: String, imageBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (!CustomAlbums.isCustom(albumId)) return@withContext false
        try {
            val album = albumDao.getById(albumId) ?: return@withContext false
            val scaled = PhotoScaler.scale(imageBytes) ?: return@withContext false
            val fileName = "${safeFileName(albumId)}-${clock()}.jpg"
            store.write(fileName, scaled)
            albumDao.setUserCover(albumId, fileName)
            if (album.userCover != null && album.userCover != fileName) store.delete(album.userCover)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** The cover, or null when there is none or its file is missing or damaged. */
    suspend fun loadCover(albumId: String): Bitmap? = withContext(Dispatchers.IO) {
        val fileName = albumDao.getById(albumId)?.userCover ?: return@withContext null
        val bytes = store.read(fileName) ?: return@withContext null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /** Takes the cover away and deletes its file. False when the album has none. */
    suspend fun removeCover(albumId: String): Boolean = withContext(Dispatchers.IO) {
        val fileName = albumDao.getById(albumId)?.takeIf { CustomAlbums.isCustom(it.id) }?.userCover ?: return@withContext false
        albumDao.setUserCover(albumId, null)
        store.delete(fileName)
        true
    }
}
