package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseDetailResponse
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumDetailsDao
import com.nickallport.findmevinyl.storage.AlbumDetailsEntity
import kotlinx.coroutines.CancellationException

class AlbumDetailsViewModel(
    private val api: MusicBrainzApi,
    private val detailsDao: AlbumDetailsDao,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshAfterMillis: Long = 90L * 24 * 60 * 60 * 1000
) {
    /**
     * The details page of an album: one pressing's tracks, label and date, and the album's genres.
     * Null means they could not be loaded, and nothing is saved, so it is tried again next time.
     *
     * Details found once are kept and looked up again only after 90 days. An unreadable stored row
     * counts as missing. A new lookup makes two requests, the album's releases and then the chosen
     * one (see [PressingChooser]), both inside one turn in the MusicBrainz queue and a second apart.
     * Once it has started, finding the details and saving them are part of it, so a caller that is
     * cancelled meanwhile still leaves them stored. An album with no releases is saved as having
     * nothing, so it is not asked about again for 90 days.
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

    private suspend fun fetchAndSave(albumId: String): AlbumDetails {
        val releases = api.getReleasesForReleaseGroup(releaseGroupId = albumId).releases.orEmpty()
        val chosenId = PressingChooser.choose(releases)?.id

        val details = if (chosenId == null) {
            AlbumDetails.from(MusicBrainzReleaseDetailResponse())
        } else {
            throttle.awaitNextSlot()
            AlbumDetails.from(api.getReleaseDetail(releaseId = chosenId))
        }
        detailsDao.save(AlbumDetailsEntity(albumId, AlbumDetailsJson.encode(details), clock()))
        return details
    }
}
