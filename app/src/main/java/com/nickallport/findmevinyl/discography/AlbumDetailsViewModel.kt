package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDetailResponse
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumDetailsDao
import com.nickallport.findmevinyl.storage.AlbumDetailsEntity
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

class AlbumDetailsViewModel(
    private val api: MusicBrainzApi,
    private val detailsDao: AlbumDetailsDao,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshAfterMillis: Long = 90L * 24 * 60 * 60 * 1000
) {
    /**
     * The details page of an album: the list of its pressings, one pressing's tracks, label and date,
     * and the album's genres and rating. Null means they could not be loaded, and nothing is saved, so
     * it is tried again next time.
     *
     * Details found once are kept and looked up again only after 90 days. An unreadable or old stored
     * row counts as missing. A new lookup makes three requests, all inside one turn in the MusicBrainz
     * queue and each in its own slot: the album's releases with their labels (the pressings, see
     * [PressingChooser]), the first pressing with its tracks, and the album's rating. Once it has
     * started, finding the details and saving them are part of it, so a caller that is cancelled
     * meanwhile still leaves them stored. An album with no releases is saved as having nothing, so it
     * is not asked about again for 90 days.
     */
    suspend fun load(albumId: String): AlbumDetails? {
        return try {
            val stored = detailsDao.get(albumId)
            val storedDetails = stored?.takeIf { clock() - it.fetchedAt <= refreshAfterMillis }
                ?.let { AlbumDetailsJson.decode(it.json) }
            storedDetails ?: throttle.run { fetchAndSave(albumId) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The album with another of its pressings shown: that release's date, label, format and tracks,
     * and the album's own genres, pressings and rating. One request, the first time a pressing is
     * chosen in a run of the app. The choice is not stored, so the page opens on the first pressing
     * again next time. Null means the album has no stored details, or the request failed, and a later
     * try asks again.
     */
    suspend fun selectPressing(albumId: String, releaseId: String): AlbumDetails? {
        return try {
            val album = detailsDao.get(albumId)?.let { AlbumDetailsJson.decode(it.json) } ?: return null
            if (album.pressingId == releaseId) return album
            val key = "$albumId/$releaseId"
            val release = pressingsSeen[key] ?: throttle.run {
                AlbumDetails.from(api.getReleaseDetail(releaseId = releaseId, include = "recordings+labels"))
                    .copy(pressingId = releaseId)
            }.also { pressingsSeen[key] = it }
            album.withPressing(release)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private val pressingsSeen = ConcurrentHashMap<String, AlbumDetails>()

    private suspend fun fetchAndSave(albumId: String): AlbumDetails {
        val releases = api.getReleasesForReleaseGroup(releaseGroupId = albumId, include = "media+labels").releases.orEmpty()
        val candidates = PressingChooser.candidates(releases)
        val chosenId = candidates.firstOrNull()?.id

        val details = if (chosenId == null) {
            AlbumDetails.from(MusicBrainzReleaseDetailResponse())
        } else {
            throttle.awaitNextSlot()
            val first = AlbumDetails.from(api.getReleaseDetail(releaseId = chosenId))
            throttle.awaitNextSlot()
            val rating = Rating.from(api.getReleaseGroupRating(releaseGroupId = albumId).rating)
            first.copy(
                pressings = candidates.take(MAX_PRESSINGS).mapNotNull { Pressing.from(it) },
                pressingId = chosenId,
                rating = rating
            )
        }
        detailsDao.save(AlbumDetailsEntity(albumId, AlbumDetailsJson.encode(details), clock()))
        return details
    }

    private companion object {
        const val MAX_PRESSINGS = 30
    }
}
