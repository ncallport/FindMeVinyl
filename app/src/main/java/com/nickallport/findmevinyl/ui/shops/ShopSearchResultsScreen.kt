package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.shops.PlaceCandidate
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import java.time.LocalDate

@Composable
fun ShopSearchResultsScreen(
    state: ShopSearchUiState,
    modifier: Modifier = Modifier,
    onShopSelected: ((Shop) -> Unit)? = null,
    onPlaceSelected: ((PlaceCandidate) -> Unit)? = null,
    isPublicHoliday: ((LocalDate) -> Boolean)? = null,
    hiddenShops: Int = 0,
    /** What the shops left out are called, in the singular. */
    hiddenNoun: String = "charity shop",
    onOpenSettings: (() -> Unit)? = null
) {
    var othersExpanded by remember(state) { mutableStateOf(false) }

    when (state) {
        is ShopSearchUiState.Success -> {
            Column(modifier = modifier) {
                Text(
                    text = state.placeName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Shops the settings left out, so a short list is not a puzzle.
                if (hiddenShops > 0) {
                    val noun = if (hiddenShops == 1) hiddenNoun else "${hiddenNoun}s"
                    Text(
                        text = "$hiddenShops $noun hidden by your settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .testTag("hidden_shops_note")
                            .then(if (onOpenSettings != null) Modifier.clickable { onOpenSettings() } else Modifier)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (state.otherPlaces.isNotEmpty() && onPlaceSelected != null) {
                    TextButton(
                        onClick = { othersExpanded = !othersExpanded },
                        modifier = Modifier
                            .testTag("other_places_toggle")
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(if (othersExpanded) "Hide other places" else "Not the right place?")
                    }
                    if (othersExpanded) {
                        state.otherPlaces.forEach { place ->
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlaceSelected(place) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (state.shops.isEmpty()) {
                    Text(
                        text = if (hiddenShops > 0) "No other shops found nearby" else "No shops found nearby",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    ShopListScreen(
                        shops = state.shops,
                        fromLat = state.centerLatitude,
                        fromLon = state.centerLongitude,
                        modifier = Modifier.weight(1f),
                        onShopSelected = onShopSelected,
                        isPublicHoliday = isPublicHoliday
                    )
                }
            }
        }
        is ShopSearchUiState.TownNotFound -> {
            Text(
                text = "Couldn't find ${state.town}",
                modifier = modifier.padding(16.dp)
            )
        }
        is ShopSearchUiState.Error -> {
            Text(
                text = state.message,
                modifier = modifier.padding(16.dp)
            )
        }
    }
}