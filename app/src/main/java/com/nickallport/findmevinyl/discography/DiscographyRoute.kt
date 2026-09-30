package com.nickallport.findmevinyl.discography

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.discography.DiscographyScreen

private sealed class LoadState {
    object Loading : LoadState()
    data class Loaded(val state: DiscographyUiState) : LoadState()
}

@Composable
fun DiscographyRoute(
    artistId: String,
    load: suspend (String) -> DiscographyUiState,
    onToggleOwned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }

    LaunchedEffect(artistId) {
        loadState = LoadState.Loaded(load(artistId))
    }

    when (val currentState = loadState) {
        is LoadState.Loading -> {
            CircularProgressIndicator(modifier = modifier.testTag("loading_indicator"))
        }
        is LoadState.Loaded -> {
            when (val uiState = currentState.state) {
                is DiscographyUiState.Success -> {
                    DiscographyScreen(
                        groupedAlbums = uiState.groupedAlbums,
                        onToggleOwned = onToggleOwned,
                        modifier = modifier
                    )
                }
                is DiscographyUiState.Error -> {
                    Text(text = uiState.message, modifier = modifier.padding(16.dp))
                }
            }
        }
    }
}