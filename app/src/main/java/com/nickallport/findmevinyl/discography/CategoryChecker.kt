package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupMapper.toDomain
import com.nickallport.findmevinyl.storage.AlbumDao
import kotlinx.coroutines.CancellationException

/**
 * Finds the category of the owned and wanted albums saved before soundtracks were a category of their own, such as a
 * soundtrack by one artist that is on the want list. Their pages would only change when MusicBrainz's list was read again,
 * and a page already complete is not. Albums are asked about [BATCH] at a time with one search by release group id, each
 * batch in its own turn of the shared throttle, and each album once: one MusicBrainz no longer lists is marked too.
 * A failed request stops the run and changes nothing, so the next run asks again.
 */
class CategoryChecker(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared
) {
    suspend fun run() {
        while (true) {
            val ids = albumDao.getUncheckedCategoryIds(BATCH)
            if (ids.isEmpty()) return
            val found = try {
                throttle.run { api.searchReleaseGroups("rgid:(${ids.joinToString(" OR ")})", limit = BATCH).releaseGroups }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return
            }
            found.map { it.toDomain() }
                .groupBy({ DiscographyCategorizer.categorize(it).name }, { it.id })
                .forEach { (category, groupIds) -> albumDao.setCategories(groupIds, category) }
            // The ones MusicBrainz did not list are marked as well, or they would be asked about for ever.
            albumDao.markCategoriesChecked(ids)
        }
    }

    private companion object {
        const val BATCH = 25
    }
}
