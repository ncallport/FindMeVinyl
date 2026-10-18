package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.discography.Pressing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.discography.FormatNotes
import com.nickallport.findmevinyl.discography.MAX_NOTE_LENGTH
import com.nickallport.findmevinyl.discography.ReleaseFormats

/**
 * What the user says about their own copy of an album they own: formats MusicBrainz does not list (the
 * Olympia vinyl is on no MusicBrainz release), and a note such as the country or the language of the sleeve.
 * The formats are added to MusicBrainz's, never replacing them. [onSave] gets the chosen formats and the note.
 *
 * The page's own pressing can be chosen as the copy here too ("This is my pressing"), and the choice cleared.
 *
 * Whether the editor is open, and what has been chosen and typed in it, are kept over a rotation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyCopySection(
    album: AlbumUiModel,
    onSave: (formats: List<String>, note: String, formatNotes: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    /** The pressing the page is showing, which "This is my pressing" would choose. Null when it is not known. */
    shownPressing: Pressing? = null,
    /** Chooses [Pressing] as the user's own copy, or takes the choice away with null. */
    onMarkPressing: (Pressing?) -> Unit = {}
) {
    var editing by rememberSaveable(album.id) { mutableStateOf(false) }
    // The chosen formats are kept as the text the database uses, which a rotation can keep.
    var chosen by rememberSaveable(album.id) { mutableStateOf("") }
    var note by rememberSaveable(album.id) { mutableStateOf("") }
    // The note of each format, as the text the database uses, which a rotation can keep.
    var formatNotesText by rememberSaveable(album.id) { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth().testTag("my_copy_section")) {
        Text(text = "My copy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        if (!editing) {
            val summary = listOfNotNull(
                album.userFormats.takeIf { it.isNotEmpty() }?.let { "Formats you added: ${it.joinToString(" · ")}" },
                album.note?.takeIf { it.isNotBlank() }
            ).plus(album.formatNotes.map { (label, text) -> "$label: $text" }).joinToString("\n")
            if (summary.isNotEmpty()) {
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("my_copy_summary"))
                Spacer(Modifier.height(8.dp))
            }
            if (album.myPressingId != null) {
                Text(
                    text = "My pressing: ${album.myPressingText ?: "chosen"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("my_pressing_text")
                )
                Spacer(Modifier.height(8.dp))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        chosen = ReleaseFormats.encodeUser(album.userFormats)
                        note = album.note.orEmpty()
                        formatNotesText = FormatNotes.encode(album.formatNotes, ReleaseFormats.displayOrder).orEmpty()
                        editing = true
                    },
                    modifier = Modifier.testTag("my_copy_edit")
                ) {
                    Text(if (summary.isEmpty()) "Add details of my copy" else "Edit my copy")
                }
                // The pressing the page shows, if it is not the one already chosen.
                if (shownPressing != null && shownPressing.releaseId != album.myPressingId) {
                    OutlinedButton(onClick = { onMarkPressing(shownPressing) }, modifier = Modifier.testTag("my_pressing_mark")) {
                        Text("This is my pressing")
                    }
                }
                if (album.myPressingId != null) {
                    TextButton(onClick = { onMarkPressing(null) }, modifier = Modifier.testTag("my_pressing_clear")) {
                        Text("Clear my pressing")
                    }
                }
            }
        } else {
            val chosenFormats = ReleaseFormats.decodeUser(chosen)
            val formatNotes = FormatNotes.decode(formatNotesText)
            Text(
                text = "Formats my copy is on, if MusicBrainz does not list them",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReleaseFormats.displayOrder.forEach { label ->
                    FilterChip(
                        selected = label in chosenFormats,
                        onClick = {
                            chosen = ReleaseFormats.encodeUser(if (label in chosenFormats) chosenFormats - label else chosenFormats + label)
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("my_copy_format_$label")
                    )
                }
            }
            // Each format of the copy has a note of its own, such as where it came from or what shape it is in.
            chosenFormats.forEach { label ->
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = formatNotes[label].orEmpty(),
                    onValueChange = {
                        formatNotesText = FormatNotes.encode(
                            formatNotes + (label to it.take(MAX_NOTE_LENGTH)), ReleaseFormats.displayOrder
                        ).orEmpty()
                    },
                    label = { Text("Note for $label, such as where it came from") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("my_copy_format_note_$label")
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(MAX_NOTE_LENGTH) },
                label = { Text("Note, such as the country or language") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("my_copy_note")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSave(chosenFormats, note, formatNotes.filterKeys { it in chosenFormats })
                        editing = false
                    },
                    modifier = Modifier.testTag("my_copy_save")
                ) { Text("Save") }
                TextButton(onClick = { editing = false }, modifier = Modifier.testTag("my_copy_cancel")) { Text("Cancel") }
            }
        }
    }
}
