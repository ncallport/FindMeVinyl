package com.nickallport.findmevinyl.ui.wantlist

import com.nickallport.findmevinyl.ui.theme.fadedWantContainer
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
    onMoveToTop: (String) -> Unit = {}
) {
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
        Text(
            text = "My Wants list",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).testTag("wants_title")
        )
        FilledTonalButton(
            onClick = { showShareOptions = true },
            modifier = Modifier
                .testTag("share_button")
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Share")
        }

        val faded = fadedWantContainer(MaterialTheme.colorScheme)
        LazyColumn(modifier = Modifier.weight(1f)) {
            // One card for each want, in the order shown: the artist and the album, the format if one is wanted, and the
            // buttons to move it. The first ten are in the wanted colour under a heading, the next ten in that colour
            // faded under another, and the rest are plain under a third.
            wanted.forEachIndexed { index, album ->
                if (index == 0) {
                    item(key = "heading-top") { TierHeading("Top ten wanted", "wants_top_heading") }
                }
                if (index == TIER_SIZE) {
                    item(key = "heading-next") { TierHeading("Next ten wanted", "wants_next_heading") }
                }
                if (index == 2 * TIER_SIZE) {
                    item(key = "heading-rest") { TierHeading("The rest", "wants_rest_heading") }
                }
                item(key = album.albumId) {
                    val tier = when {
                        index < TIER_SIZE -> "Top ten"
                        index < 2 * TIER_SIZE -> "Next ten"
                        else -> null
                    }
                    val colors = MaterialTheme.colorScheme
                    ListCard(
                        modifier = Modifier
                            .testTag("want_${album.albumId}")
                            .semantics { if (tier != null) stateDescription = tier },
                        container = when (tier) {
                            "Top ten" -> colors.secondaryContainer
                            "Next ten" -> faded
                            else -> colors.surface
                        }
                    ) {
                        val textColor = if (tier == "Top ten") colors.onSecondaryContainer else colors.onSurface
                        val quietColor = if (tier == "Top ten") colors.onSecondaryContainer else colors.onSurfaceVariant
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${album.artistName} - ${album.albumTitle}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                    modifier = Modifier.testTag("want_title_${album.albumId}")
                                )
                                album.wantFormat?.let {
                                    Text(
                                        text = "Wanted on $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = quietColor,
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
                                IconButton(
                                    onClick = { onMoveToTop(album.albumId) },
                                    enabled = index > 0,
                                    modifier = Modifier.testTag("want_top_${album.albumId}").size(40.dp)
                                ) {
                                    Icon(
                                        Glyphs.DoubleArrowUp,
                                        contentDescription = "Move ${album.albumTitle} to the top",
                                        tint = if (index > 0) colors.primary else colors.outlineVariant
                                    )
                                }
                            }
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

/** How many wants are in each of the two highlighted groups at the top of the list. */
private const val TIER_SIZE = 10

@Composable
private fun TierHeading(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 2.dp).testTag(tag)
    )
}
