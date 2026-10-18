package com.nickallport.findmevinyl.genre

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nickallport.findmevinyl.favourites.ArtistGroupViewModel
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistGenresDao
import com.nickallport.findmevinyl.storage.ArtistGenresEntity
import com.nickallport.findmevinyl.storage.ArtistGroupDao
import kotlinx.coroutines.CancellationException

/** A genre MusicBrainz lists for an artist, and how many people voted for it. */
data class GenreOption(val name: String, val votes: Int)

/**
 * The genres of an artist, to choose from, and the one the user chose. MusicBrainz is asked for an artist's genres
 * (`artist/{id}?inc=genres`) in a turn of the shared [throttle], and the answer is kept for [refreshAfterMillis]
 * (90 days) in `artist_genres`, an artist with no genres included. If asking again fails the old answer is used.
 * A group offers the genres of all its members, one request each, one after another, with the votes added up.
 * Genres are kept in lower case, as MusicBrainz names them.
 */
class ArtistGenreViewModel(
    private val api: MusicBrainzApi,
    private val genresDao: ArtistGenresDao,
    private val artistDao: ArtistDao,
    private val groupDao: ArtistGroupDao,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshAfterMillis: Long = 90L * 24 * 60 * 60 * 1000
) {
    private val gson = Gson()

    // What is kept in `artist_genres`.
    private data class Stored(val name: String, val count: Int)

    /**
     * The genres to choose from, the best voted first and fifteen at most. Empty when MusicBrainz lists none. Null
     * when none could be had at all: every request failed and nothing was kept.
     */
    suspend fun options(id: String): List<GenreOption>? {
        val members = if (ArtistGroupViewModel.isGroupId(id)) groupDao.getMembers(id).map { it.artistId } else listOf(id)
        if (members.isEmpty()) return emptyList()

        var answered = false
        val votes = linkedMapOf<String, Int>()
        for (member in members) {
            val found = genresOf(member) ?: continue
            answered = true
            for (genre in found) votes[genre.name] = (votes[genre.name] ?: 0) + genre.count
        }
        if (!answered) return null
        return votes.map { GenreOption(it.key, it.value) }
            .sortedWith(compareByDescending<GenreOption> { it.votes }.thenBy { it.name })
            .take(MAX_OPTIONS)
    }

    private suspend fun genresOf(artistId: String): List<Stored>? {
        val kept = genresDao.get(artistId)
        val keptGenres = kept?.let { parse(it.genres) }
        if (kept != null && keptGenres != null && clock() - kept.checkedAt <= refreshAfterMillis) return keptGenres
        return try {
            val response = throttle.run { api.getArtistGenres(artistId) }
            val found = response.genres.orEmpty()
                .mapNotNull { dto -> GenreNames.normalise(dto.name)?.let { Stored(it, dto.count ?: 0) } }
                .sortedWith(compareByDescending<Stored> { it.count }.thenBy { it.name })
            genresDao.save(ArtistGenresEntity(artistId, gson.toJson(found), clock()))
            found
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Asking again failed: the old answer is better than none.
            keptGenres
        }
    }

    private fun parse(json: String): List<Stored>? = try {
        gson.fromJson<List<Stored>>(json, object : TypeToken<List<Stored>>() {}.type)
    } catch (e: Exception) {
        null
    }

    /** The genre the user chose for the artist or the group, or null. */
    suspend fun genreOf(id: String): String? =
        if (ArtistGroupViewModel.isGroupId(id)) groupDao.getGroup(id)?.genre else artistDao.getById(id)?.genre

    /** Keeps the genre, tidied (see [GenreNames.normalise]), for the artist or the group. Null or blank takes it away. */
    suspend fun setGenre(id: String, genre: String?) {
        val tidy = GenreNames.normalise(genre)
        if (ArtistGroupViewModel.isGroupId(id)) groupDao.setGenre(id, tidy) else artistDao.setGenre(id, tidy)
    }

    private companion object {
        const val MAX_OPTIONS = 15
    }
}
