package com.nickallport.findmevinyl.musicbrainz

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nickallport.findmevinyl.artist.ArtistSearchResultDto

data class MusicBrainzArtistSearchResponse(
    val artists: List<MusicBrainzArtistDto>
)

data class MusicBrainzArtistDto(
    val id: String,
    val name: String,
    val type: String?,
    val country: String?,
    @SerializedName("life-span") val lifeSpan: MusicBrainzLifeSpanDto?,
    val disambiguation: String?
)

data class MusicBrainzLifeSpanDto(
    val begin: String?,
    val end: String?
)

object MusicBrainzArtistMapper {
    private val gson = Gson()

    fun parseSearchResponse(json: String): List<ArtistSearchResultDto> {
        val response = gson.fromJson(json, MusicBrainzArtistSearchResponse::class.java)
        return response.artists.map { it.toDomain() }
    }

    fun MusicBrainzArtistDto.toDomain(): ArtistSearchResultDto {
        return ArtistSearchResultDto(
            id = id,
            name = name,
            type = type,
            country = country,
            lifeSpanBegin = lifeSpan?.begin,
            lifeSpanEnd = lifeSpan?.end,
            disambiguation = disambiguation
        )
    }
}