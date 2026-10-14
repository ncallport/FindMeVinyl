package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.SearchResultCache
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState
import com.nickallport.findmevinyl.ui.common.SearchField

/**
 * The results of an artist search. It searches for [query] when it opens, unless [cache] already
 * holds the results for that same query (after a rotation, or coming back from a discography).
 * The field at the top starts with the query, and searching again asks [onSearch] for a new page.
 * Only a successful search is kept: an error is tried again next time.
 */
@Composable
fun ArtistResultsRoute(
    query: String,
    search: suspend (String) -> ArtistSearchUiState,
    onArtistSelected: (ArtistDisplayItem) -> Unit,
    onSearch: (String) -> Unit,
    cache: SearchResultCache,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null }
) {
    var text by rememberSaveable(query) { mutableStateOf(query) }
    var uiState by remember(query) {
        mutableStateOf<ArtistSearchUiState>(
            if (cache.artistQuery == query) cache.artist ?: ArtistSearchUiState.Loading else ArtistSearchUiState.Loading
        )
    }

    LaunchedEffect(query) {
        if (uiState is ArtistSearchUiState.Loading) {
            val result = search(query)
            uiState = result
            if (result is ArtistSearchUiState.Success) {
                cache.artist = result
                cache.artistQuery = query
            }
        }
    }

    Column(modifier = modifier.padding(top = 8.dp)) {
        SearchField(
            value = text,
            onValueChange = { text = it },
            onSearch = onSearch,
            hint = "Search for an artist",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        ArtistSearchResultsScreen(
            state = uiState,
            onArtistSelected = onArtistSelected,
            loadArtistImage = loadArtistImage
        )
    }
}
