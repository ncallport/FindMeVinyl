package com.nickallport.findmevinyl.musicbrainz

import com.google.gson.annotations.SerializedName

/** The releases that carry a barcode (`release?query=barcode:...`). */
data class MusicBrainzBarcodeSearchResponse(
    val releases: List<MusicBrainzBarcodeReleaseDto>? = null
)

data class MusicBrainzBarcodeReleaseDto(
    val id: String? = null,
    val title: String? = null,
    val status: String? = null,
    val country: String? = null,
    val date: String? = null,
    @SerializedName("artist-credit") val artistCredit: List<MusicBrainzArtistCreditDto>? = null,
    @SerializedName("release-group") val releaseGroup: MusicBrainzBarcodeReleaseGroupDto? = null,
    val media: List<MusicBrainzMediumDto>? = null
)

data class MusicBrainzArtistCreditDto(
    val name: String? = null,
    val artist: MusicBrainzCreditedArtistDto? = null
)

data class MusicBrainzCreditedArtistDto(
    val id: String? = null,
    val name: String? = null
)

/** The release group as a search result gives it: no first release date and no secondary types. */
data class MusicBrainzBarcodeReleaseGroupDto(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("primary-type") val primaryType: String? = null
)
