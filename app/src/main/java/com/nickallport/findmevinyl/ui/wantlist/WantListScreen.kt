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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nickallport.findmevinyl.export.ShareOptions
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
    onShare: (ShareOptions) -> Unit,
    modifier: Modifier = Modifier,
    onMoveUp: (String) -> Unit = {},
    onMoveDown: (String) -> Unit = {},
    onRemove: (String) -> Unit = {}
) {
    // The album the user has tapped remove on, until they answer the question.
    var pendingRemoval by remember { mutableStateOf<WantedAlbumUiModel?>(null) }

    // What to share, asked for when Share is tapped. The choices are kept for the next time, and over a rotation.
    var showShareOptions by rememberSaveable { mutableStateOf(false) }
    var shareIntro by rememberSaveable { mutableStateOf("") }
    var shareLimit by rememberSaveable { mutableStateOf<Int?>(null) }

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

    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = { showShareOptions = true },
            modifier = Modifier
                .testTag("share_button")
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Share")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            // One card for each want, in the user's own order: the artist and the album, the format if one is wanted,
            // and the buttons to move it and to remove it.
            itemsIndexed(wanted, key = { _, album -> album.albumId }) { index, album ->
                ListCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${album.artistName} - ${album.albumTitle}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("want_title_${album.albumId}")
                            )
                            album.wantFormat?.let {
                                Text(
                                    text = "on $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("want_format_${album.albumId}")
                                )
                            }
                        }
                        // With one want there is nothing to move it past.
                        if (wanted.size > 1) {
                            MoveButtons(
                                name = album.albumTitle,
                                upTag = "want_up_${album.albumId}",
                                downTag = "want_down_${album.albumId}",
                                canMoveUp = index > 0,
                                canMoveDown = index < wanted.lastIndex,
                                onMoveUp = { onMoveUp(album.albumId) },
                                onMoveDown = { onMoveDown(album.albumId) }
                            )
                        }
                        IconButton(
                            onClick = { pendingRemoval = album },
                            modifier = Modifier.testTag("want_remove_${album.albumId}").size(40.dp)
                        ) {
                            Icon(
                                Glyphs.Delete,
                                contentDescription = "Remove ${album.albumTitle} from wants",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareOptions) {
        ShareOptionsDialog(
            total = wanted.size,
            intro = shareIntro,
            onIntroChange = { shareIntro = it },
            limit = shareLimit,
            onLimitChange = { shareLimit = it },
            onShare = {
                showShareOptions = false
                onShare(ShareOptions(intro = shareIntro, limit = shareLimit?.takeIf { it < wanted.size }))
            },
            onCancel = { showShareOptions = false }
        )
    }

    pendingRemoval?.let { album ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${album.albumTitle} from your wants?") },
            text = { Text("The album stays in its discography, as not wanted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        onRemove(album.albumId)
                    },
                    modifier = Modifier.testTag("want_remove_confirm")
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoval = null },
                    modifier = Modifier.testTag("want_remove_cancel")
                ) { Text("Keep") }
            },
            modifier = Modifier.testTag("want_remove_dialog")
        )
    }
}

// How many albums can be chosen, when the list is long enough for that choice to mean something.
private val LIMIT_CHOICES = listOf(10, 20, 30, 50)

/** The question before sharing: an optional message, and how much of the list to share. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareOptionsDialog(
    total: Int,
    intro: String,
    onIntroChange: (String) -> Unit,
    limit: Int?,
    onLimitChange: (Int?) -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit
) {
    val choices = LIMIT_CHOICES.filter { it < total }
    // A choice kept from a longer list, that is now as long as the list or longer, means all of it.
    val chosen = limit?.takeIf { it < total }
    val albums = if (total == 1) "1 album" else "$total albums"
    val summary = if (chosen == null) "Sharing ${if (total == 1) "1 album" else "all $total albums"}" else "Sharing the top $chosen of $albums"

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Share your want list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = intro,
                    onValueChange = onIntroChange,
                    label = { Text("Message (optional)") },
                    placeholder = { Text("It's my birthday soon, here are some ideas") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("share_intro")
                )
                if (choices.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        choices.forEach { n ->
                            FilterChip(
                                selected = chosen == n,
                                onClick = { onLimitChange(n) },
                                label = { Text("Top $n") },
                                modifier = Modifier.testTag("share_limit_$n")
                            )
                        }
                        FilterChip(
                            selected = chosen == null,
                            onClick = { onLimitChange(null) },
                            label = { Text("All") },
                            modifier = Modifier.testTag("share_limit_all")
                        )
                    }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("share_summary")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onShare, modifier = Modifier.testTag("share_confirm")) { Text("Share") }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("share_cancel")) { Text("Cancel") }
        },
        modifier = Modifier.testTag("share_dialog")
    )
}
