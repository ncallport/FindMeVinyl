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

        val activePhrase = if (artist.lifeSpanEnd != null) {
            "active ${artist.lifeSpanBegin}\u2013${artist.lifeSpanEnd}"
        } else {
            "active since ${artist.lifeSpanBegin}"
        }

        val base = "$typeLabel$fromPhrase, $activePhrase"

        return artist.disambiguation?.let { "$base ($it)" } ?: base
    }
}