package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.discography.DiscographyCategory

private val categoryDisplayOrder = listOf(
    DiscographyCategory.STUDIO_ALBUM to "Studio Albums",
    DiscographyCategory.LIVE_ALBUM to "Live Albums",
    DiscographyCategory.COMPILATION to "Compilations",
    DiscographyCategory.EP to "EPs",
    DiscographyCategory.SINGLE to "Singles",
    DiscographyCategory.OTHER to "Other Releases"
)

/**
 * Every row has a key, so when albums are added above what is on screen as more pages load, the
 * list stays where it is. [loadingMore] adds a footer while pages are still arriving, and
 * [loadError] a footer saying some albums could not be loaded, with a button to try again.
 */
@Composable
fun DiscographyScreen(
    groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>,
    onToggleOwned: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: (String) -> Unit = {},
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    loadingMore: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {}
) {
    LazyColumn(modifier = modifier.testTag("discography_list")) {
        categoryDisplayOrder.forEach { (category, headerText) ->
            val albums = groupedAlbums[category]
            if (!albums.isNullOrEmpty()) {
                item(key = "header-${category.name}") {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(albums, key = { it.id }) { album ->
                    AlbumRow(
                        album = album,
                        onToggleOwned = { onToggleOwned(album.id) },
                        onToggleWanted = { onToggleWanted(album.id) },
                        loadCover = loadCover,
                        loadFormats = loadFormats
                    )
                }
            }
        }

        if (loadingMore) {
            item(key = "loading-more") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("loading_more"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Loading more albums…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (loadError != null) {
            item(key = "load-error") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("load_error")) {
                    Text(text = "Couldn't load all the albums")
                    Text(
                        text = loadError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRetry, modifier = Modifier.testTag("retry_button")) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}
