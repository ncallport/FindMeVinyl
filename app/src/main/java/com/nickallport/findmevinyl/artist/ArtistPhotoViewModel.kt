package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nickallport.findmevinyl.storage.ArtistUserPhotoDao
import com.nickallport.findmevinyl.storage.ArtistUserPhotoEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The photos the user picks for artists, kept on the phone. A photo is scaled down ([PhotoScaler]) and
 * kept in a file of its own, with a record of which file belongs to which artist. It is shown in place
 * of the picture from Wikimedia Commons, which is not touched, so removing the photo brings that back.
 */
class ArtistPhotoViewModel(
    private val photoDao: ArtistUserPhotoDao,
    private val store: ArtistPhotoStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    /**
     * Keeps [imageBytes] as the photo of the artist, in place of any photo kept before. False when the
     * bytes are not a picture or the file cannot be written, and then an earlier photo is left as it is.
     * Each photo gets a new file name, so a picture shown from the old file is not mistaken for the new.
     */
    suspend fun savePhoto(artistId: String, imageBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val scaled = PhotoScaler.scale(imageBytes) ?: return@withContext false
            val now = clock()
            val fileName = "${safeName(artistId)}-$now.jpg"
            val old = photoDao.get(artistId)

            store.write(fileName, scaled)
            photoDao.save(ArtistUserPhotoEntity(artistId, fileName, now))
            if (old != null && old.fileName != fileName) store.delete(old.fileName)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** The artist's photo, or null when there is none or its file is missing or damaged. */
    suspend fun loadPhoto(artistId: String): Bitmap? = withContext(Dispatchers.IO) {
        val stored = photoDao.get(artistId) ?: return@withContext null
        val bytes = store.read(stored.fileName) ?: return@withContext null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun safeName(artistId: String): String = safeFileName(artistId)
}
