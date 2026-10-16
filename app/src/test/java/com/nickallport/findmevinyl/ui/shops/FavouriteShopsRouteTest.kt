package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.shops.FavouriteShop
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouriteShopsRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val carbono = FavouriteShop(Shop("node/2", "Carbono", ShopCategory.RECORD_SHOP, 38.7, -9.1, null), null)

    @Test
    fun `shows the record while the shops load, and then the shops`() {
        val pending = CompletableDeferred<List<FavouriteShop>>()
        composeTestRule.setContent {
            FindMeVinylTheme { FavouriteShopsRoute(load = { pending.await() }, onShopSelected = {}) }
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertExists()

        pending.complete(listOf(carbono))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
        composeTestRule.onNodeWithText("Carbono").assertExists()
    }

    @Test
    fun `passes on the shop that is tapped`() {
        var selected: FavouriteShop? = null
        composeTestRule.setContent {
            FindMeVinylTheme { FavouriteShopsRoute(load = { listOf(carbono) }, onShopSelected = { selected = it }) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Carbono").performClick()

        assertEquals(carbono, selected)
    }

    @Test
    fun `passes on a tap on the add button`() {
        var added = 0
        composeTestRule.setContent {
            FindMeVinylTheme {
                FavouriteShopsRoute(load = { listOf(carbono) }, onShopSelected = {}, onAddShop = { added++ })
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("favourite_shops_add").performClick()

        assertEquals(1, added)
    }
}
