package com.nickallport.findmevinyl.ui.common

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows its content`() {
        composeTestRule.setContent { FindMeVinylTheme { ListCard { Text("Foxtrot") } } }

        composeTestRule.onNodeWithText("Foxtrot").assertExists()
    }

    @Test
    fun `is tappable when given an action`() {
        var taps = 0
        composeTestRule.setContent {
            FindMeVinylTheme { ListCard(onClick = { taps++ }, modifier = Modifier.testTag("card")) { Text("x") } }
        }

        composeTestRule.onNodeWithTag("card").assertHasClickAction().performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `is not tappable without an action`() {
        composeTestRule.setContent {
            FindMeVinylTheme { ListCard(modifier = Modifier.testTag("card")) { Text("x") } }
        }

        composeTestRule.onNodeWithTag("card").assertExists()
    }

    @Test
    fun `draws in dark mode too`() {
        composeTestRule.setContent { FindMeVinylTheme(darkTheme = true) { ListCard { Text("Dark") } } }

        composeTestRule.onNodeWithText("Dark").assertExists()
    }
}

