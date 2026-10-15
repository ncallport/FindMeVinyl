package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupMapper.toDomain
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last

sealed class DiscographyUiState {
    data class Success(
        val groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>,
        /** More pages are still coming, so the list may grow. */
        val loadingMore: Boolean = false,
        /** A later page failed. The list is what had loaded by then. */
        val loadError: String? = null
    ) : DiscographyUiState()

    data class Error(val message: String) : DiscographyUiState()
}

class DiscographyViewModel(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val pageSize: Int = 100,
    private val pageDelayMillis: Long = 1_100,
    private val maxPages: Int = 10,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    private val clock: () -> Long = System::currentTimeMillis,
    private val formatRefreshAfterMillis: Long = 30L * 24 * 60 * 60 * 1000
) {
    /**
     * Looks up the formats of an album's releases on MusicBrainz, saves them, and returns the
     * labels to show. Null means the lookup failed, and nothing is saved, so it can be tried again.
     * The request waits its turn in the throttle. Once it has started, the save is part of it, so a
     * caller that is cancelled meanwhile still leaves the formats stored.
     */
    suspend fun loadFormats(albumId: String): List<String>? {
        return try {
            val names = throttle.run {
                val found = api.getReleasesForReleaseGroup(releaseGroupId = albumId).formatNames()
                albumDao.setFormats(albumId, ReleaseFormats.encode(found), clock())
                found
            }
            ReleaseFormats.labels(names, categoryOf(albumId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun categoryOf(albumId: String): DiscographyCategory =
        albumDao.getById(albumId)?.category?.let(::categoryFrom) ?: DiscographyCategory.OTHER

    private fun categoryFrom(stored: String): DiscographyCategory =
        runCatching { DiscographyCategory.valueOf(stored) }.getOrDefault(DiscographyCategory.OTHER)

    /**
     * The discography as it is saved on the phone, with no request to MusicBrainz. Taps on Owned
     * and Want use this, so they show at once. Albums are grouped by their stored category and
     * sorted by release date with undated last, then by title.
     */
    suspend fun loadSaved(artistId: String): DiscographyUiState.Success {
        val now = clock()
        val grouped = albumDao.getAlbumsForArtist(artistId)
            .groupBy { categoryFrom(it.category) }
            .mapValues { (category, albums) ->
                albums
                    // Albums saved before the mapper dropped empty dates still have "", so both mean undated.
                    .sortedWith(
                        compareBy<AlbumEntity, String?>(nullsLast()) { it.firstReleaseDate?.takeIf { d -> d.isNotEmpty() } }
                            .thenBy { it.title }
                    )
                    .map { toUiModel(it, category, now) }
            }
        return DiscographyUiState.Success(grouped)
    }

    /** One saved album as the screens show it, or null when it is not saved. */
    suspend fun loadAlbum(albumId: String): AlbumUiModel? =
        albumDao.getById(albumId)?.let { toUiModel(it, categoryFrom(it.category), clock()) }

    private fun toUiModel(album: AlbumEntity, category: DiscographyCategory, now: Long) = AlbumUiModel(
        id = album.id,
        title = album.title,
        owned = album.owned,
        wanted = album.wanted,
        formats = album.formats?.let { ReleaseFormats.labels(ReleaseFormats.decode(it), category) },
        formatsStale = album.formatsCheckedAt?.let { now - it > formatRefreshAfterMillis } ?: false
    )

    /**
     * The final state of [discography]: the whole discography once every page has loaded.
     */
    suspend fun loadDiscography(artistId: String): DiscographyUiState = discography(artistId).last()

    /**
     * Loads an artist's discography from MusicBrainz a page at a time. Each page is saved as it
     * arrives, and a state is emitted after it, so the first page can be shown while the rest load.
     * A state with [DiscographyUiState.Success.loadingMore] set is followed by another.
     *
     * An artist opened before starts with the saved albums, before any request. If the first page
     * fails and nothing is saved, the only state is an error. In every other case a failure leaves
     * the list so far as the last state, with [DiscographyUiState.Success.loadError], so the list is
     * never silently short.
     */
    fun discography(artistId: String): Flow<DiscographyUiState> =
        pages(artistId).catch { e ->
            if (e is CancellationException) throw e
            emit(DiscographyUiState.Error(e.message ?: "Something went wrong"))
        }

    /**
     * MusicBrainz allows about one request a second, so each page is asked for at least
     * [pageDelayMillis] after the one before it was asked for. A slow response already used that time.
     */
    private fun pages(artistId: String): Flow<DiscographyUiState> = flow {
        // Albums saved on an earlier visit show at once, before any request. With none saved there
        // is nothing to show yet, so no empty list is shown first.
        val saved = loadSaved(artistId)
        val showedSaved = saved.groupedAlbums.isNotEmpty()
        if (showedSaved) emit(saved.copy(loadingMore = true))

        var fetched = 0
        var pages = 0
        var lastStartNanos = 0L
        while (true) {
            if (pages > 0) {
                val sinceLastStartMillis = (System.nanoTime() - lastStartNanos) / 1_000_000
                val remaining = pageDelayMillis - sinceLastStartMillis
                if (remaining > 0) delay(remaining)
            }
            lastStartNanos = System.nanoTime()
            val response = try {
                api.getReleaseGroupsForArtist(artistId = artistId, limit = pageSize, offset = fetched)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Something went wrong"
                // With nothing to show, a failed first page is an error. Otherwise keep the list.
                emit(
                    if (pages == 0 && !showedSaved) DiscographyUiState.Error(message)
                    else loadSaved(artistId).copy(loadError = message)
                )
                return@flow
            }

            albumDao.insertAllIfNotExist(response.releaseGroups.map { newAlbum(artistId, it.toDomain()) })
            fetched += response.releaseGroups.size
            pages++

            // Every saved album came from MusicBrainz, so once as many are saved as it lists, there
            // is nothing new in the pages still to come. That makes reopening an artist one request.
            val nowSaved = loadSaved(artistId)
            val savedCount = nowSaved.groupedAlbums.values.sumOf { it.size }

            val total = response.releaseGroupCount
            val finished = total == null ||
                fetched >= total ||
                savedCount >= total ||
                response.releaseGroups.isEmpty() ||
                pages >= maxPages
            emit(nowSaved.copy(loadingMore = !finished))
            if (finished) return@flow
        }
    }

    private fun newAlbum(artistId: String, releaseGroup: ReleaseGroupDto) = AlbumEntity(
        id = releaseGroup.id,
        artistId = artistId,
        title = releaseGroup.title,
        category = DiscographyCategorizer.categorize(releaseGroup).name,
        firstReleaseDate = releaseGroup.firstReleaseDate,
        owned = false,
        wanted = false
    )

    suspend fun toggleOwned(albumId: String, currentlyOwned: Boolean) {
        albumDao.setOwned(albumId, !currentlyOwned)
        if (!currentlyOwned) {
            albumDao.setWanted(albumId, false)
        }
    }

    suspend fun toggleWanted(
        albumId: String,
        currentlyWanted: Boolean,
        artistId: String,
        artistName: String
    ) {
        if (currentlyWanted) {
            albumDao.setWanted(albumId, false)
        } else {
            albumDao.markWanted(albumId)
            saveFavourite(artistId, artistName)
        }
    }

    suspend fun isFavourite(artistId: String): Boolean {
        return artistDao.getById(artistId) != null
    }

    suspend fun saveFavourite(artistId: String, artistName: String) {
        // A favourite that is already saved keeps its place in the user's order. A new one goes last.
        if (artistDao.getById(artistId) != null) return
        val position = (artistDao.maxPosition() ?: -1) + 1
        artistDao.insert(ArtistEntity(id = artistId, name = artistName, type = null, country = null, position = position))
    }

    suspend fun removeFavourite(artistId: String) {
        albumDao.clearWantedForArtist(artistId)
        artistDao.deleteById(artistId)
    }
}
