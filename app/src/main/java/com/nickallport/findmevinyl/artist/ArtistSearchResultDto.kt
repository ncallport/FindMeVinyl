package com.nickallport.findmevinyl.artist

data class ArtistSearchResultDto(
    val id: String,
    val name: String,
    val type: String?,
    val country: String?,
    val lifeSpanBegin: String?,
    val lifeSpanEnd: String?,
    val disambiguation: String?
)

object ArtistDescriptionFormatter {
    fun describe(artist: ArtistSearchResultDto): String {
        val typeLabel = when (artist.type) {
            "Group" -> "Band"
            "Person" -> "Solo artist"
            else -> "Artist"
        }

        val fromPhrase = artist.country?.let { " from $it" } ?: ""

        // MusicBrainz has no dates for many artists, so the active phrase is only there when it has something to say.
        val begin = artist.lifeSpanBegin?.takeIf { it.isNotBlank() }
        val end = artist.lifeSpanEnd?.takeIf { it.isNotBlank() }
        val activePhrase = when {
            begin != null && end != null -> ", active $begin–$end"
            begin != null -> ", active since $begin"
            end != null -> ", active until $end"
            else -> ""
        }

        val base = "$typeLabel$fromPhrase$activePhrase"

        return artist.disambiguation?.let { "$base ($it)" } ?: base
    }
}