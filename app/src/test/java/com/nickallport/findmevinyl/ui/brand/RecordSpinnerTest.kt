package com.nickallport.findmevinyl.ui.brand

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The loading spinner: the record from the logo, going round. */
@RunWith(RobolectricTestRunner::class)
class RecordSpinnerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `is shown with the tag it is given`() {
        composeTestRule.setContent {
            FindMeVinylTheme { RecordSpinner(modifier = Modifier.testTag("spinner")) }
        }

        composeTestRule.onNodeWithTag("spinner").assertExists()
    }

    @Test
    fun `says it is loading to a screen reader`() {
        composeTestRule.setContent {
            FindMeVinylTheme { RecordSpinner(modifier = Modifier.testTag("spinner")) }
        }

        composeTestRule.onNodeWithTag("spinner").assertContentDescriptionEquals("Loading")
    }

    @Test
    fun `is 56 dp across unless told otherwise`() {
        composeTestRule.setContent {
            FindMeVinylTheme { Column { RecordSpinner(modifier = Modifier.testTag("spinner")) } }
        }

        composeTestRule.onNodeWithTag("spinner").assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun `can be made small to sit beside a line of text`() {
        composeTestRule.setContent {
            FindMeVinylTheme { Column { RecordSpinner(modifier = Modifier.testTag("spinner"), size = 20.dp) } }
        }

        composeTestRule.onNodeWithTag("spinner").assertWidthIsEqualTo(20.dp).assertHeightIsEqualTo(20.dp)
    }
}
