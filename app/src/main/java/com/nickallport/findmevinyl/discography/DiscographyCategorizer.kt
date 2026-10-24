package com.nickallport.findmevinyl.discography

data class ReleaseGroupDto(
    val id: String,
    val title: String,
    val primaryType: String?,
    val secondaryTypes: List<String>,
    val firstReleaseDate: String?
)

enum class DiscographyCategory {
    STUDIO_ALBUM, LIVE_ALBUM, COMPILATION, SOUNDTRACK, EP, SINGLE, OTHER,

    /** Never stored: Various Artists albums the user owns or wants that an artist is on, shown under the artist's own albums. */
    APPEARS_ON
}

object DiscographyCategorizer {
    fun categorize(releaseGroup: ReleaseGroupDto): DiscographyCategory {
        val secondary = releaseGroup.secondaryTypes

        return when {
            // A soundtrack album is one whatever else it is, but a single or EP from a film stays a single or EP.
            secondary.contains("Soundtrack") && releaseGroup.primaryType == "Album" -> DiscographyCategory.SOUNDTRACK
            secondary.contains("Live") -> DiscographyCategory.LIVE_ALBUM
            secondary.contains("Compilation") -> DiscographyCategory.COMPILATION
            releaseGroup.primaryType == "EP" -> DiscographyCategory.EP
            releaseGroup.primaryType == "Single" -> DiscographyCategory.SINGLE
            releaseGroup.primaryType == "Album" -> DiscographyCategory.STUDIO_ALBUM
            else -> DiscographyCategory.OTHER
        }
    }
}