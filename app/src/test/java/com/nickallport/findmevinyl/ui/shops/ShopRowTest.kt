package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShopRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the shop name and category label`() {
        val shop = Shop(
            id = "1", name = "Sister Ray", category = ShopCategory.RECORD_SHOP,
            latitude = 51.5, longitude = -0.1, openingHours = "Mo-Sa 10:00-18:00"
        )

        composeTestRule.setContent { ShopRow(shop) }

        composeTestRule.onNodeWithText("Sister Ray").assertExists()
        composeTestRule.onNodeWithText("Record Shop").assertExists()
    }

    @Test
    fun `shows the opening hours when present`() {
        val shop = Shop(
            id = "1", name = "Sister Ray", category = ShopCategory.RECORD_SHOP,
            latitude = 51.5, longitude = -0.1, openingHours = "Mo-Sa 10:00-18:00"
        )

        composeTestRule.setContent { ShopRow(shop) }

        composeTestRule.onNodeWithText("Mo-Sa 10:00-18:00").assertExists()
    }

    @Test
    fun `shows a fallback message when opening hours are unknown`() {
        val shop = Shop(
            id = "1", name = "Oxfam", category = ShopCategory.CHARITY_SHOP,
            latitude = 51.5, longitude = -0.1, openingHours = null
        )

        composeTestRule.setContent { ShopRow(shop) }

        composeTestRule.onNodeWithText("Hours unknown").assertExists()
    }
}

