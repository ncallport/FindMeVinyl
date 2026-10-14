package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.ArtistImageDao
import com.nickallport.findmevinyl.storage.ArtistImageEntity
import kotlinx.coroutines.CancellationException

class ArtistImageViewModel(
    private val finder: ArtistImageFinder,
    private val imageDao: ArtistImageDao,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshAfterMillis: Long = 90L * 24 * 60 * 60 * 1000
) {
    /**
     * The address of the artist's picture, or null when there is none or it could not be looked up.
     *
     * A picture found once is kept, and so is "this artist has none", so each artist is looked up
     * once and again only after 90 days. A failed lookup is not kept, so it is tried again next time.
     *
     * The lookup waits its turn in the MusicBrainz queue. Once it has started, finding the picture
     * and saving it are part of it, so a caller that is cancelled meanwhile, such as a row that has
     * scrolled away, still leaves the result stored.
     */
    suspend fun loadImageUrl(artistId: String): String? {
        return try {
            val stored = imageDao.get(artistId)
            if (stored != null && clock() - stored.checkedAt <= refreshAfterMillis) {
                stored.imageUrl
            } else {
                throttle.run {
                    val url = finder.pictureUrl(finder.fetchLinks(artistId))
                    imageDao.save(ArtistImageEntity(artistId, url, clock()))
                    url
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
