package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzArtistMapper.toDomain

sealed class ArtistSearchUiState {
    object Idle : ArtistSearchUiState()
    object Loading : ArtistSearchUiState()
    data class Success(val results: List<ArtistDisplayItem>) : ArtistSearchUiState()
    data class Error(val message: String) : ArtistSearchUiState()
}

data class ArtistDisplayItem(
    val id: String,
    val name: String,
    val description: String
)

class ArtistSearchViewModel(private val api: MusicBrainzApi) {
    suspend fun search(query: String): ArtistSearchUiState {
        return try {
            val response = api.searchArtists(query = query)
            val results = response.artists.map { dto ->
                val domain = dto.toDomain()
                ArtistDisplayItem(
                    id = domain.id,
                    name = domain.name,
                    description = ArtistDescriptionFormatter.describe(domain)
                )
            }
            ArtistSearchUiState.Success(results)
        } catch (e: Exception) {
            ArtistSearchUiState.Error(e.message ?: "Something went wrong")
        }
    }
}