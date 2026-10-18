package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.genre.GenreOption

/**
 * What the page of an artist or a group needs to show and change its genre: the genre now chosen, the genre
 * suggested from the votes on MusicBrainz (shown while none is chosen), the genres MusicBrainz offers (null when they
 * could not be had), and a way to keep a choice (null takes it away).
 */
class GenreControls(
    val load: suspend () -> String?,
    val suggested: suspend () -> String? = { null },
    val options: suspend () -> List<GenreOption>?,
    val set: suspend (String?) -> Unit
)
