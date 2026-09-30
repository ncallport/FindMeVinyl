package com.nickallport.findmevinyl.export

data class WantedAlbumExportItem(
    val artistName: String,
    val albumTitle: String
)

object WantListFormatter {
    fun format(items: List<WantedAlbumExportItem>): String {
        val header = "My vinyl want list:"
        if (items.isEmpty()) return header

        val grouped = items.groupBy { it.artistName }
        val body = grouped.entries.joinToString("\n\n") { (artist, albums) ->
            buildString {
                append(artist)
                albums.forEach { append("\n- ${it.albumTitle}") }
            }
        }

        return "$header\n\n$body"
    }
}