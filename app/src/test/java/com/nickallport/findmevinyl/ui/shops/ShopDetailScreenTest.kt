package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopAddress
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

/** A shop's own page: what OpenStreetMap knows about it, with ways to call, browse, write or find it. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class ShopDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // A Monday, at noon.
    private val monday = LocalDateTime.of(2026, 10, 12, 12, 0)

    private val louie = Shop(
        id = "1", name = "Louie Louie", category = ShopCategory.RECORD_SHOP, latitude = 38.71, longitude = -9.14,
        openingHours = "Mo-Sa 11:00-19:30; Su,PH 15:00-19:30",
        address = ShopAddress("Escadinhas do Santo Espírito da Pedreira", "3", "1100-225", "Lisboa", "PT"),
        phone = "+351 213 472 232", website = "https://www.louielouie.biz/", email = "louielouie.lisboa@gmail.com"
    )

    private fun show(
        shop: Shop = louie,
        fallbackCountry: String? = null,
        isPublicHoliday: ((LocalDate) -> Boolean)? = null,
        onOpenMap: () -> Unit = {},
        onCall: (String) -> Unit = {},
        onOpenWebsite: (String) -> Unit = {},
        onSendEmail: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                ShopDetailScreen(
                    shop = shop, fallbackCountry = fallbackCountry, now = monday, isPublicHoliday = isPublicHoliday,
                    onOpenMap = onOpenMap, onCall = onCall, onOpenWebsite = onOpenWebsite, onSendEmail = onSendEmail
                )
            }
        }
    }

    @Test
    fun `shows the name and the kind of shop`() {
        show()

        composeTestRule.onNodeWithTag("shop_detail_name").assertTextEquals("Louie Louie")
        composeTestRule.onNodeWithTag("shop_detail_kind").assertTextEquals("Record Shop")
    }

    @Test
    fun `says when a shop is second-hand only`() {
        show(louie.copy(secondHandOnly = true))

        composeTestRule.onNodeWithTag("shop_detail_kind").assertTextEquals("Record Shop, second-hand only")
    }

    @Test
    fun `shows the address in two lines`() {
        show()

        composeTestRule.onNodeWithTag("shop_detail_address")
            .assertTextEquals("Escadinhas do Santo Espírito da Pedreira 3\n1100-225 Lisboa")
    }

    @Test
    fun `an address without a country follows the country of the search`() {
        show(
            louie.copy(address = ShopAddress("Berwick Street", "34", "W1F 8RP", "London")),
            fallbackCountry = "GB"
        )

        composeTestRule.onNodeWithTag("shop_detail_address").assertTextEquals("34 Berwick Street\nLondon W1F 8RP")
    }

    @Test
    fun `leaves out what is not known`() {
        show(Shop("2", "Plain Records", ShopCategory.RECORD_SHOP, 1.0, 2.0, null))

        composeTestRule.onNodeWithTag("shop_detail_address").assertDoesNotExist()
        composeTestRule.onNodeWithTag("shop_detail_phone").assertDoesNotExist()
        composeTestRule.onNodeWithTag("shop_detail_website").assertDoesNotExist()
        composeTestRule.onNodeWithTag("shop_detail_email").assertDoesNotExist()
    }

    @Test
    fun `tapping the phone number dials it`() {
        var dialled: String? = null
        show(onCall = { dialled = it })

        composeTestRule.onNodeWithTag("shop_detail_phone").performScrollTo().performClick()

        assertEquals("tel:+351213472232", dialled)
    }

    @Test
    fun `tapping the website opens it`() {
        var opened: String? = null
        show(onOpenWebsite = { opened = it })

        composeTestRule.onNodeWithTag("shop_detail_website").performScrollTo().performClick()

        assertEquals("https://www.louielouie.biz/", opened)
    }

    @Test
    fun `tapping the email starts a message`() {
        var written: String? = null
        show(onSendEmail = { written = it })

        composeTestRule.onNodeWithTag("shop_detail_email").performScrollTo().performClick()

        assertEquals("mailto:louielouie.lisboa@gmail.com", written)
    }

    @Test
    fun `a website that is not a web address is shown as text and opens nothing`() {
        var opened: String? = null
        show(louie.copy(website = "javascript:alert(1)"), onOpenWebsite = { opened = it })

        composeTestRule.onNodeWithTag("shop_detail_website").performScrollTo().performClick()

        assertEquals(null, opened)
    }

    @Test
    fun `shows whether the shop is open now, and says holidays may differ when they are not known`() {
        show()

        composeTestRule.onNodeWithTag("shop_detail_status")
            .assertTextEquals("Open now, closes 19:30 (public holidays may differ)")
    }

    @Test
    fun `with the holidays known the status is plain`() {
        show(isPublicHoliday = { false })

        composeTestRule.onNodeWithTag("shop_detail_status").assertTextEquals("Open now, closes 19:30")
    }

    @Test
    fun `lists the hours of each day of the week`() {
        show()

        composeTestRule.onNodeWithTag("hours_day_MONDAY").performScrollTo().assertTextEquals("Monday", "11:00-19:30")
        composeTestRule.onNodeWithTag("hours_day_SATURDAY").assertTextEquals("Saturday", "11:00-19:30")
        composeTestRule.onNodeWithTag("hours_day_SUNDAY").assertTextEquals("Sunday", "15:00-19:30")
    }

    @Test
    fun `says what the public holidays are`() {
        show()

        composeTestRule.onNodeWithTag("hours_public_holidays").performScrollTo()
            .assertTextEquals("Public holidays", "15:00-19:30")
    }

    @Test
    fun `no public holiday line when the hours do not name them`() {
        show(louie.copy(openingHours = "Mo-Sa 11:00-19:30"))

        composeTestRule.onNodeWithTag("hours_public_holidays").assertDoesNotExist()
    }

    @Test
    fun `hours that cannot be read are shown as they were written`() {
        show(louie.copy(openingHours = "sunrise-sunset"))

        composeTestRule.onNodeWithTag("hours_raw").performScrollTo().assertTextEquals("sunrise-sunset")
        composeTestRule.onNodeWithTag("hours_day_MONDAY").assertDoesNotExist()
    }

    @Test
    fun `says so when the hours are not known`() {
        show(louie.copy(openingHours = null))

        composeTestRule.onNodeWithText("Opening hours not known").performScrollTo().assertExists()
    }

    @Test
    fun `has a button that opens the shop on a map`() {
        var opened = 0
        show(onOpenMap = { opened++ })

        composeTestRule.onNodeWithTag("shop_detail_map").performScrollTo().performClick()

        assertEquals(1, opened)
    }

    private fun showWithFavourite(isFavourite: Boolean, onToggle: (() -> Unit)?) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                ShopDetailScreen(
                    shop = louie, now = monday, onOpenMap = {}, onCall = {}, onOpenWebsite = {}, onSendEmail = {},
                    isFavourite = isFavourite, onToggleFavourite = onToggle
                )
            }
        }
    }

    @Test
    fun `offers to add the shop to the favourites`() {
        showWithFavourite(isFavourite = false, onToggle = {})

        composeTestRule.onNodeWithTag("shop_detail_favourite").assertTextEquals("Add to favourites")
    }

    @Test
    fun `shows that the shop is a favourite`() {
        showWithFavourite(isFavourite = true, onToggle = {})

        composeTestRule.onNodeWithTag("shop_detail_favourite").assertTextEquals("Favourite")
    }

    @Test
    fun `tapping the favourite button asks to switch it`() {
        var toggled = 0
        showWithFavourite(isFavourite = false, onToggle = { toggled++ })

        composeTestRule.onNodeWithTag("shop_detail_favourite").performClick()

        assertEquals(1, toggled)
    }

    @Test
    fun `with no way to keep a shop there is no button`() {
        showWithFavourite(isFavourite = false, onToggle = null)

        composeTestRule.onNodeWithTag("shop_detail_favourite").assertDoesNotExist()
    }
}
