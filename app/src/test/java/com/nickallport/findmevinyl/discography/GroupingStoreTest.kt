package com.nickallport.findmevinyl.discography

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupingStoreTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("grouping-test", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `nothing is grouped for an artist nothing was chosen for`() {
        assertEquals(Grouping.None, GroupingStore(prefs).get("a"))
    }

    @Test
    fun `remembers the choice for each artist separately`() {
        val store = GroupingStore(prefs)

        store.set("a", Grouping(owned = true, wanted = true, wantedFirst = true))

        assertEquals(Grouping(owned = true, wanted = true, wantedFirst = true), store.get("a"))
        assertEquals(Grouping.None, store.get("b"))
    }

    @Test
    fun `is kept by a new store on the same file, as after the app is closed`() {
        GroupingStore(prefs).set("a", Grouping(wanted = true))

        assertEquals(Grouping(wanted = true), GroupingStore(prefs).get("a"))
    }

    @Test
    fun `turning everything off forgets the artist`() {
        val store = GroupingStore(prefs)
        store.set("a", Grouping(owned = true))

        store.set("a", Grouping.None)

        assertEquals(Grouping.None, store.get("a"))
        assertTrue(prefs.all.isEmpty())
    }

    @Test
    fun `the order is kept while a group is off`() {
        val store = GroupingStore(prefs)

        store.set("a", Grouping(owned = false, wanted = true, wantedFirst = true))

        assertTrue(store.get("a").wantedFirst)
        assertFalse(store.get("a").owned)
    }

    @Test
    fun `an artist saved by the old Owned first switch is grouped by owned`() {
        // The old store kept a true boolean for each artist it was on for.
        prefs.edit().putBoolean("a", true).commit()

        assertEquals(Grouping(owned = true), GroupingStore(prefs).get("a"))
    }

    @Test
    fun `a value that cannot be read means nothing is grouped`() {
        prefs.edit().putString("a", "nonsense,owned,,x").commit()
        prefs.edit().putInt("b", 7).commit()

        assertEquals(Grouping(owned = true), GroupingStore(prefs).get("a"))
        assertEquals(Grouping.None, GroupingStore(prefs).get("b"))
    }
}
