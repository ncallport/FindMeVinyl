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
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
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
 * With [ownedFirst], the albums you own are taken out of their categories and shown together in an
 * Owned section at the top, in the order of the categories. Every row has a key, so when albums are added above what is on screen as more pages load, the
 * list stays where it is. [loadingMore] adds a footer while pages are still arriving, and
 * [loadError] a footer saying some albums could not be loaded, with a button to try again.
 */
@Composable
fun DiscographyScreen(
    groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>,
    onToggleOwned: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    ownedFirst: Boolean = false,
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    loadingMore: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    checkingFormats: Boolean = false
) {
    val owned = if (ownedFirst) {
        categoryDisplayOrder.flatMap { (category, _) -> groupedAlbums[category].orEmpty().filter { it.owned } }
    } else {
        emptyList()
    }
    val categorised = if (ownedFirst) {
        groupedAlbums.mapValues { (_, albums) -> albums.filterNot { it.owned } }
    } else {
        groupedAlbums
    }

    // The list keeps its place by item, so a section added at the top would be out of sight above it.
    // Switching owned first on or off starts from the top. A list that is only restored (a rotation) stays.
    val listState = rememberLazyListState()
    var shownOwnedFirst by remember { mutableStateOf(ownedFirst) }
    LaunchedEffect(ownedFirst) {
        if (ownedFirst != shownOwnedFirst) {
            shownOwnedFirst = ownedFirst
            listState.scrollToItem(0)
        }
    }

    LazyColumn(state = listState, modifier = modifier.testTag("discography_list")) {
        if (owned.isNotEmpty()) {
            item(key = "header-owned") {
                Text(
                    text = "Owned",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .testTag("owned_section_header")
                        .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
                )
            }
            items(owned, key = { it.id }) { album ->
                AlbumRow(
                    album = album,
                    onOpen = { onOpenAlbum(album.id) },
                    onToggleOwned = { onToggleOwned(album.id) },
                    onToggleWanted = { onToggleWanted(album.id) },
                    loadCover = loadCover,
                    loadFormats = loadFormats,
                    checkingFormats = checkingFormats
                )
            }
        }
        categoryDisplayOrder.forEach { (category, headerText) ->
            val albums = categorised[category]
            if (!albums.isNullOrEmpty()) {
                item(key = "header-${category.name}") {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
                    )
                }
                items(albums, key = { it.id }) { album ->
                    AlbumRow(
                        album = album,
                        onOpen = { onOpenAlbum(album.id) },
                        onToggleOwned = { onToggleOwned(album.id) },
                        onToggleWanted = { onToggleWanted(album.id) },
                        loadCover = loadCover,
                        loadFormats = loadFormats,
                        checkingFormats = checkingFormats
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
                    RecordSpinner(size = 20.dp)
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
