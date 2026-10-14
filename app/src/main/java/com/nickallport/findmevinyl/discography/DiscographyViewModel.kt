package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupMapper.toDomain
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

sealed class DiscographyUiState {
    data class Success(val groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>) : DiscographyUiState()
    data class Error(val message: String) : DiscographyUiState()
}

class DiscographyViewModel(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val pageSize: Int = 100,
    private val pageDelayMillis: Long = 1_100,
    private val maxPages: Int = 10
) {
    suspend fun loadDiscography(artistId: String): DiscographyUiState {
        return try {
            val domainReleaseGroups = fetchAllReleaseGroups(artistId).map { it.toDomain() }

            albumDao.insertAllIfNotExist(
                domainReleaseGroups.map { releaseGroup ->
                    AlbumEntity(
                        id = releaseGroup.id,
                        artistId = artistId,
                        title = releaseGroup.title,
                        category = DiscographyCategorizer.categorize(releaseGroup).name,
                        firstReleaseDate = releaseGroup.firstReleaseDate,
                        owned = false,
                        wanted = false
                    )
                }
            )

            val savedAlbums = albumDao.getAlbumsForArtist(artistId)
            val organized = DiscographyOrganizer.organize(domainReleaseGroups)

            val uiGrouped = organized.mapValues { (_, releaseGroups) ->
                releaseGroups.map { releaseGroup ->
                    val savedAlbum = savedAlbums.find { it.id == releaseGroup.id }
                    AlbumUiModel(
                        id = releaseGroup.id,
                        title = releaseGroup.title,
                        owned = savedAlbum?.owned ?: false,
                        wanted = savedAlbum?.wanted ?: false
                    )
                }
            }

            DiscographyUiState.Success(uiGrouped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DiscographyUiState.Error(e.message ?: "Something went wrong")
        }
    }

    /**
     * MusicBrainz returns release groups a page at a time. This follows the pages until it has
     * them all. MusicBrainz allows about one request a second, so each request starts at least
     * [pageDelayMillis] after the one before it started. A slow response already used that time.
     * A failure on any page throws, so a short list is never mistaken for the whole discography.
     */
    private suspend fun fetchAllReleaseGroups(artistId: String): List<MusicBrainzReleaseGroupDto> {
        val all = mutableListOf<MusicBrainzReleaseGroupDto>()
        var pages = 0
        var lastStartNanos = 0L
        while (true) {
            if (pages > 0) {
                val sinceLastStartMillis = (System.nanoTime() - lastStartNanos) / 1_000_000
                val remaining = pageDelayMillis - sinceLastStartMillis
                if (remaining > 0) delay(remaining)
            }
            lastStartNanos = System.nanoTime()
            val response = api.getReleaseGroupsForArtist(
                artistId = artistId,
                limit = pageSize,
                offset = all.size
            )
            all += response.releaseGroups
            pages++

            val total = response.releaseGroupCount
            val finished = total == null ||
                all.size >= total ||
                response.releaseGroups.isEmpty() ||
                pages >= maxPages
            if (finished) return all
        }
    }

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
        albumDao.setWanted(albumId, !currentlyWanted)
        if (!currentlyWanted) {
            saveFavourite(artistId, artistName)
        }
    }

    suspend fun isFavourite(artistId: String): Boolean {
        return artistDao.getById(artistId) != null
    }

    suspend fun saveFavourite(artistId: String, artistName: String) {
        artistDao.insert(ArtistEntity(id = artistId, name = artistName, type = null, country = null))
    }

    suspend fun removeFavourite(artistId: String) {
        albumDao.clearWantedForArtist(artistId)
        artistDao.deleteById(artistId)
    }
}