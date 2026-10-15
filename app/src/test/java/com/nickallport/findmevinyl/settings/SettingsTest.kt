package com.nickallport.findmevinyl.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    private val defaults = Settings()

    @Test
    fun `the defaults of the discography settings are the ones the user chose`() {
        assertEquals(false, defaults.hideLiveAlbums)
        assertEquals(false, defaults.hideCompilations)
        assertEquals(false, defaults.hideSingles)
        assertEquals(false, defaults.hideOtherReleases)
        assertEquals(false, defaults.hideLps)
        assertEquals(false, defaults.hideCds)
        assertEquals(false, defaults.hideTwelveInches)
        assertEquals(false, defaults.hideSevenInches)
        assertEquals(false, defaults.hideMcs)
        assertEquals(true, defaults.hideDigital)
        assertEquals(true, defaults.hideBootlegs)
        assertEquals(true, defaults.hidePromos)
        assertEquals(true, defaults.hideUnknownStatus)
    }

    @Test
    fun `the default of the location settings is the one the user chose`() {
        assertEquals(true, defaults.hideCharityShops)
    }

    @Test
    fun `a changed copy changes only what was asked`() {
        val changed = defaults.copy(hideCds = true)

        assertEquals(true, changed.hideCds)
        assertEquals(defaults.copy(hideCds = true), changed)
        assertEquals(false, changed.hideLps)
    }
}
