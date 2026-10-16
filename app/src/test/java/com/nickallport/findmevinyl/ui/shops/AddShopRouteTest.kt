package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.nickallport.findmevinyl.nominatim.Coordinates
import com.nickallport.findmevinyl.shops.FoundPosition
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopAddress
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Adding a shop that OpenStreetMap does not have, from what Google Maps shares or from what the user types. */
@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class AddShopRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val link = "https://maps.app.goo.gl/Q4Xy2CQNv29QYFyCA"
    private val shared = "Silver Tentacle Merchandising\nR. Raul Proença 56a, 2500 Caldas da Rainha\n$link"
    private val town = Coordinates(39.4072, -9.1346)
    private val street = Coordinates(39.4041, -9.1311)

    private val asked = mutableListOf<String>()
    private val resolved = mutableListOf<String>()
    private val saved = mutableListOf<Shop>()

    private fun show(
        editing: Shop? = null,
        sharedText: String? = null,
        resolveLink: suspend (String) -> String? = { resolved.add(it); null },
        findPosition: suspend (String) -> FoundPosition? = { asked.add(it); FoundPosition(street, false, it) },
        currentPosition: suspend () -> Coordinates? = { null }
    ) {
        composeTestRule.setContent {
            FindMeVinylTheme {
                AddShopRoute(
                    sharedText = sharedText, editing = editing, resolveLink = resolveLink, findPosition = findPosition,
                    currentPosition = currentPosition, onSave = { saved.add(it) }, newId = { "custom/test" }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun paste(text: String) {
        composeTestRule.onNodeWithTag("add_shop_paste").performScrollTo().performTextInput(text)
        composeTestRule.onNodeWithTag("add_shop_read").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `starts empty and cannot be saved yet`() {
        show()

        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("add_shop_position").performScrollTo().assertTextEquals("No position yet")
    }

    @Test
    fun `text shared from Maps fills in the name and the address and finds the street`() {
        show()

        paste(shared)

        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().assertTextContains("Silver Tentacle Merchandising", substring = true)
        composeTestRule.onNodeWithTag("add_shop_address").assertTextContains("R. Raul Proença 56a, 2500 Caldas da Rainha", substring = true)
        assertEquals(listOf("R. Raul Proença 56a, 2500 Caldas da Rainha"), asked)
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("39.4041, -9.1311", substring = true)
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("found from the address", substring = true)
        composeTestRule.onNodeWithTag("add_shop_save").assertIsEnabled()
    }

    @Test
    fun `when only the town is found the position says it is approximate`() {
        show(findPosition = { FoundPosition(town, approximate = true, place = "Caldas da Rainha") })

        paste(shared)

        composeTestRule.onNodeWithTag("add_shop_position").performScrollTo().assertTextContains("approximate", substring = true)
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("Caldas da Rainha", substring = true)
        composeTestRule.onNodeWithTag("add_shop_save").assertIsEnabled()
    }

    @Test
    fun `when the address is not found it says so, and the shop cannot be saved without a position`() {
        show(findPosition = { null })

        paste(shared)

        composeTestRule.onNodeWithTag("add_shop_status").performScrollTo().assertTextContains("Couldn't find that address", substring = true)
        composeTestRule.onNodeWithTag("add_shop_save").assertIsNotEnabled()
    }

    @Test
    fun `pasted coordinates give an exact position without looking anything up`() {
        show()

        paste("39.4036, -9.1349")

        composeTestRule.onNodeWithTag("add_shop_position").performScrollTo().assertTextContains("39.4036, -9.1349", substring = true)
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("exact", substring = true)
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `a short link is followed to read the name and the address from where it leads`() {
        val place = "https://www.google.com/maps/place/Silver+Tentacle+Merchandising,+R.+Raul+Proen%C3%A7a+56a+2500,+Caldas+da+Rainha/data=!4m2"
        show(resolveLink = { resolved.add(it); place })

        paste(link)

        assertEquals(listOf(link), resolved)
        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().assertTextContains("Silver Tentacle Merchandising", substring = true)
        composeTestRule.onNodeWithTag("add_shop_address").assertTextContains("R. Raul Proença 56a 2500, Caldas da Rainha", substring = true)
    }

    @Test
    fun `text that already has the name and address is not sent to Google at all`() {
        show()

        paste(shared)

        assertEquals(emptyList<String>(), resolved)
    }

    @Test
    fun `a link that cannot be followed says so, and the form can still be filled in by hand`() {
        show(resolveLink = { null })

        paste(link)

        composeTestRule.onNodeWithTag("add_shop_status").performScrollTo().assertTextContains("Couldn't read that link", substring = true)
        composeTestRule.onNodeWithTag("add_shop_name").performTextInput("Silver Tentacle")
    }

    @Test
    fun `text shared into the app is read as soon as the page opens`() {
        show(sharedText = shared)

        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().assertTextContains("Silver Tentacle Merchandising", substring = true)
        composeTestRule.onNodeWithTag("add_shop_save").assertIsEnabled()
    }

    @Test
    fun `the user's own position can be used`() {
        show(currentPosition = { Coordinates(39.5, -9.2) })

        composeTestRule.onNodeWithTag("add_shop_use_position").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("39.5000, -9.2000", substring = true)
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("your position", substring = true)
    }

    @Test
    fun `when the position of the phone is not available it says so`() {
        show(currentPosition = { null })

        composeTestRule.onNodeWithTag("add_shop_use_position").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_shop_status").assertTextContains("Couldn't get your position", substring = true)
    }

    @Test
    fun `saving makes a shop of the kind chosen, with the details typed`() {
        show()
        paste(shared)
        composeTestRule.onNodeWithTag("add_shop_kind_second_hand").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("add_shop_phone").performScrollTo().performTextInput("+351 262 000 000")
        composeTestRule.onNodeWithTag("add_shop_website").performTextInput("silvertentacle.com")
        composeTestRule.onNodeWithTag("add_shop_hours").performTextInput("Mo-Sa 10:00-19:00")
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val shop = saved.single()
        assertEquals("custom/test", shop.id)
        assertEquals("Silver Tentacle Merchandising", shop.name)
        assertEquals(ShopCategory.SECOND_HAND_SHOP, shop.category)
        assertEquals(street.latitude, shop.latitude, 0.00001)
        assertEquals(street.longitude, shop.longitude, 0.00001)
        assertEquals(ShopAddress(street = "R. Raul Proença 56a", postcode = "2500", city = "Caldas da Rainha"), shop.address)
        assertEquals("+351 262 000 000", shop.phone)
        assertEquals("silvertentacle.com", shop.website)
        assertEquals("Mo-Sa 10:00-19:00", shop.openingHours)
        assertEquals(false, shop.approximatePosition)
        assertEquals("Silver Tentacle Merchandising, R. Raul Proença 56a, 2500 Caldas da Rainha", shop.mapQuery)
    }

    @Test
    fun `a shop saved with only the town's position is marked approximate`() {
        show(findPosition = { FoundPosition(town, true, "Caldas da Rainha") })
        paste(shared)

        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, saved.single().approximatePosition)
        assertEquals(ShopCategory.RECORD_SHOP, saved.single().category)
    }

    @Test
    fun `details that were left empty are not kept as empty text`() {
        show()
        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().performTextInput("  Silver Tentacle ")
        paste("39.4036, -9.1349")

        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val shop = saved.single()
        assertEquals("Silver Tentacle", shop.name)
        assertNull(shop.address)
        assertNull(shop.phone)
        assertNull(shop.website)
        assertNull(shop.openingHours)
        assertEquals("Silver Tentacle", shop.mapQuery)
    }

    @Test
    fun `hours in a form that is not understood are allowed, with a note`() {
        show()

        composeTestRule.onNodeWithTag("add_shop_hours").performScrollTo().performTextInput("mornings")

        composeTestRule.onNodeWithTag("add_shop_hours_note").assertExists()
    }

    @Test
    fun `hours in the form OpenStreetMap uses have no note`() {
        show()

        composeTestRule.onNodeWithTag("add_shop_hours").performScrollTo().performTextInput("Mo-Sa 10:00-19:00")

        composeTestRule.onNodeWithTag("add_shop_hours_note").assertDoesNotExist()
    }

    // Hours as people write them.

    @Test
    fun `hours written in plain words are understood and shown as they will be kept`() {
        show()

        composeTestRule.onNodeWithTag("add_shop_hours").performScrollTo().performTextInput("Mon-Sat 10-7")

        composeTestRule.onNodeWithTag("add_shop_hours_preview").assertTextContains("Mo-Sa 10:00-19:00", substring = true)
        composeTestRule.onNodeWithTag("add_shop_hours_note").assertDoesNotExist()
    }

    @Test
    fun `hours that are saved are kept in the form the app reads`() {
        show()
        paste("39.4036, -9.1349")
        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().performTextInput("Silver Tentacle")
        composeTestRule.onNodeWithTag("add_shop_hours").performScrollTo().performTextInput("Tue-Sat 10-7; Sun closed")
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Tu-Sa 10:00-19:00; Su off", saved.single().openingHours)
    }

    @Test
    fun `hours that cannot be read are kept as written`() {
        show()
        paste("39.4036, -9.1349")
        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().performTextInput("Silver Tentacle")
        composeTestRule.onNodeWithTag("add_shop_hours").performScrollTo().performTextInput("by appointment")
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("by appointment", saved.single().openingHours)
    }

    // Editing a shop that was added by hand.

    private val added = Shop(
        id = "custom/abc", name = "Silver Tentacle", category = ShopCategory.SECOND_HAND_SHOP,
        latitude = 39.4088, longitude = -9.1377, openingHours = null,
        address = ShopAddress(street = "R. Raul Proença 56a", postcode = "2500", city = "Caldas da Rainha"),
        phone = "+351 262 000 000", website = "silvertentacle.com", approximatePosition = true,
        mapQuery = "Silver Tentacle, R. Raul Proença 56a, 2500 Caldas da Rainha"
    )

    @Test
    fun `editing shows the shop's details in the form`() {
        show(editing = added)

        composeTestRule.onNodeWithTag("add_shop_title").assertTextEquals("Edit shop")
        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().assertTextContains("Silver Tentacle")
        composeTestRule.onNodeWithTag("add_shop_address").assertTextContains("R. Raul Proença 56a, 2500 Caldas da Rainha")
        composeTestRule.onNodeWithTag("add_shop_phone").assertTextContains("+351 262 000 000")
        composeTestRule.onNodeWithTag("add_shop_website").assertTextContains("silvertentacle.com")
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("39.4088, -9.1377", substring = true)
        composeTestRule.onNodeWithTag("add_shop_position").assertTextContains("approximate", substring = true)
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `the page for a new shop is not an edit page`() {
        show()

        composeTestRule.onNodeWithTag("add_shop_title").assertTextEquals("Add a shop")
    }

    @Test
    fun `saving an edit keeps the id and the position and changes what was changed`() {
        show(editing = added)

        composeTestRule.onNodeWithTag("add_shop_hours").performScrollTo().performTextInput("Tue-Sat 11-7")
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val shop = saved.single()
        assertEquals("custom/abc", shop.id)
        assertEquals("Tu-Sa 11:00-19:00", shop.openingHours)
        assertEquals(39.4088, shop.latitude, 0.00001)
        assertEquals(-9.1377, shop.longitude, 0.00001)
        assertEquals(true, shop.approximatePosition)
        assertEquals(ShopCategory.SECOND_HAND_SHOP, shop.category)
        assertEquals("+351 262 000 000", shop.phone)
        assertEquals(ShopAddress(street = "R. Raul Proença 56a", postcode = "2500", city = "Caldas da Rainha"), shop.address)
    }

    @Test
    fun `an edit can change the name and the kind of shop`() {
        show(editing = added)

        composeTestRule.onNodeWithTag("add_shop_kind_record").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("add_shop_name").performScrollTo().performTextReplacement("Silver Tentacle Merchandising")
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Silver Tentacle Merchandising", saved.single().name)
        assertEquals(ShopCategory.RECORD_SHOP, saved.single().category)
        assertEquals("custom/abc", saved.single().id)
    }

    @Test
    fun `an edit can give the shop a better position`() {
        show(editing = added)

        paste("39.4036, -9.1349")
        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(39.4036, saved.single().latitude, 0.00001)
        assertEquals(false, saved.single().approximatePosition)
    }

    @Test
    fun `the button says it saves changes`() {
        show(editing = added)

        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().assertTextEquals("Save changes")
    }

    @Test
    fun `an edit keeps what is not on the form, such as an email`() {
        show(editing = added.copy(email = "hi@silver.pt"))

        composeTestRule.onNodeWithTag("add_shop_save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("hi@silver.pt", saved.single().email)
    }
}
