package com.nickallport.findmevinyl.ui.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import kotlinx.coroutines.launch

@Composable
fun ArtistSearchRoute(
    search: suspend (String) -> ArtistSearchUiState,
    onArtistSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf<ArtistSearchUiState>(ArtistSearchUiState.Idle) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        ArtistSearchScreen(
            onSearch = { query ->
                uiState = ArtistSearchUiState.Loading
                coroutineScope.launch {
                    uiState = search(query)
                }
            }
        )
        ArtistSearchResultsScreen(state = uiState, onArtistSelected = onArtistSelected)
    }
}