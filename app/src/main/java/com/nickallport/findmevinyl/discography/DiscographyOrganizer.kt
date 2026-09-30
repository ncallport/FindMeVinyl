package com.nickallport.findmevinyl.discography

object DiscographyOrganizer {
    fun organize(releaseGroups: List<ReleaseGroupDto>): Map<DiscographyCategory, List<ReleaseGroupDto>> {
        return releaseGroups
            .groupBy { DiscographyCategorizer.categorize(it) }
            .mapValues { (_, groups) ->
                groups.sortedWith(compareBy(nullsLast()) { it.firstReleaseDate })
            }
    }
}