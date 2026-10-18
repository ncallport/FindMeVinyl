package com.nickallport.findmevinyl.genre

import java.util.Locale

/** How a genre is kept and shown. */
object GenreNames {
    const val MAX_LENGTH = 40

    /**
     * A genre as it is kept: trimmed, in lower case, with one space between words, and cut to [MAX_LENGTH]. MusicBrainz
     * names its genres in lower case, so a genre typed with capitals joins the same group. Null when nothing is left.
     */
    fun normalise(text: String?): String? {
        val tidy = text.orEmpty().trim().lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ").take(MAX_LENGTH).trim()
        return tidy.ifEmpty { null }
    }

    /** A genre as it is shown, with a capital letter to begin with. No genre is "No genre yet". */
    fun display(genre: String?): String = when {
        genre == null -> "No genre yet"
        genre.isEmpty() -> ""
        else -> genre.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
