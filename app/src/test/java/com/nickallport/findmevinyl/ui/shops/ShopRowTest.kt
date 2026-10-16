package com.nickallport.findmevinyl.ui.shops

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class ShopRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val monday = LocalDate.of(2026, 10, 12)

    private fun at(time: String): LocalDateTime = monday.atTime(LocalTime.parse(time))

    private fun shopWithHours(hours: String?) = Shop(
        id = "1", name = "Sister Ray", category = ShopCategory.RECORD_SHOP,
        latitude = 51.5, longitude = -0.1, openingHours = hours
    )

    @Test
    fun `shows the shop name and category label`() {
        composeTestRule.setContent { ShopRow(shopWithHours("Mo-Sa 10:00-18:00")) }

        composeTestRule.onNodeWithText("Sister Ray").assertExists()
        composeTestRule.onNodeWithText("Record Shop").assertExists()
    }

    @Test
    fun `shows a fallback message and no toggle when opening hours are unknown`() {
        val shop = Shop(
            id = "1", name = "Oxfam", category = ShopCategory.CHARITY_SHOP,
            latitude = 51.5, longitude = -0.1, openingHours = null
        )

        composeTestRule.setContent { ShopRow(shop) }

        composeTestRule.onNodeWithText("Hours unknown").assertExists()
        composeTestRule.onNodeWithTag("hours_toggle").assertDoesNotExist()
    }

    @Test
    fun `shows the status line but hides the full hours by default`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours("Mo-Sa 10:00-18:00"), now = at("12:00"))
        }

        composeTestRule.onNodeWithText("Open now, closes 18:00").assertExists()
        composeTestRule.onNodeWithText("Show hours").assertExists()
        composeTestRule.onNodeWithText("Mo-Sa 10:00-18:00").assertDoesNotExist()
    }

    @Test
    fun `shows when a closed shop next opens`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours("Mo-Sa 10:00-18:00"), now = at("19:00"))
        }

        composeTestRule.onNodeWithText("Closed, opens Tue 10:00").assertExists()
    }

    @Test
    fun `tapping Show hours reveals the full hours`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours("Mo-Sa 10:00-18:00"), now = at("12:00"))
        }

        composeTestRule.onNodeWithTag("hours_toggle").performClick()

        composeTestRule.onNodeWithText("Mo-Sa 10:00-18:00").assertExists()
        composeTestRule.onNodeWithText("Hide hours").assertExists()
    }

    @Test
    fun `tapping Hide hours hides them again`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours("Mo-Sa 10:00-18:00"), now = at("12:00"))
        }

        composeTestRule.onNodeWithTag("hours_toggle").performClick()
        composeTestRule.onNodeWithTag("hours_toggle").performClick()

        composeTestRule.onNodeWithText("Mo-Sa 10:00-18:00").assertDoesNotExist()
        composeTestRule.onNodeWithText("Show hours").assertExists()
    }

    @Test
    fun `shows the raw hours directly with no toggle when they cannot be read`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours("by appointment"), now = at("12:00"))
        }

        composeTestRule.onNodeWithText("by appointment").assertExists()
        composeTestRule.onNodeWithTag("hours_toggle").assertDoesNotExist()
        composeTestRule.onNodeWithText("Open now", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Closed", substring = true).assertDoesNotExist()
    }

    @Test
    fun `tapping the row invokes the click callback`() {
        var clicked = false

        composeTestRule.setContent {
            ShopRow(shopWithHours(null), onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("Sister Ray").performClick()

        assertTrue(clicked)
    }

    @Test
    fun `the row is only clickable when a callback is given`() {
        composeTestRule.setContent { ShopRow(shopWithHours(null)) }
        composeTestRule.onNodeWithText("Sister Ray").assertHasNoClickAction()
    }

    @Test
    fun `the row is clickable when a callback is given`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours(null), onClick = {})
        }
        composeTestRule.onNodeWithText("Sister Ray").assertHasClickAction()
    }

    @Test
    fun `tapping Show hours does not invoke the row click callback`() {
        var clicked = false

        composeTestRule.setContent {
            ShopRow(
                shopWithHours("Mo-Sa 10:00-18:00"),
                now = at("12:00"),
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithTag("hours_toggle").performClick()

        assertFalse(clicked)
        composeTestRule.onNodeWithText("Mo-Sa 10:00-18:00").assertExists()
    }

    // Public holidays.

    @Test
    fun `says the shop is closed on a public holiday when its hours say so`() {
        composeTestRule.setContent {
            ShopRow(
                shopWithHours("Mo-Fr 09:00-17:00; PH off"),
                now = at("12:00"),
                isPublicHoliday = { it == monday }
            )
        }

        composeTestRule.onNodeWithText("Closed, opens Tue 09:00 (public holiday today)").assertExists()
    }

    @Test
    fun `leaves out the note about holidays when the holidays are known and it is not one`() {
        composeTestRule.setContent {
            ShopRow(
                shopWithHours("Mo-Fr 09:00-17:00; PH off"),
                now = at("12:00"),
                isPublicHoliday = { false }
            )
        }

        composeTestRule.onNodeWithText("Open now, closes 17:00").assertExists()
    }

    @Test
    fun `keeps the note that holidays may differ when the holidays are not known`() {
        composeTestRule.setContent {
            ShopRow(shopWithHours("Mo-Fr 09:00-17:00; PH off"), now = at("12:00"))
        }

        composeTestRule.onNodeWithText("Open now, closes 17:00 (public holidays may differ)").assertExists()
    }

    @Test
    fun `a favourite shop has a heart on its row`() {
        composeTestRule.setContent { ShopRow(shopWithHours(null), favourite = true) }

        composeTestRule.onNodeWithTag("shop_favourite_heart", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a shop that is not a favourite has no heart`() {
        composeTestRule.setContent { ShopRow(shopWithHours(null)) }

        composeTestRule.onNodeWithTag("shop_favourite_heart", useUnmergedTree = true).assertDoesNotExist()
    }
}
