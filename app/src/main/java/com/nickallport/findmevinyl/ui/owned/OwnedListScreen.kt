package com.nickallport.findmevinyl.ui.owned

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.owned.GiveMeFive
import com.nickallport.findmevinyl.owned.OwnedAlbumUiModel
import com.nickallport.findmevinyl.ui.brand.Glyphs
import com.nickallport.findmevinyl.ui.common.ListCard
import com.nickallport.findmevinyl.ui.discography.CoverImage
import kotlin.random.Random

/**
 * The complete Owned list: a card for every album the user owns, in the order given (A to Z by artist), each with the
 * copies they have. Tapping a card opens the album. Share sends the whole list as text, and Give Me Five picks five of the
 * albums at random in a dialog, with Refresh for another five ([random] is only given by tests).
 */
@Composable
fun OwnedListScreen(
    owned: List<OwnedAlbumUiModel>,
    onShare: () -> Unit,
    onOpenAlbum: (OwnedAlbumUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadCover: suspend (String) -> Bitmap? = { null },
    random: Random = Random.Default
) {
    // The ids of the five being shown, one to a line so the choice survives a rotation. Empty: the dialog is closed.
    var fiveIds by rememberSaveable { mutableStateOf("") }

    if (owned.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                Glyphs.Record,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp).alpha(0.8f)
            )
            Text(text = "Your Owned list is empty", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("owned_empty"))
            Text(
                text = "Tap + Owned on an album to add it here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier = modifier) {
        val albums = if (owned.size == 1) "1 album" else "${owned.size} albums"
        Text(
            text = "My Owned list ($albums)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).testTag("owned_title")
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { fiveIds = GiveMeFive.pick(owned.map { it.albumId }, random).joinToString("\n") },
                modifier = Modifier.testTag("give_me_five")
            ) { Text("Give Me Five") }
            FilledTonalButton(onClick = onShare, modifier = Modifier.testTag("owned_share")) { Text("Share") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(owned, key = { it.albumId }) { album ->
                ListCard(modifier = Modifier.testTag("owned_${album.albumId}"), onClick = { onOpenAlbum(album) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = "${album.artistName} - ${album.title}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("owned_name_${album.albumId}")
                        )
                        album.copies.forEachIndexed { index, copy ->
                            Text(
                                text = copy.note?.let { "${copy.format}: $it" } ?: copy.format,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("owned_copy_${album.albumId}_$index")
                            )
                        }
                    }
                }
            }
        }
    }

    if (fiveIds.isNotEmpty()) {
        val shownIds = fiveIds.split("\n")
        val five = shownIds.mapNotNull { id -> owned.firstOrNull { it.albumId == id } }
        // With five or fewer there is nothing else to pick.
        val canRefresh = owned.size > GiveMeFive.COUNT
        GiveMeFiveDialog(
            five = five,
            canRefresh = canRefresh,
            loadCover = loadCover,
            onOpenAlbum = { fiveIds = ""; onOpenAlbum(it) },
            onRefresh = { fiveIds = GiveMeFive.pick(owned.map { it.albumId }, random, avoid = shownIds.toSet()).joinToString("\n") },
            onClose = { fiveIds = "" }
        )
    }
}

/** The five records, each with its cover, artist, album and copies. Tapping one opens it. */
@Composable
private fun GiveMeFiveDialog(
    five: List<OwnedAlbumUiModel>,
    canRefresh: Boolean,
    loadCover: suspend (String) -> Bitmap?,
    onOpenAlbum: (OwnedAlbumUiModel) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Give Me Five") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                five.forEach { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("five_${album.albumId}")
                            .clickable { onOpenAlbum(album) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverImage(albumId = album.albumId, loadCover = loadCover)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${album.artistName} - ${album.title}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("five_name_${album.albumId}")
                            )
                            if (album.copies.isNotEmpty()) {
                                Text(
                                    text = album.copies.map { it.format }.distinct().joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("five_copies_${album.albumId}")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (canRefresh) {
                TextButton(onClick = onRefresh, modifier = Modifier.testTag("five_refresh")) { Text("Refresh") }
            } else {
                TextButton(onClick = onClose, modifier = Modifier.testTag("five_close")) { Text("Close") }
            }
        },
        dismissButton = {
            if (canRefresh) TextButton(onClick = onClose, modifier = Modifier.testTag("five_close")) { Text("Close") }
        },
        modifier = Modifier.testTag("five_dialog")
    )
}
