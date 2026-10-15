package com.nickallport.findmevinyl.discography

import com.google.gson.Gson

/**
 * The stored form of [AlbumDetails]. Reading is forgiving about fields (missing ones are empty, unknown
 * ones are ignored), but a row carries the version of its layout. A row with no version (written before
 * pressings and ratings were kept) or an older one (2: pressings sorted with a bare year before a month
 * of the same year) is out of date, and a row from a newer version of the app is not understood, so all
 * of them read as missing and the page fetches the details again. Raise the version when what is stored
 * changes in a way old rows cannot be read as. Text that is not details at all gives null too.
 */
object AlbumDetailsJson {
    private const val VERSION = 3
    private val gson = Gson()

    // Everything nullable, because Gson puts null in a field the JSON does not have, whatever its Kotlin type.
    private data class StoredTrack(val number: String? = null, val title: String? = null, val lengthMillis: Long? = null)
    private data class StoredDisc(val title: String? = null, val format: String? = null, val tracks: List<StoredTrack?>? = null)
    private data class StoredPressing(
        val releaseId: String? = null,
        val date: String? = null,
        val country: String? = null,
        val label: String? = null,
        val catalogNumber: String? = null,
        val format: String? = null
    )
    private data class StoredRating(val value: Double? = null, val votes: Int? = null)
    private data class StoredDetails(
        val version: Int? = null,
        val releaseDate: String? = null,
        val country: String? = null,
        val label: String? = null,
        val catalogNumber: String? = null,
        val firstReleaseDate: String? = null,
        val format: String? = null,
        val genres: List<String?>? = null,
        val discs: List<StoredDisc?>? = null,
        val pressings: List<StoredPressing?>? = null,
        val pressingId: String? = null,
        val rating: StoredRating? = null
    )

    fun encode(details: AlbumDetails): String {
        val tree = gson.toJsonTree(details).asJsonObject
        tree.addProperty("version", VERSION)
        return gson.toJson(tree)
    }

    fun decode(json: String): AlbumDetails? {
        if (json.isBlank()) return null
        val stored = try {
            gson.fromJson(json, StoredDetails::class.java)
        } catch (e: Exception) {
            null
        } ?: return null
        if (stored.version != VERSION) return null

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
            },
            pressings = stored.pressings.orEmpty().filterNotNull().mapNotNull {
                it.releaseId?.let { id -> Pressing(id, it.date, it.country, it.label, it.catalogNumber, it.format) }
            },
            pressingId = stored.pressingId,
            rating = Rating.of(stored.rating?.value, stored.rating?.votes)
        )
    }
}
