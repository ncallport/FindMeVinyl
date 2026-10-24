package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.favourites.SpecialArtists
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzTrackArtistsResponse
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.AlbumTrackArtistEntity
import kotlinx.coroutines.CancellationException

/**
 * Reads the artists on the tracklist of each owned or wanted Various Artists album, once, so the album can be listed on each
 * artist's page. One official release of the album is asked for, with its tracks and who is credited on each (any release when
 * none is official), in its own turn of the shared throttle. An album MusicBrainz has no releases for is marked read too, or it
 * would be asked about for ever. A failed request stops the run and changes nothing, so the next run asks again.
 */
class AppearsOnChecker(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared
) {
    suspend fun run() {
        while (true) {
            val albumId = albumDao.getUncheckedTrackArtistAlbumIds(SpecialArtists.VARIOUS_ID, 1).firstOrNull() ?: return
            val artists = try {
                trackArtistsOf(albumId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return
            }
            albumDao.deleteTrackArtists(albumId)
            if (artists.isNotEmpty()) albumDao.insertTrackArtists(artists.map { AlbumTrackArtistEntity(albumId, it) })
            albumDao.markTrackArtistsChecked(albumId)
        }
    }

    private suspend fun trackArtistsOf(albumId: String): Set<String> {
        var response = throttle.run { api.getReleaseTrackArtists(albumId, status = "official") }
        if (response.releases.isNullOrEmpty()) response = throttle.run { api.getReleaseTrackArtists(albumId) }
        return artistIdsOf(response)
    }

    private fun artistIdsOf(response: MusicBrainzTrackArtistsResponse): Set<String> =
        response.releases?.firstOrNull()?.media.orEmpty()
            .flatMap { it.tracks.orEmpty() }
            .flatMap { it.artistCredit.orEmpty() }
            .mapNotNull { it.artist?.id }
            .filter { it != SpecialArtists.VARIOUS_ID }
            .toSet()
}
