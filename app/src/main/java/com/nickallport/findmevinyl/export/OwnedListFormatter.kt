package com.nickallport.findmevinyl.export

/** One owned album for the share text: its artist, its title and the formats of the copies the user has. */
data class OwnedAlbumExportItem(
    val artistName: String,
    val albumTitle: String,
    val formats: List<String> = emptyList()
)

object OwnedListFormatter {
    /**
     * The owned list as text, one numbered line for each album in the order given, with the formats of its copies in
     * brackets (each once) when it has any: `1. Bob Dylan - Desire (LP, CD)`.
     */
    fun format(items: List<OwnedAlbumExportItem>): String {
        val albums = if (items.size == 1) "1 album" else "${items.size} albums"
        val header = "My record collection ($albums):"
        if (items.isEmpty()) return header

        val body = items.mapIndexed { index, item ->
            val formats = item.formats.distinct()
            "${index + 1}. ${item.artistName} - ${item.albumTitle}" + if (formats.isEmpty()) "" else " (${formats.joinToString(", ")})"
        }.joinToString("\n")

        return "$header\n\n$body"
    }
}
