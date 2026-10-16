package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.SearchResultCache
import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.shops.PlaceCandidate
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import com.nickallport.findmevinyl.ui.brand.Glyphs
import com.nickallport.findmevinyl.ui.common.SearchField
import java.time.LocalDate
import kotlinx.coroutines.launch

private sealed class RouteState {
    object Idle : RouteState()
    object Loading : RouteState()
    data class Result(val state: ShopSearchUiState) : RouteState()
}

/**
 * The Shops page: a search field for a town (the keyboard's search key searches), a Near me button,
 * and the shops found.
 *
 * It starts from the last result kept in [cache], so coming back or rotating costs no request. The
 * first time it opens with nothing kept, it searches near you by itself, once. That is one request
 * to a shared public server, and [SearchResultCache.nearbyTried] stops it repeating. After that,
 * searching again is up to the user.
 *
 * With [loadPublicHolidays], the public holidays of the region the shown shops are in are looked up,
 * so opening hours can follow their public holiday rules.
 *
 * With [hideCharityShops] and [hideSecondHandShops], those shops are left out of what is shown, and a note says
 * how many. The kept result still has them, so changing the setting needs no new search. Until they are known, and when they cannot be
 * found or the region is not known, hours that mention holidays say that holidays may differ.
 */
@Composable
fun ShopSearchRoute(
    search: suspend (String) -> ShopSearchUiState,
    modifier: Modifier = Modifier,
    searchNearby: (suspend () -> ShopSearchUiState)? = null,
    onShopSelected: ((Shop) -> Unit)? = null,
    searchPlace: (suspend (PlaceCandidate, List<PlaceCandidate>) -> ShopSearchUiState)? = null,
    cache: SearchResultCache = remember { SearchResultCache() },
    loadPublicHolidays: (suspend (HolidayRegion) -> Set<LocalDate>?)? = null,
    hideCharityShops: Boolean = false,
    hideSecondHandShops: Boolean = false,
    onOpenSettings: (() -> Unit)? = null
) {
    var town by rememberSaveable { mutableStateOf("") }
    var routeState by remember {
        mutableStateOf<RouteState>(cache.shop?.let { RouteState.Result(it) } ?: RouteState.Idle)
    }
    val coroutineScope = rememberCoroutineScope()

    val shownRegion = ((routeState as? RouteState.Result)?.state as? ShopSearchUiState.Success)?.region
    var holidayDates by remember(shownRegion) { mutableStateOf<Set<LocalDate>?>(null) }
    LaunchedEffect(shownRegion) {
        holidayDates = if (shownRegion != null && loadPublicHolidays != null) loadPublicHolidays(shownRegion) else null
    }
    val isPublicHoliday: ((LocalDate) -> Boolean)? = holidayDates?.let { dates -> { date -> date in dates } }

    fun showResult(state: ShopSearchUiState) {
        cache.shop = state
        routeState = RouteState.Result(state)
    }

    LaunchedEffect(Unit) {
        if (routeState is RouteState.Idle && searchNearby != null && !cache.nearbyTried) {
            routeState = RouteState.Loading
            val result = searchNearby()
            // Only once it has finished, so a rotation in the middle tries again.
            cache.nearbyTried = true
            showResult(result)
        }
    }

    Column(modifier = modifier.padding(top = 8.dp)) {
        SearchField(
            value = town,
            onValueChange = { town = it },
            onSearch = { trimmed ->
                routeState = RouteState.Loading
                coroutineScope.launch { showResult(search(trimmed)) }
            },
            hint = "Search for a town",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (searchNearby != null) {
            FilledTonalButton(
                onClick = {
                    routeState = RouteState.Loading
                    coroutineScope.launch { showResult(searchNearby()) }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("use_location_button")
            ) {
                Row {
                    Icon(Glyphs.Pin, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Near me")
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (val current = routeState) {
                is RouteState.Idle -> {
                    Text(
                        text = "Search for a town to find record shops",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is RouteState.Loading -> {
                    RecordSpinner(modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("loading_indicator"))
                }
                is RouteState.Result -> {
                    val shown = current.state
                    val onPlaceSelected: ((PlaceCandidate) -> Unit)? =
                        if (searchPlace != null && shown is ShopSearchUiState.Success) {
                            { picked ->
                                val previous = PlaceCandidate(
                                    shown.placeName,
                                    shown.centerLatitude,
                                    shown.centerLongitude,
                                    shown.region
                                )
                                val others = listOf(previous) + shown.otherPlaces.filter { it != picked }
                                routeState = RouteState.Loading
                                coroutineScope.launch {
                                    showResult(searchPlace(picked, others))
                                }
                            }
                        } else {
                            null
                        }

                    val hiddenCategories = buildSet {
                        if (hideCharityShops) add(ShopCategory.CHARITY_SHOP)
                        if (hideSecondHandShops) add(ShopCategory.SECOND_HAND_SHOP)
                    }
                    val hiddenList = if (shown is ShopSearchUiState.Success) {
                        shown.shops.filter { it.category in hiddenCategories }
                    } else {
                        emptyList()
                    }
                    val hiddenShops = hiddenList.size
                    // What the note calls them: the one kind that was left out, or just shops when it was both.
                    val hiddenNoun = when (hiddenList.map { it.category }.toSet()) {
                        setOf(ShopCategory.SECOND_HAND_SHOP) -> "second-hand shop"
                        setOf(ShopCategory.CHARITY_SHOP) -> "charity shop"
                        else -> "shop"
                    }
                    val displayed = if (hiddenShops > 0 && shown is ShopSearchUiState.Success) {
                        shown.copy(shops = shown.shops.filter { it.category !in hiddenCategories })
                    } else {
                        shown
                    }

                    ShopSearchResultsScreen(
                        state = displayed,
                        hiddenShops = hiddenShops,
                        hiddenNoun = hiddenNoun,
                        onOpenSettings = onOpenSettings,
                        onShopSelected = onShopSelected,
                        onPlaceSelected = onPlaceSelected,
                        isPublicHoliday = isPublicHoliday
                    )
                }
            }
        }
    }
}
