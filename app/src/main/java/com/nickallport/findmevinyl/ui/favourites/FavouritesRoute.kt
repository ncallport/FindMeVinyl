package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nickallport.findmevinyl.favourites.FavouriteFilter
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
    grid: Boolean = false,
    /** Loads Various Artists and Soundtracks, shown under the artists. Without it there are none. */
    loadSpecials: (suspend () -> List<FavouriteArtistUiModel>)? = null,
    /** Only the artists whose name begins with this are shown (see [FavouriteFilter]). Blank shows them all. */
    filter: String = "",
    /** Told how many artists are shown and how many there are, once they are loaded and whenever either changes. */
    onCounts: (shown: Int, total: Int) -> Unit = { _, _ -> }
) {
    var artists by remember { mutableStateOf<List<FavouriteArtistUiModel>?>(null) }
    var specials by remember { mutableStateOf<List<FavouriteArtistUiModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        specials = loadSpecials?.invoke().orEmpty()
        artists = load()
    }

    val loaded = artists
    if (loaded == null) {
        RecordSpinner(modifier = modifier.fillMaxWidth().padding(16.dp).testTag("loading_indicator"))
    } else {
        val shown = FavouriteFilter.apply(loaded, filter)
        val shownSpecials = FavouriteFilter.apply(specials, filter)
        LaunchedEffect(shown.size, loaded.size) { onCounts(shown.size, loaded.size) }
        if (filter.isNotBlank() && shown.isEmpty() && shownSpecials.isEmpty()) {
            Text(
                text = "No favourites begin with \"${filter.trim()}\"",
                modifier = modifier.padding(16.dp).testTag("favourites_no_match")
            )
        } else {
            FavouritesScreen(
                artists = shown,
                onArtistSelected = onArtistSelected,
                modifier = modifier,
                loadArtistImage = loadArtistImage,
                grid = grid,
                specials = shownSpecials,
                scrollToTopOn = filter
            )
        }
    }
}
