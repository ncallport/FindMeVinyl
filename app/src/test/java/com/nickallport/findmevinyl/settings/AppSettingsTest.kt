package com.nickallport.findmevinyl.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSettingsTest {

    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("app-settings-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = SettingsStore(prefs)
    }

    @Test
    fun `starts with what is saved`() {
        store.save(Settings(hideCds = true))

        assertEquals(true, AppSettings(store).settings.value.hideCds)
    }

    @Test
    fun `an update changes the current settings at once`() {
        val app = AppSettings(store)

        app.update { it.copy(hideLps = true) }

        assertEquals(true, app.settings.value.hideLps)
    }

    @Test
    fun `an update is saved, so it is still there next time`() {
        AppSettings(store).update { it.copy(hideSevenInches = true) }

        assertEquals(true, AppSettings(store).settings.value.hideSevenInches)
    }

    @Test
    fun `updates build on each other`() {
        val app = AppSettings(store)

        app.update { it.copy(hideLps = true) }
        app.update { it.copy(hideCds = true) }

        assertEquals(Settings(hideLps = true, hideCds = true), app.settings.value)
    }

    @Test
    fun `setting everything in one go works too`() {
        val app = AppSettings(store)

        app.set(Settings(hideCharityShops = false))

        assertEquals(false, app.settings.value.hideCharityShops)
        assertEquals(false, store.load().hideCharityShops)
    }
}
