package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private sealed class LoadState {
    object Loading : LoadState()
    data class Loaded(val state: DiscographyUiState) : LoadState()
}

/**
 * [load] gives the discography a page at a time, and each state it gives replaces what is shown, so
 * the first page appears while the rest are still loading. [loadSaved] reads the saved albums
 * only, with no request to MusicBrainz. Taps on Owned, Want and Save use it, so they show at once.
 */
@Composable
fun DiscographyRoute(
    artistId: String,
    artistName: String,
    load: (String) -> Flow<DiscographyUiState>,
    onToggleOwned: suspend (albumId: String, currentlyOwned: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: suspend (albumId: String, currentlyWanted: Boolean) -> Unit = { _, _ -> },
    loadIsFavourite: suspend () -> Boolean = { false },
    onSaveFavourite: suspend (artistId: String, artistName: String) -> Unit = { _, _ -> },
    onRemoveFavourite: suspend (artistId: String) -> Unit = {},
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    loadSaved: (suspend (String) -> DiscographyUiState)? = null
) {
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var isFavourite by remember { mutableStateOf(false) }
    // Counts the taps on Try again. Each one starts the load again.
    var attempt by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // After a tap, take the owned and wanted flags from the saved albums. They go onto what is on
    // screen, album by album, so albums that arrived while the saved albums were being read stay.
    suspend fun refreshSaved() {
        isFavourite = loadIsFavourite()
        val saved = loadSaved?.invoke(artistId) as? DiscographyUiState.Success ?: return
        val current = (loadState as? LoadState.Loaded)?.state as? DiscographyUiState.Success ?: return
        loadState = LoadState.Loaded(current.withFlagsFrom(saved))
    }

    LaunchedEffect(artistId, attempt) {
        isFavourite = loadIsFavourite()
        load(artistId).collect { loadState = LoadState.Loaded(it) }
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
                    refreshSaved()
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
                                        refreshSaved()
                                    }
                                },
                                onToggleWanted = { albumId ->
                                    val currentlyWanted = allAlbums.find { it.id == albumId }?.wanted ?: false
                                    coroutineScope.launch {
                                        onToggleWanted(albumId, currentlyWanted)
                                        refreshSaved()
                                    }
                                },
                                loadCover = loadCover,
                                loadFormats = loadFormats,
                                loadingMore = uiState.loadingMore,
                                loadError = uiState.loadError,
                                onRetry = {
                                    // The albums stay on screen while the pages are asked for again.
                                    loadState = LoadState.Loaded(uiState.copy(loadError = null, loadingMore = true))
                                    attempt++
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

private fun DiscographyUiState.Success.withFlagsFrom(saved: DiscographyUiState.Success): DiscographyUiState.Success {
    val savedById = saved.groupedAlbums.values.flatten().associateBy { it.id }
    return copy(
        groupedAlbums = groupedAlbums.mapValues { (_, albums) ->
            albums.map { album ->
                savedById[album.id]?.let { album.copy(owned = it.owned, wanted = it.wanted) } ?: album
            }
        }
    )
}
