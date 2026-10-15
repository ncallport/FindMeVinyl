package com.nickallport.findmevinyl.discography

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OwnedFirstStoreTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("owned-first-test", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `is off for an artist nothing was chosen for`() {
        assertFalse(OwnedFirstStore(prefs).isOn("a"))
    }

    @Test
    fun `remembers the choice for each artist separately`() {
        val store = OwnedFirstStore(prefs)

        store.set("a", true)

        assertTrue(store.isOn("a"))
        assertFalse(store.isOn("b"))
    }

    @Test
    fun `is kept by a new store on the same file, as after the app is closed`() {
        OwnedFirstStore(prefs).set("a", true)

        assertTrue(OwnedFirstStore(prefs).isOn("a"))
    }

    @Test
    fun `turning it off forgets it`() {
        val store = OwnedFirstStore(prefs)
        store.set("a", true)

        store.set("a", false)

        assertFalse(store.isOn("a"))
        assertTrue(prefs.all.isEmpty())
    }
}
