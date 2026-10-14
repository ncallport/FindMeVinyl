package com.nickallport.findmevinyl.coverart

/** Cover Art Archive addresses. It is keyed by the MusicBrainz release group id. */
object CoverArt {
    const val BASE_URL = "https://coverartarchive.org"

    /** The sizes the archive offers that the app uses, in pixels. */
    const val SMALL = 250
    const val LARGE = 500

    /** The front cover, [size] pixels wide. The archive answers 404 when a release group has no art. */
    fun url(releaseGroupId: String, baseUrl: String = BASE_URL, size: Int = SMALL): String =
        "$baseUrl/release-group/$releaseGroupId/front-$size"
}
