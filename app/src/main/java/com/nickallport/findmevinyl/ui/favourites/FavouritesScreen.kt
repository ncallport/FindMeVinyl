package com.nickallport.findmevinyl.ui.favourites

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
import com.nickallport.findmevinyl.ui.artist.ArtistImage
import com.nickallport.findmevinyl.ui.artist.PhotoCredit

data class FavouriteArtistUiModel(
    val id: String,
    val name: String
)

/**
 * Each artist has a small picture, loaded by [loadArtistImage] for the rows on screen, with the
 * Wikimedia Commons credit under the list while at least one picture is shown.
 */
@Composable
fun FavouritesScreen(
    artists: List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null }
) {
    if (artists.isEmpty()) {
        Text(
            text = "No favourite artists yet",
            modifier = modifier.padding(16.dp)
        )
    } else {
        var picturesShown by remember { mutableIntStateOf(0) }

        Column(modifier = modifier) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(artists, key = { it.id }) { artist ->
                    Row(
                        modifier = Modifier
                            .testTag("favourite_artist_${artist.id}")
                            .fillMaxWidth()
                            .clickable { onArtistSelected(artist) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtistImage(
                            artistId = artist.id,
                            loadImage = loadArtistImage,
                            onShownChange = { shown -> picturesShown += if (shown) 1 else -1 }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (picturesShown > 0) PhotoCredit()
        }
    }
}
