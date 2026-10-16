package com.nickallport.findmevinyl.ui.favourites

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
    onMoveDown: suspend (String) -> Unit = {}
) {
    var artists by remember { mutableStateOf<List<FavouriteArtistUiModel>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        artists = load()
    }

    val loaded = artists
    if (loaded == null) {
        RecordSpinner(modifier = modifier.testTag("loading_indicator"))
    } else {
        FavouritesScreen(
            artists = loaded,
            onArtistSelected = onArtistSelected,
            modifier = modifier,
            loadArtistImage = loadArtistImage,
            // After a move the list is read again, so it shows what is stored.
            onMoveUp = { id -> coroutineScope.launch { onMoveUp(id); artists = load() } },
            onMoveDown = { id -> coroutineScope.launch { onMoveDown(id); artists = load() } }
        )
    }
}