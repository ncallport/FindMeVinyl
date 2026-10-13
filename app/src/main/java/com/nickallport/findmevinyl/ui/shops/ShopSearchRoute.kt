package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.shops.PlaceCandidate
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import kotlinx.coroutines.launch

private sealed class RouteState {
    object Idle : RouteState()
    object Loading : RouteState()
    data class Result(val state: ShopSearchUiState) : RouteState()
}

@Composable
fun ShopSearchRoute(
    search: suspend (String) -> ShopSearchUiState,
    modifier: Modifier = Modifier,
    searchNearby: (suspend () -> ShopSearchUiState)? = null,
    onShopSelected: ((Shop) -> Unit)? = null,
    searchPlace: (suspend (PlaceCandidate, List<PlaceCandidate>) -> ShopSearchUiState)? = null
) {
    var town by remember { mutableStateOf("") }
    var routeState by remember { mutableStateOf<RouteState>(RouteState.Idle) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = town,
                onValueChange = { town = it },
                modifier = Modifier.testTag("town_field")
            )
            Button(
                onClick = {
                    val trimmed = town.trim()
                    if (trimmed.isNotEmpty()) {
                        routeState = RouteState.Loading
                        coroutineScope.launch {
                            routeState = RouteState.Result(search(trimmed))
                        }
                    }
                },
                modifier = Modifier.testTag("town_search_button")
            ) {
                Text("Search")
            }
            if (searchNearby != null) {
                TextButton(
                    onClick = {
                        routeState = RouteState.Loading
                        coroutineScope.launch {
                            routeState = RouteState.Result(searchNearby())
                        }
                    },
                    modifier = Modifier.testTag("use_location_button")
                ) {
                    Text("Use my location")
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (val current = routeState) {
                is RouteState.Idle -> {
                    Text(
                        text = "Enter a town to find record shops",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is RouteState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                }
                is RouteState.Result -> {
                    val shown = current.state
                    val onPlaceSelected: ((PlaceCandidate) -> Unit)? =
                        if (searchPlace != null && shown is ShopSearchUiState.Success) {
                            { picked ->
                                val previous = PlaceCandidate(
                                    shown.placeName,
                                    shown.centerLatitude,
                                    shown.centerLongitude
                                )
                                val others = listOf(previous) + shown.otherPlaces.filter { it != picked }
                                routeState = RouteState.Loading
                                coroutineScope.launch {
                                    routeState = RouteState.Result(searchPlace(picked, others))
                                }
                            }
                        } else {
                            null
                        }

                    ShopSearchResultsScreen(
                        state = shown,
                        onShopSelected = onShopSelected,
                        onPlaceSelected = onPlaceSelected
                    )
                }
            }
        }
    }
}