package com.nickallport.findmevinyl.ui.shops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.shops.Shop
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A shop's page, with the public holidays of [region] looked up so its status follows them. With no region
 * nothing is asked for, and hours that name holidays say that holidays may differ.
 */
@Composable
fun ShopDetailRoute(
    shop: Shop,
    region: HolidayRegion?,
    loadPublicHolidays: suspend (HolidayRegion) -> Set<LocalDate>?,
    onOpenMap: () -> Unit,
    onCall: (String) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now()
) {
    var holidays by remember(region) { mutableStateOf<Set<LocalDate>?>(null) }

    LaunchedEffect(region) {
        holidays = region?.let { loadPublicHolidays(it) }
    }

    val known = holidays
    ShopDetailScreen(
        shop = shop,
        onOpenMap = onOpenMap,
        onCall = onCall,
        onOpenWebsite = onOpenWebsite,
        onSendEmail = onSendEmail,
        modifier = modifier,
        fallbackCountry = region?.country,
        now = now,
        isPublicHoliday = known?.let { set -> { date: LocalDate -> date in set } }
    )
}
