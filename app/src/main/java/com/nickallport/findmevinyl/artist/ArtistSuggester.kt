package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import kotlinx.coroutines.CancellationException

/** An artist offered while the user types: [favourite] ones come from the user's own list, the rest from MusicBrainz. */
data class ArtistSuggestion(
    val id: String,
    val name: String,
    val description: String,
    val favourite: Boolean
)

/**
 * Suggests artists for what the user has typed so far, from MusicBrainz, five at most. The last word is
 * asked for as a start ("Joy Divi*"), unless it is followed by a space. Nothing is asked before three
 * letters, each answer is kept for the run of the app, and a request goes through [throttle], so it waits
 * its turn behind other lookups and costs nothing if typing goes on and it is cancelled while waiting.
 * A failure (MusicBrainz is often busy on a shared address) gives no suggestions and is not kept.
 */
class ArtistSuggester(
    private val api: MusicBrainzApi,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared
) {
    private val answers = mutableMapOf<String, List<ArtistSuggestion>>()

    suspend fun suggest(text: String): List<ArtistSuggestion> {
        val words = text.replace(SEARCH_SYNTAX, " ").trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.sumOf { it.length } < MIN_LETTERS) return emptyList()
        // A word still being typed is a start. One followed by a space is finished.
        val query = words.joinToString(" ") + if (text.last().isWhitespace()) "" else "*"
        val key = query.lowercase()
        answers[key]?.let { return it }
        return try {
            val response = throttle.run { api.searchArtists(query = query, limit = LIMIT) }
            response.artists
                .filterNot { it.id in SPECIAL_ARTIST_IDS || it.disambiguation?.startsWith("Special Purpose Artist") == true }
                .map { ArtistSuggestion(it.id, it.name, describe(it.type, it.country, it.disambiguation), favourite = false) }
                .also { answers[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun describe(type: String?, country: String?, disambiguation: String?): String {
        val kind = when (type) {
            "Group" -> "Band"
            "Person" -> "Solo artist"
            else -> "Artist"
        }
        return kind + (country?.let { " from $it" } ?: "") + (disambiguation?.let { " ($it)" } ?: "")
    }

    private companion object {
        const val MIN_LETTERS = 3
        const val LIMIT = 5

        /** What the Lucene search syntax uses, which would make a name like AC/DC a malformed query. */
        val SEARCH_SYNTAX = Regex("""[+\-&|!(){}\[\]^"~*?:\/]""")
        val WHITESPACE = Regex("""\s+""")

        /** "Various Artists" and "[unknown]", which a start such as "Va*" brings in. */
        val SPECIAL_ARTIST_IDS = setOf("89ad4ac3-39f7-470e-963a-56509c546377", "125ec42a-7229-4250-afc5-e57cfe0f4a02")
    }
}

/** Putting the user's own favourites ahead of what MusicBrainz suggests. */
object ArtistSuggestions {
    private const val MAX_FAVOURITES = 3
    private const val MAX_ALL = 6

    /** The favourites whose name has every word typed in it, in the user's order. One letter is too little. */
    fun favouritesMatching(text: String, favourites: List<FavouriteArtistUiModel>): List<ArtistSuggestion> {
        val words = text.lowercase().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (words.sumOf { it.length } < 2) return emptyList()
        return favourites
            .filter { favourite -> words.all { favourite.name.lowercase().contains(it) } }
            .take(MAX_FAVOURITES)
            .map { ArtistSuggestion(it.id, it.name, "In your favourites", favourite = true) }
    }

    /** The favourites first, then the others that are not already among them, six in all. */
    fun merge(local: List<ArtistSuggestion>, remote: List<ArtistSuggestion>): List<ArtistSuggestion> {
        val known = local.map { it.id }.toSet()
        return (local + remote.filter { it.id !in known }).take(MAX_ALL)
    }
}
