package com.nickallport.findmevinyl.musicbrainz

import com.google.gson.annotations.SerializedName

/** A release group with its rating (`inc=ratings`). The rating is missing when nobody has voted. */
data class MusicBrainzReleaseGroupRatingResponse(
    val rating: MusicBrainzRatingDto? = null
)

data class MusicBrainzRatingDto(
    /** The average, out of 5. Null when there are no votes. */
    val value: Double? = null,
    @SerializedName("votes-count") val votesCount: Int? = null
)
