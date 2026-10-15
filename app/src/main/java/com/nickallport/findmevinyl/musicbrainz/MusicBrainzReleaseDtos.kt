package com.nickallport.findmevinyl.musicbrainz

import com.google.gson.annotations.SerializedName

/** The releases inside one release group, with their media (`inc=media`). */
data class MusicBrainzReleaseListResponse(
    val releases: List<MusicBrainzReleaseDto>? = null
) {
    /** Each format name once, in the order they first appear. Releases or media without one are skipped. */
    fun formatNames(): List<String> =
        releases.orEmpty()
            .flatMap { it.media.orEmpty() }
            .mapNotNull { it.format }
            .distinct()
}

/** One page of the releases of an artist (`release?artist=`), with the total over all pages. */
data class MusicBrainzArtistReleasesResponse(
    @SerializedName("release-count") val releaseCount: Int? = null,
    val releases: List<MusicBrainzReleaseDto>? = null
)

data class MusicBrainzReleaseGroupRefDto(
    val id: String? = null
)

data class MusicBrainzReleaseDto(
    val id: String? = null,
    val media: List<MusicBrainzMediumDto>? = null,
    val date: String? = null,
    val status: String? = null,
    val country: String? = null,
    /** Only there when the list was asked for with labels (`inc=media+labels`). */
    @SerializedName("label-info") val labelInfo: List<MusicBrainzLabelInfoDto>? = null,
    /** Only there when asked for with `inc=release-groups`. */
    @SerializedName("release-group") val releaseGroup: MusicBrainzReleaseGroupRefDto? = null
)

data class MusicBrainzMediumDto(
    val format: String? = null
)
