package com.nickallport.findmevinyl.export

data class WantedAlbumExportItem(
    val artistName: String,
    val albumTitle: String
)

/**
 * What the user chose before sharing the want list. [intro] is a message for the top of it, and
 * [limit] is how many albums to share, from the top of the list in the user's own order, or null for all.
 */
data class ShareOptions(
    val intro: String = "",
    val limit: Int? = null
)

object WantListFormatter {
    /**
     * The want list as text, grouped by artist in the order given. A message goes above it, and a limit
     * keeps only the first albums, with the header saying how many of how many. A limit that is not a
     * positive number, or is as big as the list, shares everything.
     */
    fun format(items: List<WantedAlbumExportItem>, intro: String = "", limit: Int? = null): String {
        val cap = limit?.takeIf { it > 0 && it < items.size }
        val shown = if (cap != null) items.take(cap) else items

        val header = if (cap != null) "My vinyl want list (top $cap of ${items.size}):" else "My vinyl want list:"
        val message = intro.trim()
        val top = if (message.isEmpty()) header else "$message\n\n$header"
        if (shown.isEmpty()) return top

        val grouped = shown.groupBy { it.artistName }
        val body = grouped.entries.joinToString("\n\n") { (artist, albums) ->
            buildString {
                append(artist)
                albums.forEach { append("\n- ${it.albumTitle}") }
            }
        }

        return "$top\n\n$body"
    }
}
