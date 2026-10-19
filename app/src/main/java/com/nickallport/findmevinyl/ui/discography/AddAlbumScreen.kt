package com.nickallport.findmevinyl.ui.discography

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import com.nickallport.findmevinyl.discography.MAX_TRACK_TITLE_LENGTH
import com.nickallport.findmevinyl.discography.MAX_USER_TRACKS
import com.nickallport.findmevinyl.discography.MAX_NOTE_LENGTH
import com.nickallport.findmevinyl.ui.common.MoveButtons
import com.nickallport.findmevinyl.ui.brand.Glyphs
import com.nickallport.findmevinyl.ordering.Reorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.discography.AlbumDraft
import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.discography.cleaned

private val kinds = listOf(
    DiscographyCategory.STUDIO_ALBUM to "Studio album",
    DiscographyCategory.LIVE_ALBUM to "Live album",
    DiscographyCategory.COMPILATION to "Compilation",
    DiscographyCategory.EP to "EP",
    DiscographyCategory.SINGLE to "Single",
    DiscographyCategory.OTHER to "Other"
)

/**
 * The form for an album that MusicBrainz does not list: a title, an optional year, what kind of release it is and
 * whether it is a bootleg. With [editing], it is for changing an album added earlier ([initial] fills it in), and has a
 * button to delete it, asked about first. [showHiddenNote] adds a note to check that the album is not one MusicBrainz
 * lists but the settings hide, with a way to the settings. What was typed is kept over a rotation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAlbumScreen(
    artistName: String,
    editing: Boolean,
    initial: AlbumDraft?,
    showHiddenNote: Boolean,
    onSave: (AlbumDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** Something that went wrong with the last save, shown under the buttons. */
    error: String? = null
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(initial?.year.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf((initial?.category ?: DiscographyCategory.LIVE_ALBUM).name) }
    var bootleg by rememberSaveable { mutableStateOf(initial?.bootleg ?: false) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    // The tracks, one to a line (a title never has a line break in it), and the title being typed for the next.
    var tracksText by rememberSaveable { mutableStateOf(initial?.tracks.orEmpty().joinToString("\n")) }
    var trackInput by rememberSaveable { mutableStateOf("") }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val tracks = if (tracksText.isEmpty()) emptyList() else tracksText.split("\n")
    val draft = AlbumDraft(title, year, DiscographyCategory.valueOf(kind), bootleg, description, tracks)
    fun setTracks(list: List<String>) {
        tracksText = list.joinToString("\n")
    }
    val yearOk = year.isBlank() || AlbumDraft(title.ifBlank { "x" }, year, DiscographyCategory.OTHER, false).cleaned() != null
    val valid = draft.cleaned() != null

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (editing) "Edit album" else "Add an album",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("add_album_title_text")
        )
        Text(
            text = artistName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.testTag("add_album_artist")
        )
        if (!editing) {
            Text(
                text = "For an album you have that MusicBrainz does not list. It starts as owned, and its page lets you add a cover image and its copies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showHiddenNote) {
            Text(
                text = "Check first that it is not already listed. Hide Bootlegs is on in Settings, so bootlegs that " +
                    "MusicBrainz lists are hidden until you own or want them.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("add_album_hidden_note")
            )
            TextButton(onClick = onOpenSettings, modifier = Modifier.testTag("add_album_open_settings")) { Text("Open Settings") }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(300) },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("add_album_title")
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.take(10) },
            label = { Text("Year (optional)") },
            singleLine = true,
            isError = !yearOk,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("add_album_year")
        )
        if (!yearOk) {
            Text(
                text = "The year is four digits, such as 1983, or leave it empty.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("add_album_year_error")
            )
        }

        Text(text = "Kind of release", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kinds.forEach { (category, label) ->
                FilterChip(
                    selected = kind == category.name,
                    onClick = { kind = category.name },
                    label = { Text(label) },
                    modifier = Modifier.testTag("add_album_kind_${category.name}")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = bootleg, role = Role.Switch, onValueChange = { bootleg = it })
                .testTag("add_album_bootleg"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "This is a bootleg", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = bootleg, onCheckedChange = null)
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(MAX_NOTE_LENGTH) },
            label = { Text("Description (optional), such as where it is from") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("add_album_description")
        )

        Text(text = "Tracklist (optional)", style = MaterialTheme.typography.labelLarge)
        tracks.forEachIndexed { index, track ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${index + 1}. $track",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).testTag("add_album_track_$index")
                )
                MoveButtons(
                    name = track,
                    upTag = "add_album_track_up_$index",
                    downTag = "add_album_track_down_$index",
                    canMoveUp = index > 0,
                    canMoveDown = index < tracks.lastIndex,
                    onMoveUp = { Reorder.swappedAt(tracks, index, -1)?.let(::setTracks) },
                    onMoveDown = { Reorder.swappedAt(tracks, index, 1)?.let(::setTracks) }
                )
                IconButton(
                    onClick = { setTracks(tracks.filterIndexed { i, _ -> i != index }) },
                    modifier = Modifier.testTag("add_album_track_remove_$index").size(40.dp)
                ) {
                    Icon(Glyphs.Delete, contentDescription = "Remove $track", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val next = trackInput.replace("\n", " ").trim()
            val canAdd = next.isNotEmpty() && tracks.size < MAX_USER_TRACKS
            val addTrack = {
                setTracks(tracks + next)
                trackInput = ""
            }
            OutlinedTextField(
                value = trackInput,
                onValueChange = { trackInput = it.replace("\n", " ").take(MAX_TRACK_TITLE_LENGTH) },
                label = { Text(if (tracks.isEmpty()) "First track" else "Next track") },
                singleLine = true,
                // The keyboard's Done adds the track, so a whole tracklist can be typed without leaving the keyboard.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canAdd) addTrack() }),
                modifier = Modifier.weight(1f).testTag("add_album_track_input")
            )
            OutlinedButton(
                onClick = addTrack,
                enabled = canAdd,
                modifier = Modifier.testTag("add_album_track_add")
            ) { Text("Add track") }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(draft) }, enabled = valid, modifier = Modifier.testTag("add_album_save")) {
                Text(if (editing) "Save changes" else "Add album")
            }
            TextButton(onClick = onCancel, modifier = Modifier.testTag("add_album_cancel")) { Text("Cancel") }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("add_album_error")
            )
        }
        if (editing) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { confirmingDelete = true }, modifier = Modifier.testTag("add_album_delete")) {
                Text("Delete this album")
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this album?") },
            text = { Text("It is taken off your list, with its copies. Albums from MusicBrainz are never deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("add_album_delete_confirm")
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }, modifier = Modifier.testTag("add_album_delete_cancel")) { Text("Keep") }
            }
        )
    }
}
