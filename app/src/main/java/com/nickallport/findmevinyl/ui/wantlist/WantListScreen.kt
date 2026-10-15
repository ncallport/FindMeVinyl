package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.brand.Glyphs
import com.nickallport.findmevinyl.ui.common.ListCard
import com.nickallport.findmevinyl.ui.common.MoveButtons
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel

@Composable
fun WantListScreen(
    wanted: List<WantedAlbumUiModel>,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveUp: (String) -> Unit = {},
    onMoveDown: (String) -> Unit = {}
) {
    if (wanted.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                Glyphs.Heart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(56.dp).alpha(0.8f)
            )
            Text(text = "Your want list is empty", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Tap + Want on an album to add it here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val byArtist = wanted.groupBy { it.artistName }.toList()

    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = onShare,
            modifier = Modifier
                .testTag("share_button")
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Share")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            // One card for each artist, with their wanted albums inside it.
            items(byArtist, key = { it.first }) { (artistName, albums) ->
                ListCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        albums.forEachIndexed { index, album ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = album.albumTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                // An artist with one album has nothing to move it past.
                                if (albums.size > 1) {
                                    MoveButtons(
                                        name = album.albumTitle,
                                        upTag = "want_up_${album.albumId}",
                                        downTag = "want_down_${album.albumId}",
                                        canMoveUp = index > 0,
                                        canMoveDown = index < albums.lastIndex,
                                        onMoveUp = { onMoveUp(album.albumId) },
                                        onMoveDown = { onMoveDown(album.albumId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
