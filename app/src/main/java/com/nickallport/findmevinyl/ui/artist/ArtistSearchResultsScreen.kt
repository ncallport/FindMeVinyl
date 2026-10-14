package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.artist.ArtistDisplayItem
import com.nickallport.findmevinyl.artist.ArtistSearchUiState

/**
 * Each result has a small picture of the artist, loaded by [loadArtistImage] for the rows that are
 * on screen. The pictures come from Wikimedia Commons, which asks for credit, so a line under the
 * results gives it while at least one picture is on screen.
 */
@Composable
fun ArtistSearchResultsScreen(
    state: ArtistSearchUiState,
    onArtistSelected: (ArtistDisplayItem) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null }
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
            var picturesShown by remember { mutableIntStateOf(0) }

            Column(modifier = modifier) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(state.results, key = { it.id }) { artist ->
                        ArtistResultRow(
                            artist = artist,
                            onSelected = { onArtistSelected(artist) },
                            loadArtistImage = loadArtistImage,
                            onPictureShownChange = { shown -> picturesShown += if (shown) 1 else -1 }
                        )
                    }
                }
                if (picturesShown > 0) {
                    Text(
                        text = "Artist photos: Wikimedia Commons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("photo_credit")
                    )
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
private fun ArtistResultRow(
    artist: ArtistDisplayItem,
    onSelected: () -> Unit,
    loadArtistImage: suspend (String) -> Bitmap?,
    onPictureShownChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .testTag("artist_result_${artist.id}")
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistImage(
            artistId = artist.id,
            loadImage = loadArtistImage,
            onShownChange = onPictureShownChange
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
            Text(text = artist.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
