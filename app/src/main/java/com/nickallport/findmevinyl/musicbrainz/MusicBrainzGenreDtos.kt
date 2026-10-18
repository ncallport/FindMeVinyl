package com.nickallport.findmevinyl.musicbrainz

/** An artist with its genres (`inc=genres`). The genres are the same kind as an album's (`MusicBrainzGenreDto`). */
data class MusicBrainzArtistGenresResponse(val genres: List<MusicBrainzGenreDto>? = null)
