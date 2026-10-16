package com.nickallport.findmevinyl.ui.shops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.shops.Shop
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.launch

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
    now: LocalDateTime = LocalDateTime.now(),
    /** Whether the shop is kept as a favourite. */
    loadIsFavourite: suspend () -> Boolean = { false },
    /** Keeps the shop (true) or lets it go (false). Without it the page has no favourite button. */
    onSetFavourite: (suspend (Boolean) -> Unit)? = null,
    /** Changes a shop that was added by hand. */
    onEdit: (() -> Unit)? = null,
    onOpenMapsLink: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var favourite by remember(shop.id) { mutableStateOf(false) }
    var holidays by remember(region) { mutableStateOf<Set<LocalDate>?>(null) }

    LaunchedEffect(shop.id) {
        favourite = loadIsFavourite()
    }

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
        isPublicHoliday = known?.let { set -> { date: LocalDate -> date in set } },
        isFavourite = favourite,
        onEdit = onEdit,
        onOpenMapsLink = onOpenMapsLink,
        onToggleFavourite = onSetFavourite?.let { set ->
            {
                coroutineScope.launch {
                    val next = !favourite
                    set(next)
                    favourite = next
                }
            }
        }
    )
}
