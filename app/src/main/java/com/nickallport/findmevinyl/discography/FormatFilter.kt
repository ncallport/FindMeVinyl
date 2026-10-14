package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.ui.discography.AlbumUiModel

/** One chip: a format label and how many albums have it. */
data class FormatOption(val label: String, val count: Int)

/**
 * Filtering a discography by format. An album's formats are only known once they have been looked
 * up, so an album with `formats == null` is "not checked yet". Those stay in the list while a
 * format is chosen, because their lookups are what settle the question.
 */
object FormatFilter {

    /** The formats found on the albums so far, in display order, each with the number of albums that have it. */
    fun options(albums: Collection<AlbumUiModel>): List<FormatOption> {
        val counts = albums.flatMap { it.formats.orEmpty().distinct() }.groupingBy { it }.eachCount()
        return ReleaseFormats.displayOrder.mapNotNull { label -> counts[label]?.let { FormatOption(label, it) } }
    }

    /** How many albums have not been looked up yet. An album looked up with no known formats is checked. */
    fun unchecked(albums: Collection<AlbumUiModel>): Int = albums.count { it.formats == null }

    /**
     * Keeps the albums that came out on [selected], and the ones not checked yet. Categories left
     * empty are dropped. With nothing selected the albums are returned as they are.
     */
    fun apply(
        grouped: Map<DiscographyCategory, List<AlbumUiModel>>,
        selected: String?
    ): Map<DiscographyCategory, List<AlbumUiModel>> {
        if (selected == null) return grouped
        return grouped
            .mapValues { (_, albums) -> albums.filter { it.formats == null || selected in it.formats } }
            .filterValues { it.isNotEmpty() }
    }
}
