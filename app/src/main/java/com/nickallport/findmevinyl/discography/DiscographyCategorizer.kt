package com.nickallport.findmevinyl.discography

data class ReleaseGroupDto(
    val id: String,
    val title: String,
    val primaryType: String?,
    val secondaryTypes: List<String>,
    val firstReleaseDate: String?
)

enum class DiscographyCategory {
    STUDIO_ALBUM, LIVE_ALBUM, COMPILATION, EP, SINGLE, OTHER
}

object DiscographyCategorizer {
    fun categorize(releaseGroup: ReleaseGroupDto): DiscographyCategory {
        val secondary = releaseGroup.secondaryTypes

        return when {
            secondary.contains("Live") -> DiscographyCategory.LIVE_ALBUM
            secondary.contains("Compilation") -> DiscographyCategory.COMPILATION
            releaseGroup.primaryType == "EP" -> DiscographyCategory.EP
            releaseGroup.primaryType == "Single" -> DiscographyCategory.SINGLE
            releaseGroup.primaryType == "Album" -> DiscographyCategory.STUDIO_ALBUM
            else -> DiscographyCategory.OTHER
        }
    }
}