package com.nickallport.findmevinyl.ui.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState

@Composable
fun ArtistSearchResultsScreen(
    state: ArtistSearchUiState,
    onArtistSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is ArtistSearchUiState.Idle -> {
            Text(
                text = "Search for an artist to get started",
                modifier = modifier.padding(16.dp)
            )
        }
        is ArtistSearchUiState.Loading -> {
            CircularProgressIndicator(modifier = modifier.testTag("loading_indicator"))
        }
        is ArtistSearchUiState.Success -> {
            LazyColumn(modifier = modifier) {
                items(state.results) { artist ->
                    ArtistResultRow(artist = artist, onSelected = { onArtistSelected(artist.id) })
                }
            }
        }
        is ArtistSearchUiState.Error -> {
            Text(
                text = state.message,
                modifier = modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun ArtistResultRow(artist: ArtistDisplayItem, onSelected: () -> Unit) {
    Column(
        modifier = Modifier
            .testTag("artist_result_${artist.id}")
            .clickable { onSelected() }
            .padding(16.dp)
    ) {
        Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
        Text(text = artist.description, style = MaterialTheme.typography.bodyMedium)
    }
}