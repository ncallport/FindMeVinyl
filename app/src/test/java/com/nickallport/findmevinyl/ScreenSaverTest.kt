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

    @Test
    fun aShopPageKeepsTheShopAndItsRegion() {
        val shop = com.nickallport.findmevinyl.shops.Shop(
            id = "9", name = "Louie Louie", category = com.nickallport.findmevinyl.shops.ShopCategory.RECORD_SHOP,
            latitude = 38.71, longitude = -9.14, openingHours = "Mo-Sa 11:00-19:30",
            address = com.nickallport.findmevinyl.shops.ShopAddress("Rua", "3", "1100-225", "Lisboa", "PT"),
            phone = "+351 213 472 232", website = "https://www.louielouie.biz/", email = "a@b.pt", secondHandOnly = true
        )
        val region = com.nickallport.findmevinyl.holidays.HolidayRegion("PT", "PT-11")

        assertEquals(Screen.ShopDetail(shop, region), roundTrip(Screen.ShopDetail(shop, region)))
    }

    @Test
    fun aShopPageWithNoRegionKeepsNone() {
        val shop = com.nickallport.findmevinyl.shops.Shop(
            "9", "Plain Records", com.nickallport.findmevinyl.shops.ShopCategory.CHARITY_SHOP, 1.0, 2.0, null
        )

        assertEquals(Screen.ShopDetail(shop, null), roundTrip(Screen.ShopDetail(shop, null)))
    }

    @Test
    fun aShopPageGoesBackToTheShops() {
        val shop = com.nickallport.findmevinyl.shops.Shop(
            "9", "Plain Records", com.nickallport.findmevinyl.shops.ShopCategory.RECORD_SHOP, 1.0, 2.0, null
        )

        assertEquals(Screen.Shops, Screen.ShopDetail(shop, null).backTarget())
    }

    @Test
    fun theFavouriteShopsPageRoundTripsAndGoesBackToTheShops() {
        assertEquals(Screen.FavouriteShops, roundTrip(Screen.FavouriteShops))
        assertEquals(Screen.Shops, Screen.FavouriteShops.backTarget())
    }

    @Test
    fun aShopOpenedFromTheFavouritesGoesBackToThem() {
        val shop = com.nickallport.findmevinyl.shops.Shop(
            "node/9", "Carbono", com.nickallport.findmevinyl.shops.ShopCategory.RECORD_SHOP, 1.0, 2.0, null
        )
        val screen = Screen.ShopDetail(shop, null, backTo = Screen.FavouriteShops)

        assertEquals(Screen.FavouriteShops, screen.backTarget())
        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun theAddAShopPageKeepsTheTextShared() {
        val text = "Silver Tentacle Merchandising\nR. Raul Proença 56a, 2500 Caldas da Rainha\nhttps://maps.app.goo.gl/x"

        assertEquals(Screen.AddShop(text), roundTrip(Screen.AddShop(text)))
        assertEquals(Screen.AddShop(null), roundTrip(Screen.AddShop(null)))
    }

    @Test
    fun theAddAShopPageGoesBackToTheFavouriteShops() {
        assertEquals(Screen.FavouriteShops, Screen.AddShop(null).backTarget())
    }

    @Test
    fun theEditPageKeepsTheShopBeingEdited() {
        val shop = com.nickallport.findmevinyl.shops.Shop(
            "custom/abc", "Silver Tentacle", com.nickallport.findmevinyl.shops.ShopCategory.RECORD_SHOP, 39.4, -9.1, "Tu-Sa 11:00-19:00",
            address = com.nickallport.findmevinyl.shops.ShopAddress(street = "R. Raul Proença 56a", city = "Caldas da Rainha"),
            approximatePosition = true, mapQuery = "Silver Tentacle, Caldas da Rainha"
        )

        assertEquals(Screen.AddShop(null, shop), roundTrip(Screen.AddShop(null, shop)))
        assertEquals(Screen.FavouriteShops, Screen.AddShop(null, shop).backTarget())
    }

    @Test
    fun theJoinPageKeepsTheArtistAndWhereItWasOpenedFrom() {
        val screen = Screen.JoinArtists("cave", "Nick Cave", Screen.Discography("cave", "Nick Cave", Screen.Search))
        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun aNameWithAnyCharactersSurvivesOnTheJoinPage() {
        val screen = Screen.JoinArtists("x", "AC/DC | Sigur Rós, \"live\"", Screen.Search)
        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun backFromTheJoinPageGoesToTheDiscographyItWasOpenedFrom() {
        val discography = Screen.Discography("cave", "Nick Cave", Screen.Search)
        assertEquals(discography, Screen.JoinArtists("cave", "Nick Cave", discography).backTarget())
    }

    @Test
    fun theAddAlbumPageKeepsItsArtistAndWhereItCameFrom() {
        val back = Screen.Discography("artist-1", "Big Country", Screen.Search)
        val screen = Screen.AddAlbum("artist-1", "Big Country", null, back)

        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun theEditAlbumPageKeepsTheAlbumBeingChanged() {
        val discography = Screen.Discography("artist-1", "Big Country", Screen.Search)
        val back = Screen.AlbumDetail("custom/1", "Live in a Pub", "artist-1", "Big Country", discography)
        val screen = Screen.AddAlbum("artist-1", "Big Country", "custom/1", back)

        assertEquals(screen, roundTrip(screen))
    }

    @Test
    fun backFromTheAddAlbumPageGoesToWhereItCameFrom() {
        val back = Screen.Discography("artist-1", "Big Country", Screen.Search)

        assertEquals(back, Screen.AddAlbum("artist-1", "Big Country", null, back).backTarget())
    }

    @Test
    fun theBarcodePageIsKept() {
        assertEquals(Screen.Barcode, roundTrip(Screen.Barcode))
    }

    @Test
    fun backFromTheBarcodePageGoesToTheArtistsPage() {
        assertEquals(Screen.Search, Screen.Barcode.backTarget())
    }
}
