package com.nickallport.findmevinyl.ui.favourites

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun FavouritesRoute(
    load: suspend () -> List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var artists by remember { mutableStateOf<List<FavouriteArtistUiModel>?>(null) }

    LaunchedEffect(Unit) {
        artists = load()
    }

    val loaded = artists
    if (loaded == null) {
        CircularProgressIndicator(modifier = modifier.testTag("loading_indicator"))
    } else {
        FavouritesScreen(
            artists = loaded,
            onArtistSelected = onArtistSelected,
            modifier = modifier
        )
    }
}