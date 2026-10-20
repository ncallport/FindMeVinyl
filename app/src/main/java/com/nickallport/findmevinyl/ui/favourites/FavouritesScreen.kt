package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.artist.ArtistImage
import com.nickallport.findmevinyl.ui.artist.PhotoCredit
import com.nickallport.findmevinyl.ui.common.ListCard

/**
 * One favourite: an artist, or a group of artists the user keeps together (then [isGroup] is true, [id] is the
 * group's id, and [memberIds] are its artists, first first). [imageId] is what the picture is asked for by: the
 * artist's id, or the group's, which shows the photo chosen for the group and otherwise its first member's picture.
 * [ownedCount] and [wantedCount] are how many of the artist's albums (a group's members' together) are owned and wanted.
 */
data class FavouriteArtistUiModel(
    val id: String,
    val name: String,
    val isGroup: Boolean = false,
    val memberIds: List<String> = emptyList(),
    /** The names of the members, in the same order as [memberIds], so a group can be found by them. */
    val memberNames: List<String> = emptyList(),
    val imageId: String = id,
    val ownedCount: Int = 0,
    val wantedCount: Int = 0
)

/**
 * The favourite artists in the order given, which is A to Z. Shown as a list of blocks, each with the artist's
 * small picture on the left, the name, and under it how many albums are owned and wanted, or as a [grid] of
 * pictures with the names on them. Each picture is loaded by [loadArtistImage] for the ones on screen, with the
 * Wikimedia Commons credit under the list while at least one is shown.
 */
@Composable
fun FavouritesScreen(
    artists: List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    /** Show pictures only, as a grid, instead of the list with names and counts. */
    grid: Boolean = false
) {
    if (artists.isEmpty()) {
        Text(
            text = "No favourite artists yet",
            modifier = modifier.padding(16.dp)
        )
    } else {
        var picturesShown by remember { mutableIntStateOf(0) }

        Column(modifier = modifier) {
            val onShownChange: (Boolean) -> Unit = { shown -> picturesShown += if (shown) 1 else -1 }
            if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        ArtistTile(artist, loadArtistImage, onClick = { onArtistSelected(artist) }, onShownChange = onShownChange)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(artists, key = { it.id }) { artist ->
                        ArtistBlock(artist, loadArtistImage, onArtistSelected, onShownChange)
                    }
                }
            }
            if (picturesShown > 0) PhotoCredit()
        }
    }
}

/** What an artist has: "4 owned, 3 wanted". */
internal fun countsText(artist: FavouriteArtistUiModel) = "${artist.ownedCount} owned, ${artist.wantedCount} wanted"

/** One favourite in the list: a block with its picture, name, how many albums are owned and wanted, and for a group how many artists it joins. */
@Composable
private fun ArtistBlock(
    artist: FavouriteArtistUiModel,
    loadArtistImage: suspend (String) -> Bitmap?,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    onShownChange: (Boolean) -> Unit
) {
    ListCard(modifier = Modifier.testTag("favourite_artist_${artist.id}"), onClick = { onArtistSelected(artist) }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtistImage(artistId = artist.imageId, loadImage = loadArtistImage, onShownChange = onShownChange)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = countsText(artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("favourite_counts_${artist.id}")
                )
                if (artist.isGroup) {
                    Text(
                        text = "${artist.memberIds.size} artists joined",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("favourite_group_note_${artist.id}")
                    )
                }
            }
        }
    }
}

/**
 * One artist in the grid: the picture, filling a square, with the name along the bottom on a dark bar, cut
 * to one line with an ellipsis when it is long. The name is also the description of the tile for a screen reader.
 */
@Composable
private fun ArtistTile(
    artist: FavouriteArtistUiModel,
    loadArtistImage: suspend (String) -> Bitmap?,
    onClick: () -> Unit,
    onShownChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .testTag("favourite_artist_${artist.id}")
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = artist.name }
    ) {
        ArtistImage(
            artistId = artist.imageId,
            loadImage = loadArtistImage,
            fillSize = true,
            onShownChange = onShownChange
        )
        Text(
            text = artist.name,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
