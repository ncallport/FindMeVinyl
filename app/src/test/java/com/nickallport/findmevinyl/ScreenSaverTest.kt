package com.nickallport.findmevinyl

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScreenSaverTest {

    private val scope = SaverScope { true }

    private fun roundTrip(screen: Screen): Screen? {
        val saved = with(ScreenSaver) { scope.save(screen) }
        assertNotNull(saved)
        return ScreenSaver.restore(saved!!)
    }

    @Test
    fun simpleScreensRoundTrip() {
        listOf(Screen.Search, Screen.Favourites, Screen.WantList, Screen.Shops).forEach {
            assertEquals(it, roundTrip(it))
        }
    }

    @Test
    fun homeRoundTrips() {
        assertEquals(Screen.Home, roundTrip(Screen.Home))
    }

    @Test
    fun backFromTheFrontScreenLeavesTheApp() {
        assertEquals(null, Screen.Home.backTarget())
    }

    @Test
    fun backFromAnyPageGoesToTheFrontScreen() {
        listOf(Screen.Search, Screen.Favourites, Screen.WantList, Screen.Shops).forEach {
            assertEquals(Screen.Home, it.backTarget())
        }
    }

    @Test
    fun backFromADiscographyGoesWhereYouCameFrom() {
        assertEquals(Screen.Search, Screen.Discography("id", "Name", Screen.Search).backTarget())
        assertEquals(Screen.Favourites, Screen.Discography("id", "Name", Screen.Favourites).backTarget())
    }

    @Test
    fun discographyKeepsIdAndName() {
        val screen = Screen.Discography("id-1", "Artist One", Screen.Search)
        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun discographyKeepsWhereBackGoes() {
        val screen = Screen.Discography("id-1", "Artist One", Screen.Favourites)
        val restored = roundTrip(screen) as Screen.Discography
        assertEquals(Screen.Favourites, restored.backTo)
    }
}
