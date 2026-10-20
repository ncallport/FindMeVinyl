package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.brand.RecordSpinner

/** Loads the favourites with [load], shows a spinner meanwhile, then the list or the [grid]. */
@Composable
fun FavouritesRoute(
    load: suspend () -> List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    grid: Boolean = false
) {
    var artists by remember { mutableStateOf<List<FavouriteArtistUiModel>?>(null) }

    LaunchedEffect(Unit) {
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
            grid = grid
        )
    }
}
