package com.nickallport.findmevinyl.discography

/**
 * MusicBrainz names the medium of every release ("12\" Vinyl", "CD", "Cassette", ...). The raw
 * names are stored, so these rules can change without fetching anything again.
 */
object ReleaseFormats {
    private const val SEPARATOR = "|"

    // The order the labels are shown in.
    private enum class Label(val text: String) {
        LP("LP"), TWELVE_INCH("12\""), TEN_INCH("10\""), SEVEN_INCH("7\""), CD("CD"), MC("MC"), DIGITAL("Digital")
    }

    private val cdWord = Regex("""\bCD\b""")

    /** The raw names as one string for the database. A comma will not do: some names contain one. */
    fun encode(names: Collection<String>): String = names.distinct().sorted().joinToString(SEPARATOR)

    fun decode(stored: String): List<String> = if (stored.isEmpty()) emptyList() else stored.split(SEPARATOR)

    /**
     * The labels to show for an album: LP, 12", 10", 7", CD, MC and Digital. Other media, such as
     * DVD and the SACD layer of a hybrid disc, are left out.
     */
    fun labels(names: Collection<String>, category: DiscographyCategory): List<String> {
        // A 12 inch record is an LP when it is an album, and a 12 inch when it is a single or an EP.
        val twelveInch = when (category) {
            DiscographyCategory.SINGLE, DiscographyCategory.EP -> Label.TWELVE_INCH
            else -> Label.LP
        }
        return names.mapNotNull { labelFor(it, twelveInch) }
            .distinct()
            .sortedBy { it.ordinal }
            .map { it.text }
    }

    private fun labelFor(name: String, twelveInch: Label): Label? = when {
        // Vinyl of no stated size is treated as 12 inch.
        name == "12\" Vinyl" || name == "Vinyl" -> twelveInch
        name == "10\" Vinyl" -> Label.TEN_INCH
        name == "7\" Vinyl" -> Label.SEVEN_INCH
        name == "Cassette" -> Label.MC
        name == "Digital Media" -> Label.DIGITAL
        isCd(name) -> Label.CD
        else -> null
    }

    // "CD", "CD-R", "SHM-CD", "8cm CD" and "Hybrid SACD (CD layer)" match. "SACD" and "VCD" do not.
    private fun isCd(name: String): Boolean =
        name == "HDCD" || (cdWord.containsMatchIn(name) && !name.contains("Video"))
}
