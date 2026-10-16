package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.shops.FavouriteShop
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopAddress
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w360dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class FavouriteShopsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun favourite(id: String, name: String, city: String?, category: ShopCategory = ShopCategory.RECORD_SHOP) =
        FavouriteShop(
            Shop(id, name, category, 38.7, -9.1, null, address = city?.let { ShopAddress(city = it) }),
            HolidayRegion("PT", null)
        )

    private val shops = listOf(
        favourite("node/1", "Silver Tentacle", "Caldas da Rainha"),
        favourite("node/2", "Carbono", "Lisboa"),
        favourite("node/3", "Louie Louie", "Lisboa"),
        favourite("node/4", "Nowhere Records", null, ShopCategory.SECOND_HAND_SHOP)
    )

    private fun show(list: List<FavouriteShop> = shops, onSelected: (FavouriteShop) -> Unit = {}) {
        composeTestRule.setContent {
            FindMeVinylTheme { FavouriteShopsScreen(shops = list, onShopSelected = onSelected) }
        }
    }

    @Test
    fun `shows each shop, with its kind`() {
        show()

        composeTestRule.onNodeWithText("Carbono").assertExists()
        composeTestRule.onNodeWithText("Second-Hand Shop").assertExists()
    }

    @Test
    fun `groups the shops under their town`() {
        show()

        composeTestRule.onNodeWithTag("favourite_shops_town_Caldas da Rainha").assertExists()
        composeTestRule.onNodeWithTag("favourite_shops_town_Lisboa").assertExists()
    }

    @Test
    fun `a town heading comes above its shops, and each town is shown once`() {
        show()

        val heading = composeTestRule.onNodeWithTag("favourite_shops_town_Lisboa").fetchSemanticsNode().positionInRoot.y
        val carbono = composeTestRule.onNodeWithText("Carbono").fetchSemanticsNode().positionInRoot.y
        val louie = composeTestRule.onNodeWithText("Louie Louie").fetchSemanticsNode().positionInRoot.y

        assertTrue(heading < carbono && carbono < louie)
    }

    @Test
    fun `shops with no town are listed under other places`() {
        show()

        composeTestRule.onNodeWithTag("favourite_shops_town_Other places").assertExists()
    }

    @Test
    fun `tapping a shop selects it, with its region`() {
        var selected: FavouriteShop? = null
        show(onSelected = { selected = it })

        composeTestRule.onNodeWithText("Carbono").performClick()

        assertEquals("node/2", selected?.shop?.id)
        assertEquals(HolidayRegion("PT", null), selected?.region)
    }

    @Test
    fun `with no favourites it says how to add one`() {
        show(emptyList())

        composeTestRule.onNodeWithTag("favourite_shops_empty").assertExists()
        composeTestRule.onNodeWithText("Open a shop and tap Add to favourites.").assertExists()
    }

    @Test
    fun `has a button to add a shop, which opens the form`() {
        var opened = 0
        composeTestRule.setContent {
            FindMeVinylTheme { FavouriteShopsScreen(shops = shops, onShopSelected = {}, onAddShop = { opened++ }) }
        }

        composeTestRule.onNodeWithTag("favourite_shops_add").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `the button is there when there are no favourites too`() {
        composeTestRule.setContent {
            FindMeVinylTheme { FavouriteShopsScreen(shops = emptyList(), onShopSelected = {}, onAddShop = {}) }
        }

        composeTestRule.onNodeWithTag("favourite_shops_add").assertExists()
    }

    @Test
    fun `without a way to add one there is no button`() {
        show()

        composeTestRule.onNodeWithTag("favourite_shops_add").assertDoesNotExist()
    }
}
