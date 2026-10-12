package com.nickallport.findmevinyl.discography

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.discography.DiscographyHeader
import com.nickallport.findmevinyl.ui.discography.DiscographyScreen
import kotlinx.coroutines.launch

private sealed class LoadState {
    object Loading : LoadState()
    data class Loaded(val state: DiscographyUiState) : LoadState()
}

@Composable
fun DiscographyRoute(
    artistId: String,
    artistName: String,
    load: suspend (String) -> DiscographyUiState,
    onToggleOwned: suspend (albumId: String, currentlyOwned: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: suspend (albumId: String, currentlyWanted: Boolean) -> Unit = { _, _ -> },
    loadIsFavourite: suspend () -> Boolean = { false },
    onSaveFavourite: suspend (artistId: String, artistName: String) -> Unit = { _, _ -> },
    onRemoveFavourite: suspend (artistId: String) -> Unit = {}
) {
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var isFavourite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun refresh() {
        isFavourite = loadIsFavourite()
        loadState = LoadState.Loaded(load(artistId))
    }

    LaunchedEffect(artistId) {
        refresh()
    }

    Column(modifier = modifier.fillMaxSize()) {
        DiscographyHeader(
            artistName = artistName,
            isFavourite = isFavourite,
            onToggleFavourite = {
                coroutineScope.launch {
                    if (isFavourite) {
                        onRemoveFavourite(artistId)
                    } else {
                        onSaveFavourite(artistId, artistName)
                    }
                    refresh()
                }
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (val currentState = loadState) {
                is LoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                }
                is LoadState.Loaded -> {
                    when (val uiState = currentState.state) {
                        is DiscographyUiState.Success -> {
                            val allAlbums = uiState.groupedAlbums.values.flatten()

                            DiscographyScreen(
                                groupedAlbums = uiState.groupedAlbums,
                                onToggleOwned = { albumId ->
                                    val currentlyOwned = allAlbums.find { it.id == albumId }?.owned ?: false
                                    coroutineScope.launch {
                                        onToggleOwned(albumId, currentlyOwned)
                                        refresh()
                                    }
                                },
                                onToggleWanted = { albumId ->
                                    val currentlyWanted = allAlbums.find { it.id == albumId }?.wanted ?: false
                                    coroutineScope.launch {
                                        onToggleWanted(albumId, currentlyWanted)
                                        refresh()
                                    }
                                }
                            )
                        }
                        is DiscographyUiState.Error -> {
                            Text(text = uiState.message, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}