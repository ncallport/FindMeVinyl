package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.ui.discography.AlbumDetailScreen
import com.nickallport.findmevinyl.ui.discography.AlbumDetailState
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.launch

/**
 * [loadDetails] gives the details of the album's pressing, or null when they could not be had. Try
 * again asks for them again. [loadAlbum] reads the saved owned and want flags, and is asked again
 * after each tap, so the buttons show what is stored. A tap does not reload the details.
 */
@Composable
fun AlbumDetailRoute(
    albumId: String,
    title: String,
    artistName: String,
    loadDetails: suspend (String) -> AlbumDetails?,
    loadAlbum: suspend (String) -> AlbumUiModel?,
    onToggleOwned: suspend (albumId: String, currentlyOwned: Boolean) -> Unit,
    onToggleWanted: suspend (albumId: String, currentlyWanted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    loadCover: suspend (String) -> Bitmap? = { null }
) {
    var state by remember(albumId) { mutableStateOf<AlbumDetailState>(AlbumDetailState.Loading) }
    var album by remember(albumId) { mutableStateOf<AlbumUiModel?>(null) }
    // Counts the taps on Try again. Each one starts the load again.
    var attempt by remember(albumId) { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(albumId, attempt) {
        state = AlbumDetailState.Loading
        state = loadDetails(albumId)?.let { AlbumDetailState.Loaded(it) } ?: AlbumDetailState.Failed
    }

    LaunchedEffect(albumId) {
        album = loadAlbum(albumId)
    }

    AlbumDetailScreen(
        albumId = albumId,
        title = title,
        artistName = artistName,
        album = album,
        state = state,
        onRetry = { attempt++ },
        onToggleOwned = {
            val current = album ?: return@AlbumDetailScreen
            coroutineScope.launch {
                onToggleOwned(albumId, current.owned)
                album = loadAlbum(albumId)
            }
        },
        onToggleWanted = {
            val current = album ?: return@AlbumDetailScreen
            coroutineScope.launch {
                onToggleWanted(albumId, current.wanted)
                album = loadAlbum(albumId)
            }
        },
        loadCover = loadCover,
        modifier = modifier
    )
}
