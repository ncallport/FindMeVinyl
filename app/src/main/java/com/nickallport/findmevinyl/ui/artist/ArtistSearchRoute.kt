package com.nickallport.findmevinyl.ui.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun ArtistSearchRoute(
    search: suspend (String) -> ArtistSearchUiState,
    onArtistSelected: (ArtistDisplayItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenFavourites: () -> Unit = {},
    onOpenWantList: () -> Unit = {},
    onOpenShops: () -> Unit = {},
    initialQuery: String = "",
    initialState: ArtistSearchUiState = ArtistSearchUiState.Idle,
    onQueryChange: (String) -> Unit = {},
    onStateChange: (ArtistSearchUiState) -> Unit = {}
) {
    var uiState by remember { mutableStateOf(initialState) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        ArtistSearchScreen(
            onSearch = { query ->
                uiState = ArtistSearchUiState.Loading
                coroutineScope.launch {
                    val result = search(query)
                    ensureActive()
                    uiState = result
                    onStateChange(result)
                }
            },
            onOpenFavourites = onOpenFavourites,
            onOpenWantList = onOpenWantList,
            onOpenShops = onOpenShops,
            initialQuery = initialQuery,
            onQueryChange = onQueryChange
        )
        ArtistSearchResultsScreen(state = uiState, onArtistSelected = onArtistSelected)
    }
}