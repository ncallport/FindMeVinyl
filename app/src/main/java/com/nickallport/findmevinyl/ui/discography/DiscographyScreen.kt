package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.ui.brand.Glyphs
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import com.nickallport.findmevinyl.discography.Grouping
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

private enum class GroupSection(val key: String, val title: String) {
    OWNED("owned", "Owned"),
    WANTED("wanted", "Wanted")
}

private val categoryDisplayOrder = listOf(
    DiscographyCategory.STUDIO_ALBUM to "Studio Albums",
    DiscographyCategory.LIVE_ALBUM to "Live Albums",
    DiscographyCategory.COMPILATION to "Compilations",
    DiscographyCategory.EP to "EPs",
    DiscographyCategory.SINGLE to "Singles",
    DiscographyCategory.OTHER to "Other Releases"
)

/**
 * With [grouping], the albums you own and the ones you want are taken out of their categories and shown
 * together in an Owned and a Wanted section at the top, each in the order of the categories. With both on,
 * the section that is not first has an arrow that puts it first ([onWantedFirstChange]). An album you own
 * is never in the Wanted section. Every row has a key, so when albums are added above what is on screen as
 * more pages load, the list stays where it is. [loadingMore] adds a footer while pages are still arriving, and
 * [loadError] a footer saying some albums could not be loaded, with a button to try again.
 */
@Composable
fun DiscographyScreen(
    groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>,
    onToggleOwned: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    grouping: Grouping = Grouping.None,
    /** Tells that the Wanted section (true) or the Owned section (false) was put first. */
    onWantedFirstChange: (Boolean) -> Unit = {},
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    loadingMore: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    checkingFormats: Boolean = false,
    /** The artist's name for rows that do not say whose album they are, as on a single artist's page. */
    defaultArtistName: String? = null
) {
    fun isGroupedOwned(album: AlbumUiModel) = grouping.owned && album.owned
    fun isGroupedWanted(album: AlbumUiModel) = grouping.wanted && album.wanted && !album.owned

    val inCategoryOrder = categoryDisplayOrder.flatMap { (category, _) -> groupedAlbums[category].orEmpty() }
    val owned = inCategoryOrder.filter { isGroupedOwned(it) }
    val wanted = inCategoryOrder.filter { isGroupedWanted(it) }
    val categorised = groupedAlbums.mapValues { (_, albums) ->
        albums.filterNot { isGroupedOwned(it) || isGroupedWanted(it) }
    }
    // The sections that have something in them, in the order they are shown.
    val sections = listOf(GroupSection.OWNED to owned, GroupSection.WANTED to wanted)
        .let { if (grouping.wantedFirst) it.reversed() else it }
        .filter { (_, albums) -> albums.isNotEmpty() }

    // The list keeps its place by item, so a section added at the top would be out of sight above it.
    // Changing the grouping starts from the top. A list that is only restored (a rotation) stays.
    val listState = rememberLazyListState()
    var shownGrouping by remember { mutableStateOf(grouping) }
    LaunchedEffect(grouping) {
        if (grouping != shownGrouping) {
            shownGrouping = grouping
            listState.scrollToItem(0)
        }
    }

    LazyColumn(state = listState, modifier = modifier.testTag("discography_list")) {
        sections.forEachIndexed { position, (section, albums) ->
            item(key = "header-${section.key}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("${section.key}_section_header")
                    )
                    // Only the second of two sections can be put first.
                    if (position == 1) {
                        IconButton(
                            onClick = { onWantedFirstChange(section == GroupSection.WANTED) },
                            modifier = Modifier.testTag("group_first_${section.key}").size(40.dp)
                        ) {
                            Icon(
                                Glyphs.ArrowUp,
                                contentDescription = "Move ${section.title} to the top",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            items(albums, key = { it.id }) { album ->
                AlbumRow(
                    album = album,
                    onOpen = { onOpenAlbum(album.id) },
                    onToggleOwned = { onToggleOwned(album.id) },
                    onToggleWanted = { onToggleWanted(album.id) },
                    loadCover = loadCover,
                    loadFormats = loadFormats,
                    checkingFormats = checkingFormats,
                    defaultArtistName = defaultArtistName
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
                        checkingFormats = checkingFormats,
                    defaultArtistName = defaultArtistName
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
