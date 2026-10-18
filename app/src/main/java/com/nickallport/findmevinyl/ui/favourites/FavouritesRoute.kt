package com.nickallport.findmevinyl.ui.favourites

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import android.graphics.Bitmap
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

@Composable
fun FavouritesRoute(
    load: suspend () -> List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    onMoveUp: suspend (String) -> Unit = {},
    onMoveDown: suspend (String) -> Unit = {},
    grid: Boolean = false,
    onReorder: suspend (List<String>) -> Unit = {},
    groupByGenre: Boolean = false,
    /** Changes when the favourites should be read again, such as after a genre was found for one. */
    reloadKey: Int = 0
) {
    var artists by remember { mutableStateOf<List<FavouriteArtistUiModel>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        artists = load()
    }

    val loaded = artists
    if (loaded == null) {
        RecordSpinner(modifier = modifier.fillMaxWidth().padding(16.dp).testTag("loading_indicator"))
    } else {
        FavouritesScreen(
            artists = loaded,
            onArtistSelected = onArtistSelected,
            modifier = modifier,
            loadArtistImage = loadArtistImage,
            grid = grid,
            groupByGenre = groupByGenre,
            // After a move the list is read again, so it shows what is stored.
            onMoveUp = { id -> coroutineScope.launch { onMoveUp(id); artists = load() } },
            onMoveDown = { id -> coroutineScope.launch { onMoveDown(id); artists = load() } },
            onReorder = { ids -> coroutineScope.launch { onReorder(ids); artists = load() } }
        )
    }
}