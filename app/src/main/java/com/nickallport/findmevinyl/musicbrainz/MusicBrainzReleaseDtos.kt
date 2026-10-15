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

data class MusicBrainzReleaseDto(
    val id: String? = null,
    val media: List<MusicBrainzMediumDto>? = null,
    val date: String? = null,
    val status: String? = null,
    val country: String? = null,
    /** Only there when the list was asked for with labels (`inc=media+labels`). */
    @SerializedName("label-info") val labelInfo: List<MusicBrainzLabelInfoDto>? = null
)

data class MusicBrainzMediumDto(
    val format: String? = null
)
