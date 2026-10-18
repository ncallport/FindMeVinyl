package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap
import com.nickallport.findmevinyl.favourites.ArtistGroupViewModel

/**
 * The picture shown for an artist: the photo the user chose if there is one, otherwise the picture
 * from Wikimedia Commons. When the user has chosen a photo the web is not asked at all.
 *
 * A group of artists shows the photo the user chose for the group, and otherwise the picture of its first
 * member ([firstMemberOf] says who that is). The web is never asked about a group's own id, which is no
 * MusicBrainz artist.
 */
class ArtistPictureSource(
    private val userPhoto: suspend (artistId: String) -> Bitmap?,
    private val webPicture: suspend (artistId: String) -> Bitmap?,
    private val firstMemberOf: suspend (groupId: String) -> String? = { null }
) {
    suspend fun load(artistId: String): Bitmap? {
        if (!ArtistGroupViewModel.isGroupId(artistId)) return userPhoto(artistId) ?: webPicture(artistId)
        userPhoto(artistId)?.let { return it }
        val first = firstMemberOf(artistId) ?: return null
        return userPhoto(first) ?: webPicture(first)
    }
}
