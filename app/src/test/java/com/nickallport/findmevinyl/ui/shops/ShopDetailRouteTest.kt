package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/** The shop page looks up the public holidays of the region the shop is in, so its status can follow them. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class ShopDetailRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val monday = LocalDateTime.of(2026, 10, 12, 12, 0)
    private val shop = Shop("1", "Louie Louie", ShopCategory.RECORD_SHOP, 38.71, -9.14, "Mo-Sa 11:00-19:30; Su,PH 15:00-19:30")

    private fun show(region: HolidayRegion?, loadPublicHolidays: suspend (HolidayRegion) -> Set<LocalDate>?) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                ShopDetailRoute(
                    shop = shop, region = region, now = monday, loadPublicHolidays = loadPublicHolidays,
                    onOpenMap = {}, onCall = {}, onOpenWebsite = {}, onSendEmail = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `asks for the holidays of the shop's region and uses them`() {
        val asked = mutableListOf<HolidayRegion>()
        val region = HolidayRegion("PT", null)

        show(region) { asked.add(it); setOf(monday.toLocalDate()) }

        assertEquals(listOf(region), asked)
        composeTestRule.onNodeWithTag("shop_detail_status")
            .assertTextEquals("Closed, opens 15:00 (public holiday today)")
    }

    @Test
    fun `without the holidays it says that holidays may differ`() {
        show(HolidayRegion("PT", null)) { null }

        composeTestRule.onNodeWithTag("shop_detail_status")
            .assertTextEquals("Open now, closes 19:30 (public holidays may differ)")
    }

    @Test
    fun `with no region it asks for nothing`() {
        var asked = 0

        show(null) { asked++; emptySet() }

        assertEquals(0, asked)
    }

    private fun showFavourite(
        loadIsFavourite: suspend () -> Boolean,
        onSetFavourite: (suspend (Boolean) -> Unit)?
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                ShopDetailRoute(
                    shop = shop, region = null, now = monday, loadPublicHolidays = { null },
                    onOpenMap = {}, onCall = {}, onOpenWebsite = {}, onSendEmail = {},
                    loadIsFavourite = loadIsFavourite, onSetFavourite = onSetFavourite
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `a shop that is kept shows as a favourite`() {
        showFavourite(loadIsFavourite = { true }, onSetFavourite = {})

        composeTestRule.onNodeWithTag("shop_detail_favourite").assertTextEquals("Favourite")
    }

    @Test
    fun `tapping the button keeps the shop and then shows it as a favourite`() {
        val set = mutableListOf<Boolean>()
        showFavourite(loadIsFavourite = { false }, onSetFavourite = { set.add(it) })

        composeTestRule.onNodeWithTag("shop_detail_favourite").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(true), set)
        composeTestRule.onNodeWithTag("shop_detail_favourite").assertTextEquals("Favourite")
    }

    @Test
    fun `tapping it again lets the shop go`() {
        val set = mutableListOf<Boolean>()
        showFavourite(loadIsFavourite = { true }, onSetFavourite = { set.add(it) })

        composeTestRule.onNodeWithTag("shop_detail_favourite").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(false), set)
        composeTestRule.onNodeWithTag("shop_detail_favourite").assertTextEquals("Add to favourites")
    }
}
