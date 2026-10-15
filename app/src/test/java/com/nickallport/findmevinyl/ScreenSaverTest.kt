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
        listOf(Screen.Search, Screen.WantList, Screen.Shops).forEach {
            assertEquals(it, roundTrip(it))
        }
    }

    @Test
    fun theResultsPageKeepsItsQuery() {
        assertEquals(Screen.ArtistResults("Roxy Music"), roundTrip(Screen.ArtistResults("Roxy Music")))
    }

    @Test
    fun aQueryWithAnyCharactersSurvives() {
        val screen = Screen.ArtistResults("AC/DC | Sigur Rós, \"live\"")
        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun backFromTheResultsGoesToTheArtistsPage() {
        assertEquals(Screen.Search, Screen.ArtistResults("Genesis").backTarget())
    }

    @Test
    fun aDiscographyOpenedFromResultsGoesBackToThoseResults() {
        val results = Screen.ArtistResults("Genesis")
        val screen = Screen.Discography("id", "Genesis", results)

        assertEquals(results, screen.backTarget())
        assertEquals(screen, roundTrip(screen))
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
        listOf(Screen.Search, Screen.WantList, Screen.Shops).forEach {
            assertEquals(Screen.Home, it.backTarget())
        }
    }

    @Test
    fun backFromADiscographyGoesWhereYouCameFrom() {
        assertEquals(Screen.Search, Screen.Discography("id", "Name", Screen.Search).backTarget())
        assertEquals(Screen.Shops, Screen.Discography("id", "Name", Screen.Shops).backTarget())
    }

    @Test
    fun discographyKeepsIdAndName() {
        val screen = Screen.Discography("id-1", "Artist One", Screen.Search)
        assertEquals(screen, roundTrip(screen))
    }

    private val discography = Screen.Discography("artist-1", "Genesis", Screen.ArtistResults("Genesis"))

    @Test
    fun anAlbumPageGoesBackToItsDiscography() {
        val screen = Screen.AlbumDetail("album-1", "Trespass", "artist-1", "Genesis", discography)

        assertEquals(discography, screen.backTarget())
    }

    @Test
    fun anAlbumPageRoundTripsWithEverythingItNeeds() {
        val screen = Screen.AlbumDetail("album-1", "Trespass", "artist-1", "Genesis", discography)
        val restored = roundTrip(screen) as Screen.AlbumDetail

        assertEquals(screen, restored)
        // The whole chain back is kept, so Back still works after a rotation.
        assertEquals(Screen.ArtistResults("Genesis"), (restored.backTo as Screen.Discography).backTo)
    }

    @Test
    fun anAlbumTitleWithAnyCharactersSurvives() {
        val screen = Screen.AlbumDetail("album-1", "Live | \"1973\", Vol. 2 / Pt. 1", "artist-1", "AC/DC", discography)

        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun discographyKeepsWhereBackGoes() {
        val screen = Screen.Discography("id-1", "Artist One", Screen.ArtistResults("One"))
        val restored = roundTrip(screen) as Screen.Discography
        assertEquals(Screen.ArtistResults("One"), restored.backTo)
    }

    @Test
    fun theSettingsPageRoundTrips() {
        assertEquals(Screen.Settings, roundTrip(Screen.Settings))
    }

    @Test
    fun backFromTheSettingsPageGoesToTheFrontScreen() {
        assertEquals(Screen.Home, Screen.Settings.backTarget())
    }
}
