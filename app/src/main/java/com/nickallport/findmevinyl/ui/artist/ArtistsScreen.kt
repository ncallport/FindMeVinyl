package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.common.SearchField
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import com.nickallport.findmevinyl.ui.favourites.FavouritesRoute

/**
 * The Artists page: a search field, and under it the favourite artists. Searching with the
 * keyboard's search key opens the results page.
 */
@Composable
fun ArtistsScreen(
    onSearch: (String) -> Unit,
    loadFavourites: suspend () -> List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    onMoveFavouriteUp: suspend (String) -> Unit = {},
    onMoveFavouriteDown: suspend (String) -> Unit = {},
    onReorderFavourites: suspend (List<String>) -> Unit = {},
    /** Show the favourites as a grid of pictures when the page opens. */
    startInGrid: Boolean = false,
    /** Told when the user switches between the list and the grid, so the choice can be kept. */
    onGridChange: ((Boolean) -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    var grid by rememberSaveable { mutableStateOf(startInGrid) }

    Column(modifier = modifier.padding(top = 8.dp)) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = onSearch,
            hint = "Search for an artist",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your favourites",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !grid,
                onClick = { if (grid) { grid = false; onGridChange?.invoke(false) } },
                label = { Text("List") },
                modifier = Modifier.testTag("favourites_view_list")
            )
            FilterChip(
                selected = grid,
                onClick = { if (!grid) { grid = true; onGridChange?.invoke(true) } },
                label = { Text("Grid") },
                modifier = Modifier.testTag("favourites_view_grid")
            )
        }
        FavouritesRoute(
            load = loadFavourites,
            onArtistSelected = onArtistSelected,
            loadArtistImage = loadArtistImage,
            onMoveUp = onMoveFavouriteUp,
            onMoveDown = onMoveFavouriteDown,
            onReorder = onReorderFavourites,
            grid = grid
        )
    }
}
