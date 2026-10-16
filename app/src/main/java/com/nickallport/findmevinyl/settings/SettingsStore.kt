package com.nickallport.findmevinyl.settings

import android.content.SharedPreferences

/**
 * Keeps [Settings] on the phone, in the app's preferences file, one yes-or-no value for each setting.
 * Every setting is saved, including one left at its default, so a choice the user made still stands if a
 * later version changes the default. A value of the wrong kind reads as the default. Values it does not
 * know about are left alone.
 */
class SettingsStore(private val prefs: SharedPreferences) {

    fun load(): Settings {
        val defaults = Settings()
        fun flag(key: String, default: Boolean): Boolean = try {
            prefs.getBoolean(key, default)
        } catch (e: ClassCastException) {
            default
        }

        return Settings(
            hideLiveAlbums = flag("hideLiveAlbums", defaults.hideLiveAlbums),
            hideCompilations = flag("hideCompilations", defaults.hideCompilations),
            hideSingles = flag("hideSingles", defaults.hideSingles),
            hideOtherReleases = flag("hideOtherReleases", defaults.hideOtherReleases),
            hideLps = flag("hideLps", defaults.hideLps),
            hideCds = flag("hideCds", defaults.hideCds),
            hideTwelveInches = flag("hideTwelveInches", defaults.hideTwelveInches),
            hideSevenInches = flag("hideSevenInches", defaults.hideSevenInches),
            hideMcs = flag("hideMcs", defaults.hideMcs),
            hideDigital = flag("hideDigital", defaults.hideDigital),
            hideBootlegs = flag("hideBootlegs", defaults.hideBootlegs),
            hidePromos = flag("hidePromos", defaults.hidePromos),
            hideUnknownStatus = flag("hideUnknownStatus", defaults.hideUnknownStatus),
            hideCharityShops = flag("hideCharityShops", defaults.hideCharityShops),
            hideSecondHandShops = flag("hideSecondHandShops", defaults.hideSecondHandShops)
        )
    }

    fun save(settings: Settings) {
        prefs.edit()
            .putBoolean("hideLiveAlbums", settings.hideLiveAlbums)
            .putBoolean("hideCompilations", settings.hideCompilations)
            .putBoolean("hideSingles", settings.hideSingles)
            .putBoolean("hideOtherReleases", settings.hideOtherReleases)
            .putBoolean("hideLps", settings.hideLps)
            .putBoolean("hideCds", settings.hideCds)
            .putBoolean("hideTwelveInches", settings.hideTwelveInches)
            .putBoolean("hideSevenInches", settings.hideSevenInches)
            .putBoolean("hideMcs", settings.hideMcs)
            .putBoolean("hideDigital", settings.hideDigital)
            .putBoolean("hideBootlegs", settings.hideBootlegs)
            .putBoolean("hidePromos", settings.hidePromos)
            .putBoolean("hideUnknownStatus", settings.hideUnknownStatus)
            .putBoolean("hideCharityShops", settings.hideCharityShops)
            .putBoolean("hideSecondHandShops", settings.hideSecondHandShops)
            .apply()
    }
}
