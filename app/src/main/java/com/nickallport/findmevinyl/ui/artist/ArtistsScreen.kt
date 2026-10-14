package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    loadArtistImage: suspend (String) -> Bitmap? = { null }
) {
    var query by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.padding(top = 8.dp)) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = onSearch,
            hint = "Search for an artist",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Your favourites",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
        )
        FavouritesRoute(
            load = loadFavourites,
            onArtistSelected = onArtistSelected,
            loadArtistImage = loadArtistImage
        )
    }
}
