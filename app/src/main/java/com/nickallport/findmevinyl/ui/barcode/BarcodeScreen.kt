package com.nickallport.findmevinyl.ui.barcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.barcode.BarcodeMatch
import com.nickallport.findmevinyl.barcode.BarcodeStatus
import com.nickallport.findmevinyl.discography.ReleaseFormats
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import com.nickallport.findmevinyl.ui.common.ListCard

/** A match with what the app already has of it. */
data class BarcodeMatchUi(val match: BarcodeMatch, val status: BarcodeStatus)

/** What the page shows under the barcode field. */
sealed class BarcodePageState {
    object Idle : BarcodePageState()
    object Looking : BarcodePageState()
    /** Nothing found, a number that is not a barcode, or a failure. */
    data class Message(val text: String) : BarcodePageState()
    data class Matches(val items: List<BarcodeMatchUi>) : BarcodePageState()
}

/**
 * The page for adding an owned album from its barcode: a field for the number, and under it the albums found, each with
 * what the app already has of the artist and the album, a choice of format for the copy, and a button to add it.
 * [added] holds the albums added in this visit, [busyAlbumId] the one being added, and [error] a failure to add.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarcodeScreen(
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onLookUp: () -> Unit,
    onScan: (() -> Unit)?,
    state: BarcodePageState,
    added: Map<String, String>,
    busyAlbumId: String?,
    error: String?,
    onAdd: (BarcodeMatch, format: String, takeOffWant: Boolean) -> Unit,
    onOpenAlbum: (BarcodeMatch, albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Text(
            text = "Add by barcode",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("barcode_title")
        )
        OutlinedTextField(
            value = barcode,
            onValueChange = onBarcodeChange,
            label = { Text("Barcode number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onLookUp() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("barcode_input")
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onLookUp,
                enabled = barcode.isNotBlank() && state != BarcodePageState.Looking,
                modifier = Modifier.testTag("barcode_lookup")
            ) { Text("Look up") }
            if (onScan != null) {
                OutlinedButton(
                    onClick = onScan,
                    enabled = state != BarcodePageState.Looking,
                    modifier = Modifier.testTag("barcode_scan")
                ) { Text("Scan with the camera") }
            }
        }

        when (state) {
            BarcodePageState.Idle -> Text(
                text = if (onScan != null) "Photograph the barcode on the sleeve, or type the number under it. The album is found on MusicBrainz."
                else "Type the number under the barcode on the sleeve. The album is found on MusicBrainz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("barcode_hint")
            )
            BarcodePageState.Looking -> RecordSpinner(modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("loading_indicator"))
            is BarcodePageState.Message -> Text(
                text = state.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("barcode_message")
            )
            is BarcodePageState.Matches -> state.items.forEachIndexed { index, item ->
                MatchCard(
                    index = index, item = item, addedId = added[item.match.releaseGroupId],
                    busy = busyAlbumId == item.match.releaseGroupId, error = error.takeIf { busyAlbumId == null },
                    onAdd = onAdd, onOpenAlbum = onOpenAlbum
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchCard(
    index: Int,
    item: BarcodeMatchUi,
    addedId: String?,
    busy: Boolean,
    error: String?,
    onAdd: (BarcodeMatch, String, Boolean) -> Unit,
    onOpenAlbum: (BarcodeMatch, albumId: String) -> Unit
) {
    val match = item.match
    val isAdded = addedId != null
    val status = item.status
    var format by rememberSaveable(match.releaseGroupId) {
        mutableStateOf(match.formats.firstOrNull() ?: ReleaseFormats.LP)
    }
    var takeOffWant by rememberSaveable(match.releaseGroupId) { mutableStateOf(true) }
    // A want on a format goes by itself when a copy on that format is added. A want on any format asks.
    val wantOnAnyFormat = status.wanted && status.wantFormat == null

    ListCard(modifier = Modifier.testTag("barcode_match_$index")) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${match.artistName} - ${match.albumTitle}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("barcode_match_title_$index")
            )
            if (!isAdded) {
                sourceLine(match)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("barcode_source_$index"))
                }
                Text(
                    text = artistLine(status),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("barcode_artist_status_$index")
                )
                albumLine(status)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("barcode_album_status_$index"))
                }
            }

            if (isAdded) {
                Text("Added to your owned albums.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("barcode_added_$index"))
                TextButton(onClick = { onOpenAlbum(match, addedId.orEmpty()) }, modifier = Modifier.testTag("barcode_open_$index")) { Text("Open the album") }
            } else {
                Text(
                    text = "Format of your copy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReleaseFormats.displayOrder.forEach { label ->
                        FilterChip(
                            selected = format == label,
                            onClick = { format = label },
                            label = { Text(label) },
                            modifier = Modifier.testTag("barcode_format_${index}_$label")
                        )
                    }
                }
                if (wantOnAnyFormat) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .toggleable(value = takeOffWant, role = Role.Switch, onValueChange = { takeOffWant = it })
                            .testTag("barcode_take_off_want_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(checked = takeOffWant, onCheckedChange = null)
                        Text("Take it off my Wants list", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("barcode_error")) }
                if (busy) {
                    RecordSpinner(modifier = Modifier.fillMaxWidth().testTag("barcode_adding_$index"), size = 20.dp)
                } else {
                    Button(
                        onClick = { onAdd(match, format, wantOnAnyFormat && takeOffWant) },
                        modifier = Modifier.testTag("barcode_add_$index")
                    ) { Text(if (status.owned) "Add another copy" else "Add to owned") }
                }
            }
        }
    }
}

internal fun artistLine(status: BarcodeStatus): String = when {
    status.special != null -> "Goes in ${status.special}, which is not one of your favourites."
    status.groupName != null -> "Part of your group ${status.groupName}."
    status.artistIsFavourite -> "Already in your favourites."
    else -> "A new artist, who will be added to your favourites."
}

internal fun albumLine(status: BarcodeStatus): String? {
    val parts = mutableListOf<String>()
    if (status.owned) {
        val copies = status.copyFormats
        parts += if (copies.isEmpty()) "You already own this album." else "You already own this album: ${copies.joinToString(", ")}."
    }
    if (status.wanted) {
        parts += status.wantFormat?.let { "On your Wants list, wanted on $it." } ?: "On your Wants list."
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

/** Where the match came from, when it was not MusicBrainz's own list of barcodes. */
internal fun sourceLine(match: BarcodeMatch): String? = when {
    match.byHand -> "MusicBrainz does not know this barcode. Discogs does, and MusicBrainz has no such album under ${match.artistName}, so it will be added by hand."
    match.fromDiscogs -> "MusicBrainz does not know this barcode. Discogs does, and the album was matched to MusicBrainz by name."
    else -> null
}
