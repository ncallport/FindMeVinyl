package com.nickallport.findmevinyl.musicbrainz

import com.google.gson.annotations.SerializedName

/** One release with its tracks (`inc=recordings+artist-credits+labels+release-groups+genres`). Everything can be missing. */
data class MusicBrainzReleaseDetailResponse(
    val title: String? = null,
    val date: String? = null,
    val country: String? = null,
    val status: String? = null,
    @SerializedName("label-info") val labelInfo: List<MusicBrainzLabelInfoDto>? = null,
    @SerializedName("release-group") val releaseGroup: MusicBrainzReleaseGroupDetailDto? = null,
    val media: List<MusicBrainzMediumDetailDto>? = null
)

data class MusicBrainzLabelInfoDto(
    @SerializedName("catalog-number") val catalogNumber: String? = null,
    val label: MusicBrainzLabelDto? = null
)

data class MusicBrainzLabelDto(
    val name: String? = null
)

data class MusicBrainzReleaseGroupDetailDto(
    @SerializedName("primary-type") val primaryType: String? = null,
    @SerializedName("first-release-date") val firstReleaseDate: String? = null,
    val genres: List<MusicBrainzGenreDto>? = null
)

data class MusicBrainzGenreDto(
    val name: String? = null,
    val count: Int? = null
)

data class MusicBrainzMediumDetailDto(
    val position: Int? = null,
    val format: String? = null,
    val title: String? = null,
    val tracks: List<MusicBrainzTrackDto>? = null
)

data class MusicBrainzTrackDto(
    val number: String? = null,
    val title: String? = null,
    /** The length in milliseconds. MusicBrainz does not always know it. */
    val length: Long? = null,
    /** Who is credited on the track (`inc=artist-credits`). */
    @SerializedName("artist-credit") val artistCredit: List<MusicBrainzArtistCreditDto>? = null
)
