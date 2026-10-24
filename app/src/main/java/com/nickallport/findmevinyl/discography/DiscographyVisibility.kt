package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel

/**
 * What the settings leave out of a discography.
 *
 * An album you own or want is never hidden. Otherwise it is hidden when its kind is hidden (live
 * albums, compilations, singles, other releases; studio albums and EPs cannot be), or when every
 * format it is known to have come out on is hidden: LP, CD, 12", 7", MC and Digital can be hidden, and
 * 10" cannot. A hidden format is also taken out of the album's formats, so the row and the
 * format chips do not show it. An album whose formats are not known yet is shown, because nothing can
 * be said about it until it is looked up.
 *
 * Bootlegs, promos and unknown status work on the statuses of an album's releases. An album is hidden
 * only when it has no official release and every one of its statuses is a hidden kind. Statuses that
 * are not known yet, or not listed, leave it shown.
 */
object DiscographyVisibility {

    data class Result(
        /** What is left, by category, in the same order. A category with nothing left is not in it. */
        val shown: Map<DiscographyCategory, List<AlbumUiModel>>,
        /** How many albums were left out. */
        val hiddenCount: Int
    )

    fun apply(grouped: Map<DiscographyCategory, List<AlbumUiModel>>, settings: Settings): Result {
        var hidden = 0
        val shown = LinkedHashMap<DiscographyCategory, List<AlbumUiModel>>()

        for ((category, albums) in grouped) {
            val kept = mutableListOf<AlbumUiModel>()
            for (album in albums) {
                when {
                    album.owned || album.wanted -> kept.add(withDisplayFormats(album, settings))
                    kindHidden(category, settings) || formatsHidden(album, settings) || statusHidden(album, settings) -> hidden++
                    else -> kept.add(withDisplayFormats(album, settings))
                }
            }
            if (kept.isNotEmpty()) shown[category] = kept
        }
        return Result(shown, hidden)
    }

    /**
     * The formats to show for an album: the known formats without the hidden ones. When that would leave
     * none (an album you own that is only on a hidden format) they are all kept, so the row is not blank.
     */
    fun displayFormats(formats: List<String>, settings: Settings): List<String> {
        val hidden = hiddenFormats(settings)
        return formats.filterNot { it in hidden }.ifEmpty { formats }
    }

    private fun withDisplayFormats(album: AlbumUiModel, settings: Settings): AlbumUiModel {
        val hidden = hiddenFormats(settings)
        // The formats the user added are left out when hidden, with nothing kept in their place.
        val userFormats = album.userFormats.filterNot { it in hidden }
        val formats = album.formats ?: return album.copy(userFormats = userFormats)
        return album.copy(formats = displayFormats(formats, settings), userFormats = userFormats)
    }

    private fun formatsHidden(album: AlbumUiModel, settings: Settings): Boolean {
        val formats = album.shownFormats?.takeIf { it.isNotEmpty() } ?: return false
        val hidden = hiddenFormats(settings)
        return formats.all { it in hidden }
    }

    private fun statusHidden(album: AlbumUiModel, settings: Settings): Boolean {
        val statuses = album.statuses?.takeIf { it.isNotEmpty() } ?: return false
        if (ReleaseStatuses.OFFICIAL in statuses) return false
        return statuses.all {
            when (it) {
                ReleaseStatuses.BOOTLEG -> settings.hideBootlegs
                ReleaseStatuses.PROMOTION -> settings.hidePromos
                else -> settings.hideUnknownStatus
            }
        }
    }

    private fun kindHidden(category: DiscographyCategory, settings: Settings): Boolean = when (category) {
        DiscographyCategory.LIVE_ALBUM -> settings.hideLiveAlbums
        DiscographyCategory.COMPILATION -> settings.hideCompilations
        DiscographyCategory.SINGLE -> settings.hideSingles
        DiscographyCategory.OTHER -> settings.hideOtherReleases
        DiscographyCategory.STUDIO_ALBUM, DiscographyCategory.EP, DiscographyCategory.SOUNDTRACK, DiscographyCategory.APPEARS_ON -> false
    }

    private fun hiddenFormats(settings: Settings): Set<String> = buildSet {
        if (settings.hideLps) add(ReleaseFormats.LP)
        if (settings.hideCds) add(ReleaseFormats.CD)
        if (settings.hideTwelveInches) add(ReleaseFormats.TWELVE_INCH)
        if (settings.hideSevenInches) add(ReleaseFormats.SEVEN_INCH)
        if (settings.hideMcs) add(ReleaseFormats.MC)
        if (settings.hideDigital) add(ReleaseFormats.DIGITAL)
    }
}
