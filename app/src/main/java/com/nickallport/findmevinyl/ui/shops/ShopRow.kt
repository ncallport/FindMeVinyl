package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.common.ListCard
import com.nickallport.findmevinyl.shops.OpeningHoursDescriber
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun ShopRow(
    shop: Shop,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(),
    isPublicHoliday: ((LocalDate) -> Boolean)? = null,
    onClick: (() -> Unit)? = null
) {
    var hoursExpanded by rememberSaveable { mutableStateOf(false) }

    val categoryLabel = when (shop.category) {
        ShopCategory.RECORD_SHOP -> "Record Shop"
        ShopCategory.CHARITY_SHOP -> "Charity Shop"
        ShopCategory.SECOND_HAND_SHOP -> "Second-Hand Shop"
    }
    val hours = shop.openingHours
    val status = hours?.let { OpeningHoursDescriber.describe(it, now, isPublicHoliday) }

    ListCard(modifier = modifier, onClick = onClick) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = shop.name, style = MaterialTheme.typography.titleMedium)
        Text(text = categoryLabel, style = MaterialTheme.typography.bodySmall)

        when {
            hours == null -> {
                Text(text = "Hours unknown", style = MaterialTheme.typography.bodySmall)
            }
            status == null -> {
                Text(text = hours, style = MaterialTheme.typography.bodySmall)
            }
            else -> {
                // Open now stands out, so a list of shops can be scanned at a glance.
                val open = status.startsWith("Open")
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (open) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { hoursExpanded = !hoursExpanded },
                    modifier = Modifier.testTag("hours_toggle")
                ) {
                    Text(if (hoursExpanded) "Hide hours" else "Show hours")
                }
                if (hoursExpanded) {
                    Text(text = hours, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    }
}
