package com.nickallport.findmevinyl.musicbrainz

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nickallport.findmevinyl.discography.ReleaseGroupDto

data class MusicBrainzReleaseGroupSearchResponse(
    @SerializedName("release-groups") val releaseGroups: List<MusicBrainzReleaseGroupDto>,
    /** How many release groups the artist has in all. A page holds at most the `limit` asked for. */
    @SerializedName("release-group-count") val releaseGroupCount: Int? = null
)

data class MusicBrainzReleaseGroupDto(
    val id: String,
    val title: String,
    @SerializedName("primary-type") val primaryType: String?,
    @SerializedName("secondary-types") val secondaryTypes: List<String>?,
    @SerializedName("first-release-date") val firstReleaseDate: String?
)

object MusicBrainzReleaseGroupMapper {
    private val gson = Gson()

    fun parseSearchResponse(json: String): List<ReleaseGroupDto> {
        val response = gson.fromJson(json, MusicBrainzReleaseGroupSearchResponse::class.java)
        return response.releaseGroups.map { it.toDomain() }
    }

    fun MusicBrainzReleaseGroupDto.toDomain(): ReleaseGroupDto {
        return ReleaseGroupDto(
            id = id,
            title = title,
            primaryType = primaryType,
            secondaryTypes = secondaryTypes ?: emptyList(),
            // MusicBrainz sends "" for an undated release group. That would sort before every real date.
            firstReleaseDate = firstReleaseDate?.takeIf { it.isNotEmpty() }
        )
    }
}
