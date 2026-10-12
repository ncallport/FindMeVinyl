package com.nickallport.findmevinyl.ui.favourites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class FavouriteArtistUiModel(
    val id: String,
    val name: String
)

@Composable
fun FavouritesScreen(
    artists: List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        Text(
            text = "No favourite artists yet",
            modifier = modifier.padding(16.dp)
        )
    } else {
        LazyColumn(modifier = modifier) {
            items(artists) { artist ->
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .testTag("favourite_artist_${artist.id}")
                        .fillMaxWidth()
                        .clickable { onArtistSelected(artist) }
                        .padding(16.dp)
                )
            }
        }
    }
}