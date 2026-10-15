package com.nickallport.findmevinyl.discography

import com.google.gson.Gson

/**
 * The stored form of [AlbumDetails]. Reading is forgiving: a row written by an older version of the
 * app, with fields missing, still reads, and text that is not details at all gives null, so the page
 * fetches them again.
 */
object AlbumDetailsJson {
    private val gson = Gson()

    // Everything nullable, because Gson puts null in a field the JSON does not have, whatever its Kotlin type.
    private data class StoredTrack(val number: String? = null, val title: String? = null, val lengthMillis: Long? = null)
    private data class StoredDisc(val title: String? = null, val format: String? = null, val tracks: List<StoredTrack?>? = null)
    private data class StoredDetails(
        val releaseDate: String? = null,
        val country: String? = null,
        val label: String? = null,
        val catalogNumber: String? = null,
        val firstReleaseDate: String? = null,
        val format: String? = null,
        val genres: List<String?>? = null,
        val discs: List<StoredDisc?>? = null
    )

    fun encode(details: AlbumDetails): String = gson.toJson(details)

    fun decode(json: String): AlbumDetails? {
        if (json.isBlank()) return null
        val stored = try {
            gson.fromJson(json, StoredDetails::class.java)
        } catch (e: Exception) {
            null
        } ?: return null

        return AlbumDetails(
            releaseDate = stored.releaseDate,
            country = stored.country,
            label = stored.label,
            catalogNumber = stored.catalogNumber,
            firstReleaseDate = stored.firstReleaseDate,
            format = stored.format,
            genres = stored.genres.orEmpty().filterNotNull(),
            discs = stored.discs.orEmpty().filterNotNull().map { disc ->
                Disc(
                    title = disc.title,
                    format = disc.format,
                    tracks = disc.tracks.orEmpty().filterNotNull().map {
                        Track(it.number, it.title?.takeIf { title -> title.isNotBlank() } ?: "Untitled", it.lengthMillis)
                    }
                )
            }
        )
    }
}
