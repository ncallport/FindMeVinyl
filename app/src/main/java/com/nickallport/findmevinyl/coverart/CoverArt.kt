package com.nickallport.findmevinyl.coverart

/** Cover Art Archive addresses. It is keyed by the MusicBrainz release group id. */
object CoverArt {
    const val BASE_URL = "https://coverartarchive.org"

    /** The 250px front cover. The archive answers 404 when a release group has no art. */
    fun url(releaseGroupId: String, baseUrl: String = BASE_URL): String =
        "$baseUrl/release-group/$releaseGroupId/front-250"
}
