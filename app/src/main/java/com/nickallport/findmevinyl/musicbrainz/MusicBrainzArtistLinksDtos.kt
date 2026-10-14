package com.nickallport.findmevinyl.musicbrainz

/** An artist with its web links (`inc=url-rels`). Links to other artists have no `url`. */
data class MusicBrainzArtistLinksResponse(
    val relations: List<MusicBrainzRelationDto>? = null
)

data class MusicBrainzRelationDto(
    val type: String? = null,
    val url: MusicBrainzRelationUrlDto? = null
)

data class MusicBrainzRelationUrlDto(
    val resource: String? = null
)
