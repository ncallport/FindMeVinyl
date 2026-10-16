package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.shops.EmailLink
import com.nickallport.findmevinyl.shops.OpeningHours
import com.nickallport.findmevinyl.shops.OpeningHoursDescriber
import com.nickallport.findmevinyl.shops.PhoneLink
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.shops.WebLink
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * A shop's own page: the name and kind of shop, whether it is open now, its address, phone number, website
 * and email, the hours of each day of the week, and a button to open it on a map. Anything OpenStreetMap
 * does not have is left out. The phone number, website and email can be tapped; the text of each is shown as
 * it was entered, but only what passes as a phone number, web address or email address is acted on.
 *
 * [fallbackCountry] is the country of the search, which decides how an address with no country is laid out.
 * [isPublicHoliday] says whether a date is a public holiday where the shop is, or is null when that is not known.
 */
@Composable
fun ShopDetailScreen(
    shop: Shop,
    onOpenMap: () -> Unit,
    onCall: (String) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
    fallbackCountry: String? = null,
    now: LocalDateTime = LocalDateTime.now(),
    isPublicHoliday: ((LocalDate) -> Boolean)? = null
) {
    val hours = shop.openingHours
    val parsed = hours?.let { OpeningHours.parse(it) }
    val status = hours?.let { OpeningHoursDescriber.describe(it, now, isPublicHoliday) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = shop.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("shop_detail_name")
        )
        Text(
            text = categoryLabel(shop.category) + if (shop.secondHandOnly) ", second-hand only" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("shop_detail_kind")
        )
        if (status != null) {
            val open = status.startsWith("Open")
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (open) FontWeight.SemiBold else FontWeight.Normal,
                color = if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp).testTag("shop_detail_status")
            )
        }

        Spacer(Modifier.height(16.dp))

        shop.address?.lines(fallbackCountry)?.takeIf { it.isNotEmpty() }?.let { lines ->
            DetailRow("Address", lines.joinToString("\n"), "shop_detail_address")
        }
        shop.phone?.let { phone ->
            DetailRow("Phone", phone, "shop_detail_phone", PhoneLink.dialUri(phone)?.let { uri -> { onCall(uri) } })
        }
        shop.website?.let { website ->
            DetailRow("Website", website, "shop_detail_website", WebLink.normalise(website)?.let { url -> { onOpenWebsite(url) } })
        }
        shop.email?.let { email ->
            DetailRow("Email", email, "shop_detail_email", EmailLink.mailUri(email)?.let { uri -> { onSendEmail(uri) } })
        }

        Spacer(Modifier.height(16.dp))
        Text(text = "Opening hours", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        when {
            hours == null -> Text("Opening hours not known", style = MaterialTheme.typography.bodyMedium)
            parsed == null -> Text(hours, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("hours_raw"))
            else -> {
                DayOfWeek.values().forEach { day ->
                    HoursRow(
                        label = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        value = parsed.hoursText(day),
                        tag = "hours_day_${day.name}",
                        today = day == now.dayOfWeek
                    )
                }
                parsed.publicHolidayText()?.let { HoursRow("Public holidays", it, "hours_public_holidays", today = false) }
            }
        }

        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onOpenMap, modifier = Modifier.testTag("shop_detail_map")) {
            Text("Open in map")
        }
    }
}

private fun categoryLabel(category: ShopCategory) = when (category) {
    ShopCategory.RECORD_SHOP -> "Record Shop"
    ShopCategory.CHARITY_SHOP -> "Charity Shop"
    ShopCategory.SECOND_HAND_SHOP -> "Second-Hand Shop"
}

@Composable
private fun DetailRow(label: String, value: String, tag: String, onClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        )
    }
}

@Composable
private fun HoursRow(label: String, value: String, tag: String, today: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag(tag)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val weight = if (today) FontWeight.SemiBold else FontWeight.Normal
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = weight)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = weight)
    }
}
