package com.nickallport.findmevinyl.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("settings-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private val everythingTheOtherWay = Settings(
        hideLiveAlbums = true, hideCompilations = true, hideSingles = true, hideOtherReleases = true,
        hideLps = true, hideCds = true, hideTwelveInches = true, hideSevenInches = true, hideMcs = true,
        hideDigital = false, hideBootlegs = false, hidePromos = false, hideUnknownStatus = false,
        hideCharityShops = false, hideSecondHandShops = true
    )

    @Test
    fun `gives the defaults when nothing has been saved`() {
        assertEquals(Settings(), SettingsStore(prefs).load())
    }

    @Test
    fun `gives back what was saved, every setting`() {
        SettingsStore(prefs).save(everythingTheOtherWay)

        assertEquals(everythingTheOtherWay, SettingsStore(prefs).load())
    }

    @Test
    fun `what was saved is still there for a new store on the same file, as after the app restarts`() {
        SettingsStore(prefs).save(Settings(hideCds = true))

        val again = SettingsStore(
            ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("settings-test", Context.MODE_PRIVATE)
        )

        assertEquals(true, again.load().hideCds)
    }

    @Test
    fun `saving one change keeps the rest`() {
        val store = SettingsStore(prefs)
        store.save(Settings(hideLps = true))
        store.save(store.load().copy(hideCharityShops = false))

        val loaded = store.load()
        assertEquals(true, loaded.hideLps)
        assertEquals(false, loaded.hideCharityShops)
        assertEquals(true, loaded.hideDigital)
    }

    @Test
    fun `a setting saved as the default is kept as that, not forgotten`() {
        val store = SettingsStore(prefs)
        store.save(Settings(hideDigital = false))
        // If a later version changes the default, a choice the user made must still stand.
        assertEquals(true, prefs.contains("hideDigital"))
        assertEquals(false, store.load().hideDigital)
    }

    @Test
    fun `a value of the wrong kind falls back to the default instead of crashing`() {
        prefs.edit().putString("hideCds", "yes").putString("hideDigital", "no").commit()

        val loaded = SettingsStore(prefs).load()

        assertEquals(false, loaded.hideCds)
        assertEquals(true, loaded.hideDigital)
    }

    @Test
    fun `settings it does not know about are left alone`() {
        prefs.edit().putBoolean("somethingFromALaterVersion", true).commit()

        SettingsStore(prefs).save(Settings(hideCds = true))

        assertEquals(true, prefs.getBoolean("somethingFromALaterVersion", false))
    }
}
