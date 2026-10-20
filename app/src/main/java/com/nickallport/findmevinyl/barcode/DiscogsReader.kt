package com.nickallport.findmevinyl.barcode

import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.discography.ReleaseFormats
import com.nickallport.findmevinyl.discogs.DiscogsFormatDto
import com.nickallport.findmevinyl.discogs.DiscogsResultDto
import java.text.Normalizer

/** An album Discogs lists with a barcode, in the app's terms. [format] is a label (LP, CD, ...) or null when it is not one. */
data class DiscogsFind(
    val artist: String,
    val album: String,
    val year: String?,
    val format: String?,
    val category: DiscographyCategory,
    /** A compilation by "Various": there is no one artist to look for on MusicBrainz. */
    val variousArtists: Boolean
)

/** Turns Discogs search results into [DiscogsFind]s. */
object DiscogsReader {
    private val artistSuffix = Regex("""\s*(\(\d+\)|\*)+$""")

    fun parse(results: List<DiscogsResultDto>): List<DiscogsFind> {
        val finds = results.mapNotNull { read(it) }
        // Several pressings of one album come with one barcode: the first of each is kept.
        return finds.distinctBy { MatchNames.normalise(it.artist) + "|" + MatchNames.normalise(it.album) }
    }

    private fun read(result: DiscogsResultDto): DiscogsFind? {
        val title = result.title ?: return null
        val at = title.indexOf(" - ")
        if (at <= 0) return null
        val artist = title.substring(0, at).replace(artistSuffix, "").trim()
        val album = title.substring(at + 3).trim()
        if (artist.isEmpty() || album.isEmpty()) return null
        val formats = result.formats.orEmpty()
        return DiscogsFind(
            artist = artist,
            album = album,
            year = result.year?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } && it != "0000" },
            format = formats.firstNotNullOfOrNull { labelOf(it) },
            category = categoryOf(formats),
            variousArtists = artist.equals("Various", ignoreCase = true)
        )
    }

    // Vinyl is sized by a description, and is an LP when it says so or says nothing.
    private fun labelOf(format: DiscogsFormatDto): String? {
        val descriptions = format.descriptions.orEmpty()
        return when (format.name) {
            "Vinyl" -> when {
                "7\"" in descriptions -> ReleaseFormats.SEVEN_INCH
                "10\"" in descriptions -> "10\""
                "12\"" in descriptions -> ReleaseFormats.TWELVE_INCH
                else -> ReleaseFormats.LP
            }
            "CD", "CDr", "SACD", "HDCD" -> ReleaseFormats.CD
            "Cassette" -> ReleaseFormats.MC
            "File" -> ReleaseFormats.DIGITAL
            else -> null
        }
    }

    private fun categoryOf(formats: List<DiscogsFormatDto>): DiscographyCategory {
        val descriptions = formats.flatMap { it.descriptions.orEmpty() }
        return when {
            "Compilation" in descriptions -> DiscographyCategory.COMPILATION
            "EP" in descriptions || "Mini-Album" in descriptions -> DiscographyCategory.EP
            "Single" in descriptions || "Maxi-Single" in descriptions -> DiscographyCategory.SINGLE
            else -> DiscographyCategory.STUDIO_ALBUM
        }
    }
}

/** Compares the names of artists and albums as two services write them. */
object MatchNames {
    private val brackets = Regex("""[(\[].*?[)\]]""")
    private val marks = Regex("""\p{Mn}+""")

    /** Lower case, accents off, "&" as "and", brackets and a leading "the" and anything but letters and digits dropped. */
    fun normalise(name: String): String {
        val plain = Normalizer.normalize(name, Normalizer.Form.NFD).replace(marks, "")
            .lowercase().replace("&", " and ").replace(brackets, " ")
        val words = plain.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("").trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        return (if (words.firstOrNull() == "the" && words.size > 1) words.drop(1) else words).joinToString(" ")
    }

    fun same(a: String, b: String): Boolean {
        val x = normalise(a)
        return x.isNotEmpty() && x == normalise(b)
    }
}
