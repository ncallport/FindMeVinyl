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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import com.nickallport.findmevinyl.ui.discography.DiscographyHeader
import com.nickallport.findmevinyl.ui.discography.FormatCheckNote
import com.nickallport.findmevinyl.ui.discography.FormatCheckState
import com.nickallport.findmevinyl.ui.discography.DiscographyScreen
import com.nickallport.findmevinyl.ui.discography.FormatFilterBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed class LoadState {
    object Loading : LoadState()
    data class Loaded(val state: DiscographyUiState) : LoadState()
}

/**
 * [load] gives the discography a page at a time, and each state it gives replaces what is shown, so
 * the first page appears while the rest are still loading. [loadSaved] reads the saved albums
 * only, with no request to MusicBrainz. Taps on Owned, Want and Save use it, so they show at once.
 *
 * With [checkAllFormats], the formats of all the albums are checked once, by itself, when the whole
 * discography has loaded and some albums have no formats stored or old ones. A line under the chips
 * says how far it has got. Meanwhile the rows wait instead of looking up their own album, and what the
 * check found is brought in from [loadSaved] when it is done. An artist too big to check this way, and a
 * check that failed, leave the rows to look up their own albums as before.
 *
 * A row of chips filters the albums by format. The chosen format is kept across rotation. The
 * filter works on what is known about each album, so every format lookup is recorded here as it
 * comes back, and the chips, the counts and the list follow.
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
    onOpenAlbum: (albumId: String, title: String) -> Unit = { _, _ -> },
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    loadSaved: (suspend (String) -> DiscographyUiState)? = null,
    checkAllFormats: (suspend (artistId: String, onProgress: (donePages: Int, totalPages: Int) -> Unit) -> FormatCheckResult)? = null
) {
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var isFavourite by remember { mutableStateOf(false) }
    // Counts the taps on Try again. Each one starts the load again.
    var attempt by remember { mutableStateOf(0) }
    var selectedFormat by rememberSaveable { mutableStateOf<String?>(null) }
    var formatCheck by remember { mutableStateOf<FormatCheckState>(FormatCheckState.Idle) }
    var formatCheckAttempt by remember { mutableStateOf(0) }
    var formatCheckFinished by remember(artistId) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val loadedSuccess = (loadState as? LoadState.Loaded)?.state as? DiscographyUiState.Success
    val loadedAlbums = loadedSuccess?.groupedAlbums?.values?.flatten().orEmpty()
    // Every page has been asked for, or one failed, so the albums are all there is going to be. The
    // albums saved so far are what the screen shows, so they are what gets checked either way.
    val discographySettled = loadedSuccess != null && !loadedSuccess.loadingMore

    // After a tap, take the flags and formats from the saved albums. They go onto what is on
    // screen, album by album, so albums that arrived while the saved albums were being read stay.
    suspend fun refreshSaved() {
        isFavourite = loadIsFavourite()
        val saved = loadSaved?.invoke(artistId) as? DiscographyUiState.Success ?: return
        val current = (loadState as? LoadState.Loaded)?.state as? DiscographyUiState.Success ?: return
        loadState = LoadState.Loaded(current.withSavedValuesFrom(saved))
    }

    // A lookup that comes back is recorded here, so the filter knows the album's formats now.
    val lookUpFormats: suspend (String) -> List<String>? = { albumId ->
        if (checkAllFormats != null) {
            // The whole artist is about to be checked, or is being checked. Asking for one album now would
            // be a request the check makes anyway.
            snapshotFlow { discographySettled && formatCheck !is FormatCheckState.Running }.first { it }
        }
        val checked = (((loadState as? LoadState.Loaded)?.state as? DiscographyUiState.Success)?.groupedAlbums)
            ?.values?.flatten()?.firstOrNull { it.id == albumId }
            ?.takeIf { it.formats != null && !it.formatsStale }
        if (checked != null) {
            // The check found them.
            checked.formats
        } else {
            loadFormats(albumId)?.also { labels ->
                val current = (loadState as? LoadState.Loaded)?.state as? DiscographyUiState.Success
                if (current != null) loadState = LoadState.Loaded(current.withFormats(albumId, labels))
            }
        }
    }

    LaunchedEffect(artistId, attempt) {
        isFavourite = loadIsFavourite()
        load(artistId).collect { loadState = LoadState.Loaded(it) }
    }

    // Once the whole discography is there, check the formats of every album in one go, once a visit.
    LaunchedEffect(artistId, discographySettled, formatCheckAttempt) {
        val check = checkAllFormats ?: return@LaunchedEffect
        if (!discographySettled || formatCheckFinished) return@LaunchedEffect
        if (loadedAlbums.none { it.formats == null || it.formatsStale }) return@LaunchedEffect

        formatCheck = FormatCheckState.Running(0, 0)
        when (check(artistId) { done, total -> formatCheck = FormatCheckState.Running(done, total) }) {
            is FormatCheckResult.Done -> {
                formatCheckFinished = true
                formatCheck = FormatCheckState.Idle
                refreshSaved()
            }
            FormatCheckResult.TooBig -> {
                formatCheckFinished = true
                formatCheck = FormatCheckState.Idle
            }
            FormatCheckResult.Failed -> formatCheck = FormatCheckState.Failed
        }
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
                            val chosenFormat = selectedFormat
                            val shownAlbums = FormatFilter.apply(uiState.groupedAlbums, chosenFormat)

                            Column(modifier = Modifier.fillMaxSize()) {
                                FormatFilterBar(
                                    options = FormatFilter.options(allAlbums),
                                    selected = chosenFormat,
                                    unchecked = FormatFilter.unchecked(allAlbums),
                                    onSelect = { selectedFormat = it }
                                )
                                FormatCheckNote(state = formatCheck, onRetry = { formatCheckAttempt++ })
                                if (chosenFormat != null && shownAlbums.isEmpty()) {
                                    Text(
                                        text = "No albums found on $chosenFormat",
                                        modifier = Modifier.padding(16.dp).testTag("no_matches")
                                    )
                                }
                                DiscographyScreen(
                                    groupedAlbums = shownAlbums,
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
                                    onOpenAlbum = { albumId ->
                                        allAlbums.find { it.id == albumId }?.let { onOpenAlbum(it.id, it.title) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    loadCover = loadCover,
                                    loadFormats = lookUpFormats,
                                    loadingMore = uiState.loadingMore,
                                    loadError = uiState.loadError,
                                    onRetry = {
                                        // The albums stay on screen while the pages are asked for again.
                                        loadState = LoadState.Loaded(uiState.copy(loadError = null, loadingMore = true))
                                        attempt++
                                    },
                                    checkingFormats = chosenFormat != null
                                )
                            }
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

private fun DiscographyUiState.Success.mapAlbums(change: (AlbumUiModel) -> AlbumUiModel) =
    copy(groupedAlbums = groupedAlbums.mapValues { (_, albums) -> albums.map(change) })

private fun DiscographyUiState.Success.withFormats(albumId: String, labels: List<String>) =
    mapAlbums { if (it.id == albumId) it.copy(formats = labels, formatsStale = false) else it }

// Formats the saved albums have that the screen has not heard about, because a lookup was
// cancelled before it could report back, are taken as well. Known formats are never replaced by none.
private fun DiscographyUiState.Success.withSavedValuesFrom(saved: DiscographyUiState.Success): DiscographyUiState.Success {
    val savedById = saved.groupedAlbums.values.flatten().associateBy { it.id }
    return mapAlbums { album ->
        savedById[album.id]?.let { savedAlbum ->
            album.copy(
                owned = savedAlbum.owned,
                wanted = savedAlbum.wanted,
                formats = savedAlbum.formats ?: album.formats,
                formatsStale = if (savedAlbum.formats != null) savedAlbum.formatsStale else album.formatsStale
            )
        } ?: album
    }
}
