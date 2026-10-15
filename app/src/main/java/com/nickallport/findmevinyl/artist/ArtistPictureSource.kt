package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap

/**
 * The picture shown for an artist: the photo the user chose if there is one, otherwise the picture
 * from Wikimedia Commons. When the user has chosen a photo the web is not asked at all.
 */
class ArtistPictureSource(
    private val userPhoto: suspend (artistId: String) -> Bitmap?,
    private val webPicture: suspend (artistId: String) -> Bitmap?
) {
    suspend fun load(artistId: String): Bitmap? = userPhoto(artistId) ?: webPicture(artistId)
}
