package com.nickallport.findmevinyl.ui.favourites

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.zIndex
import kotlinx.coroutines.withTimeoutOrNull
import com.nickallport.findmevinyl.ordering.Reorder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.nickallport.findmevinyl.ui.common.MoveButtons

/**
 * One favourite: an artist, or a group of artists the user keeps together (then [isGroup] is true, [id] is the
 * group's id, and [memberIds] are its artists, first first). [imageId] is what the picture is asked for by: the
 * artist's id, or the group's, which shows the photo chosen for the group and otherwise its first member's picture.
 */
data class FavouriteArtistUiModel(
    val id: String,
    val name: String,
    val isGroup: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val imageId: String = id
)

/**
 * The artists are shown in the order given, the user's own order, and each row has buttons to move it
 * up or down. Each artist has a small picture, loaded by [loadArtistImage] for the rows on screen, with the
 * Wikimedia Commons credit under the list while at least one picture is shown.
 */
@Composable
fun FavouritesScreen(
    artists: List<FavouriteArtistUiModel>,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    onMoveUp: (String) -> Unit = {},
    onMoveDown: (String) -> Unit = {},
    /** Show pictures only, as a grid, instead of the list with names and move buttons. */
    grid: Boolean = false,
    /** The ids in their new order, after a picture in the grid was dragged to a new place. */
    onReorder: (List<String>) -> Unit = {}
) {
    if (artists.isEmpty()) {
        Text(
            text = "No favourite artists yet",
            modifier = modifier.padding(16.dp)
        )
    } else {
        var picturesShown by remember { mutableIntStateOf(0) }

        Column(modifier = modifier) {
            if (grid) {
                Text(
                    text = "Touch and hold a picture, then drag it to move it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).testTag("favourites_grid_hint")
                )
                ArtistGrid(
                    artists = artists,
                    loadArtistImage = loadArtistImage,
                    onArtistSelected = onArtistSelected,
                    onReorder = onReorder,
                    onShownChange = { shown -> picturesShown += if (shown) 1 else -1 },
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
                    Row(
                        modifier = Modifier
                            .testTag("favourite_artist_${artist.id}")
                            .fillMaxWidth()
                            .clickable { onArtistSelected(artist) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtistImage(
                            artistId = artist.imageId,
                            loadImage = loadArtistImage,
                            onShownChange = { shown -> picturesShown += if (shown) 1 else -1 }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
                            if (artist.isGroup) {
                                Text(
                                    text = "${artist.memberIds.size} artists joined",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("favourite_group_note_${artist.id}")
                                )
                            }
                        }
                        // With one favourite there is nothing to move past.
                        if (artists.size > 1) {
                            MoveButtons(
                                name = artist.name,
                                upTag = "favourite_up_${artist.id}",
                                downTag = "favourite_down_${artist.id}",
                                canMoveUp = index > 0,
                                canMoveDown = index < artists.lastIndex,
                                onMoveUp = { onMoveUp(artist.id) },
                                onMoveDown = { onMoveDown(artist.id) }
                            )
                        }
                    }
                }
            }
            if (picturesShown > 0) PhotoCredit()
        }
    }
}

/**
 * The grid of pictures. A quick touch opens the artist. Holding a picture lifts it and shows its name, and
 * dragging it on moves it through the grid, the others making room. Lifting the finger keeps the new order
 * ([onReorder]). The gesture is read once for the whole grid, because a tile that has moved to a new place
 * cannot follow a finger that began on it.
 */
@Composable
private fun ArtistGrid(
    artists: List<FavouriteArtistUiModel>,
    loadArtistImage: suspend (String) -> Bitmap?,
    onArtistSelected: (FavouriteArtistUiModel) -> Unit,
    onReorder: (List<String>) -> Unit,
    onShownChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    // One state for the life of the grid: the gesture below is set up once and keeps the state it first saw,
    // so a state made anew for each reloaded list would leave it moving pictures nobody sees.
    var order by remember { mutableStateOf(artists) }
    var liftedId by remember { mutableStateOf<String?>(null) }
    // A list read again (after a drop, or a favourite added) replaces the order, but never under a held picture.
    LaunchedEffect(artists) { if (liftedId == null) order = artists }
    var finger by remember { mutableStateOf(Offset.Zero) }
    val currentOnReorder by rememberUpdatedState(onReorder)
    val edge = with(LocalDensity.current) { 56.dp.toPx() }
    val scrollStep = with(LocalDensity.current) { 12.dp.toPx() }

    fun idAt(at: Offset): String? = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
        at.x >= it.offset.x && at.x < it.offset.x + it.size.width &&
            at.y >= it.offset.y && at.y < it.offset.y + it.size.height
    }?.key as? String

    // Moves the lifted picture to the place under [at], if there is another picture there.
    fun moveLiftedTo(at: Offset) {
        val id = liftedId ?: return
        val over = idAt(at) ?: return
        if (over == id) return
        val target = order.indexOfFirst { it.id == over }
        val moving = order.firstOrNull { it.id == id } ?: return
        order = order.toMutableList().also { it.remove(moving); it.add(target, moving) }
    }

    // A finger held near the top or bottom edge scrolls the grid, so a picture can be taken a long way.
    val scrollDirection = if (liftedId == null) 0 else {
        val height = gridState.layoutInfo.viewportSize.height
        when {
            finger.y < edge -> -1
            finger.y > height - edge -> 1
            else -> 0
        }
    }
    LaunchedEffect(scrollDirection) {
        if (scrollDirection == 0) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            gridState.scrollBy(scrollDirection * scrollStep)
            moveLiftedTo(finger)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 76.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                // A touch lifted or moved on before the long press time is a tap or a scroll, which the tile and
                // the grid handle themselves. Waiting in the first pass sees the touch before they take it.
                val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                        if (change.changedToUp() || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            return@withTimeoutOrNull true
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    false
                }
                if (cancelled == true) return@awaitEachGesture
                val startedAt = idAt(down.position) ?: return@awaitEachGesture
                val before = order
                liftedId = startedAt
                finger = down.position
                var dropped = false
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        // Taken here, so the grid does not scroll and the tile does not count it as a tap.
                        event.changes.forEach { it.consume() }
                        finger = change.position
                        if (!change.pressed) {
                            dropped = true
                            break
                        }
                        moveLiftedTo(change.position)
                    }
                } finally {
                    liftedId = null
                    val ids = order.map { it.id }
                    if (dropped && ids != before.map { it.id }) currentOnReorder(ids) else order = before
                }
            }
        }
    ) {
        items(order, key = { it.id }) { artist ->
            val index = order.indexOfFirst { it.id == artist.id }
            ArtistTile(
                artist = artist,
                loadArtistImage = loadArtistImage,
                lifted = artist.id == liftedId,
                onClick = { onArtistSelected(artist) },
                onMove = { delta ->
                    Reorder.swapped(order.map { it.id }, artist.id, delta)?.let { currentOnReorder(it) }
                },
                canMoveEarlier = index > 0,
                canMoveLater = index < order.lastIndex,
                onShownChange = onShownChange,
                modifier = Modifier.animateItem()
            )
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
    lifted: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onShownChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("favourite_artist_${artist.id}")
            .zIndex(if (lifted) 1f else 0f)
            .scale(if (lifted) 1.1f else 1f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = artist.name
                // A screen reader cannot drag, so it moves a picture a place at a time.
                customActions = listOfNotNull(
                    if (canMoveEarlier) CustomAccessibilityAction("Move earlier") { onMove(-1); true } else null,
                    if (canMoveLater) CustomAccessibilityAction("Move later") { onMove(1); true } else null
                )
            }
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
