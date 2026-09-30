package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupMapper.toDomain
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel

sealed class DiscographyUiState {
    data class Success(val groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>) : DiscographyUiState()
    data class Error(val message: String) : DiscographyUiState()
}

class DiscographyViewModel(private val api: MusicBrainzApi) {
    suspend fun loadDiscography(artistId: String): DiscographyUiState {
        return try {
            val response = api.getReleaseGroupsForArtist(artistId = artistId)
            val domainReleaseGroups = response.releaseGroups.map { it.toDomain() }
            val organized = DiscographyOrganizer.organize(domainReleaseGroups)

            val uiGrouped = organized.mapValues { (_, releaseGroups) ->
                releaseGroups.map { releaseGroup ->
                    AlbumUiModel(
                        id = releaseGroup.id,
                        title = releaseGroup.title,
                        owned = false,
                        wanted = false
                    )
                }
            }

            DiscographyUiState.Success(uiGrouped)
        } catch (e: Exception) {
            DiscographyUiState.Error(e.message ?: "Something went wrong")
        }
    }
}