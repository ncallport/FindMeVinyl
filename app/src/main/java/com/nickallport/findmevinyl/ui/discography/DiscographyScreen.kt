package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.discography.DiscographyCategory

private val categoryDisplayOrder = listOf(
    DiscographyCategory.STUDIO_ALBUM to "Studio Albums",
    DiscographyCategory.LIVE_ALBUM to "Live Albums",
    DiscographyCategory.COMPILATION to "Compilations",
    DiscographyCategory.EP to "EPs",
    DiscographyCategory.SINGLE to "Singles",
    DiscographyCategory.OTHER to "Other Releases"
)

@Composable
fun DiscographyScreen(
    groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>,
    onToggleOwned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        categoryDisplayOrder.forEach { (category, headerText) ->
            val albums = groupedAlbums[category]
            if (!albums.isNullOrEmpty()) {
                item {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(albums) { album ->
                    AlbumRow(
                        album = album,
                        onToggleOwned = { onToggleOwned(album.id) }
                    )
                }
            }
        }
    }
}