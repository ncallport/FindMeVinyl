package com.nickallport.findmevinyl.settings

/**
 * What the user has chosen to hide. Every setting is "hide X", and the defaults are the user's own
 * choice. Albums the user owns or wants are never hidden, whatever is set here.
 */
data class Settings(
    // Which kinds of album to hide, from the discography.
    val hideLiveAlbums: Boolean = false,
    val hideCompilations: Boolean = false,
    val hideSingles: Boolean = false,
    val hideOtherReleases: Boolean = false,

    // Which formats to treat as not there. An album is hidden when every format it came out on is hidden.
    val hideLps: Boolean = false,
    val hideCds: Boolean = false,
    val hideTwelveInches: Boolean = false,
    val hideSevenInches: Boolean = false,
    val hideDigital: Boolean = true,

    // Albums with no official release, by what their releases are called on MusicBrainz.
    val hideBootlegs: Boolean = true,
    val hidePromos: Boolean = true,
    val hideUnknownStatus: Boolean = true,

    // Shops.
    val hideCharityShops: Boolean = true
) {
    companion object {
        /** Nothing hidden: for a screen or a test that is not about the settings. */
        val ShowEverything = Settings(
            hideLiveAlbums = false, hideCompilations = false, hideSingles = false, hideOtherReleases = false,
            hideLps = false, hideCds = false, hideTwelveInches = false, hideSevenInches = false, hideDigital = false,
            hideBootlegs = false, hidePromos = false, hideUnknownStatus = false, hideCharityShops = false
        )
    }
}
