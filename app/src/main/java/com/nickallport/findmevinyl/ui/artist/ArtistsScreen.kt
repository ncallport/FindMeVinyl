package com.nickallport.findmevinyl.ui.artist

import androidx.compose.runtime.mutableIntStateOf
import android.graphics.Bitmap
import kotlinx.coroutines.delay
import com.nickallport.findmevinyl.ui.brand.Glyphs
import com.nickallport.findmevinyl.artist.ArtistSuggestions
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistsScreen(
    onSearch: (String) -> Unit,
    loadFavourites: suspend () -> List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    /** Show the favourites as a grid of pictures when the page opens. */
    startInGrid: Boolean = false,
    /** Told when the user switches between the list and the grid, so the choice can be kept. */
    onGridChange: ((Boolean) -> Unit)? = null,
    /** Finds the suggested genre of each favourite in the background, started when the page opens. Without it nothing is looked for. */
    fillGenreDefaults: (suspend () -> Unit)? = null,
    /** Artists from MusicBrainz for the text typed so far. Without it there are no suggestions. */
    suggestArtists: (suspend (String) -> List<ArtistSuggestion>)? = null,
    /** Opens the page for adding an owned album from its barcode. Without it there is no button. */
    onAddByBarcode: (() -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    var grid by rememberSaveable { mutableStateOf(startInGrid) }
    LaunchedEffect(Unit) { fillGenreDefaults?.invoke() }

    // The artists offered while typing: the favourites that match at once, the others once typing pauses.
    var suggestions by remember { mutableStateOf<List<ArtistSuggestion>>(emptyList()) }
    LaunchedEffect(query) {
        if (suggestArtists == null) return@LaunchedEffect
        if (query.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        val local = ArtistSuggestions.favouritesMatching(query, loadFavourites())
        suggestions = local
        // The effect starts again with each letter, which cancels this wait and any answer still on its way,
        // so only the last text is asked for.
        delay(ARTIST_SUGGESTION_WAIT_MILLIS)
        suggestions = ArtistSuggestions.merge(local, suggestArtists(query))
    }

    Column(modifier = modifier.padding(top = 8.dp)) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = { text ->
                suggestions = emptyList()
                onSearch(text)
            },
            hint = "Search for an artist",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (onAddByBarcode != null) {
            TextButton(
                onClick = onAddByBarcode,
                modifier = Modifier.padding(start = 8.dp).testTag("barcode_button")
            ) { Text("Add an owned album by barcode") }
        }
        // The favourites stay where they are, with their state, and the suggestions are drawn over them.
        Box(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = "Your favourites",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    grid = grid
                )
            }
            if (suggestions.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        suggestions.forEachIndexed { index, suggestion ->
                            Column(
                                modifier = Modifier
                                    .testTag("artist_suggestion_$index")
                                    .fillMaxWidth()
                                    .clickable {
                                        suggestions = emptyList()
                                        onArtistSelected(FavouriteArtistUiModel(suggestion.id, suggestion.name))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (suggestion.favourite) {
                                        Icon(Glyphs.Heart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(text = suggestion.name, style = MaterialTheme.typography.bodyLarge)
                                }
                                Text(
                                    text = suggestion.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** How long the typing has to pause before MusicBrainz is asked, a little longer than for towns because it shares one request a second. */
internal const val ARTIST_SUGGESTION_WAIT_MILLIS = 700L
