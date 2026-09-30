package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShopListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows every shop in the list`() {
        val shops = listOf(
            Shop("1", "Sister Ray", ShopCategory.RECORD_SHOP, 51.51, -0.13, null),
            Shop("2", "Oxfam", ShopCategory.CHARITY_SHOP, 51.52, -0.14, null),
            Shop("3", "Reckless Records", ShopCategory.RECORD_SHOP, 51.50, -0.12, null)
        )

        composeTestRule.setContent {
            ShopListScreen(shops = shops, fromLat = 51.5074, fromLon = -0.1278)
        }

        composeTestRule.onAllNodesWithTag("shop_row").assertCountEquals(3)
    }
}