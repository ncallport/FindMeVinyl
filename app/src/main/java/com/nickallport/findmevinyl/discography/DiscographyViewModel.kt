package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupMapper.toDomain
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CancellationException

sealed class DiscographyUiState {
    data class Success(val groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>) : DiscographyUiState()
    data class Error(val message: String) : DiscographyUiState()
}

class DiscographyViewModel(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) {
    suspend fun loadDiscography(artistId: String): DiscographyUiState {
        return try {
            val response = api.getReleaseGroupsForArtist(artistId = artistId)
            val domainReleaseGroups = response.releaseGroups.map { it.toDomain() }

            domainReleaseGroups.forEach { releaseGroup ->
                albumDao.insertIfNotExists(
                    AlbumEntity(
                        id = releaseGroup.id,
                        artistId = artistId,
                        title = releaseGroup.title,
                        category = DiscographyCategorizer.categorize(releaseGroup).name,
                        firstReleaseDate = releaseGroup.firstReleaseDate,
                        owned = false,
                        wanted = false
                    )
                )
            }

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

    suspend fun toggleOwned(albumId: String, currentlyOwned: Boolean) {
        albumDao.setOwned(albumId, !currentlyOwned)
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