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
import com.nickallport.findmevinyl.discography.MAX_COPIES
import com.nickallport.findmevinyl.discography.MAX_NOTE_LENGTH
import com.nickallport.findmevinyl.discography.ReleaseFormats

/**
 * The copies of an album the user owns. Each copy has a format and a note of its own, such as "part of Mum's collection"
 * or "green cover", and there can be several, even on the same format. [onSave] gets the copies in the order listed.
 *
 * The page's own pressing can be chosen as the copy here too ("This is my pressing"), and the choice cleared.
 *
 * Whether the editor is open, and the copies typed in it, are kept over a rotation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyCopySection(
    album: AlbumUiModel,
    onSave: (copies: List<AlbumCopy>) -> Unit,
    modifier: Modifier = Modifier,
    /** The pressing the page is showing, which "This is my pressing" would choose. Null when it is not known. */
    shownPressing: Pressing? = null,
    /** Chooses [Pressing] as the user's own copy, or takes the choice away with null. */
    onMarkPressing: (Pressing?) -> Unit = {}
) {
    var editing by rememberSaveable(album.id) { mutableStateOf(false) }
    // The copies being edited, as text (see [CopyDrafts]), which a rotation can keep.
    var drafts by rememberSaveable(album.id) { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth().testTag("my_copy_section")) {
        Text(text = "My copies", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        if (!editing) {
            val summary = album.copies.joinToString("\n") { if (it.note.isNullOrBlank()) it.format else "${it.format}: ${it.note}" }
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
                        // With no copies yet the editor opens with one ready to fill in, its formats to choose from.
                        drafts = CopyDrafts.encode(album.copies.ifEmpty { listOf(AlbumCopy(ReleaseFormats.LP)) })
                        editing = true
                    },
                    modifier = Modifier.testTag("my_copy_edit")
                ) {
                    Text(if (summary.isEmpty()) "Add a copy" else "Edit my copies")
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
            val copies = CopyDrafts.decode(drafts)
            copies.forEachIndexed { index, copy ->
                Text(
                    text = "Copy ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReleaseFormats.displayOrder.forEach { label ->
                        FilterChip(
                            selected = copy.format == label,
                            onClick = { drafts = CopyDrafts.encode(copies.mapIndexed { i, c -> if (i == index) c.copy(format = label) else c }) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("my_copy_${index}_format_$label")
                        )
                    }
                }
                OutlinedTextField(
                    value = copy.note.orEmpty(),
                    onValueChange = { text ->
                        drafts = CopyDrafts.encode(copies.mapIndexed { i, c -> if (i == index) c.copy(note = text.take(MAX_NOTE_LENGTH)) else c })
                    },
                    label = { Text("Note, such as where it came from or the colour of the sleeve") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("my_copy_${index}_note")
                )
                TextButton(
                    onClick = { drafts = CopyDrafts.encode(copies.filterIndexed { i, _ -> i != index }) },
                    modifier = Modifier.testTag("my_copy_${index}_remove")
                ) { Text("Remove this copy") }
                Spacer(Modifier.height(4.dp))
            }
            if (copies.size < MAX_COPIES) {
                OutlinedButton(
                    onClick = { drafts = CopyDrafts.encode(copies + AlbumCopy(ReleaseFormats.LP)) },
                    modifier = Modifier.testTag("my_copy_add")
                ) { Text("Add a copy") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSave(copies)
                        editing = false
                    },
                    modifier = Modifier.testTag("my_copy_save")
                ) { Text("Save") }
                TextButton(onClick = { editing = false }, modifier = Modifier.testTag("my_copy_cancel")) { Text("Cancel") }
            }
        }
    }
}

/** The copies being edited as one text, so a rotation can keep them: a copy is `format`, a control character, `note`. */
internal object CopyDrafts {
    private const val FIELD = '\u0001'
    private const val COPY = '\u0002'

    fun encode(copies: List<AlbumCopy>): String =
        copies.joinToString(COPY.toString()) { "${it.format}$FIELD${it.note.orEmpty().filterNot { c -> c == FIELD || c == COPY }}" }

    fun decode(text: String): List<AlbumCopy> =
        if (text.isEmpty()) emptyList()
        else text.split(COPY).map { part ->
            val at = part.indexOf(FIELD)
            AlbumCopy(if (at < 0) part else part.substring(0, at), if (at < 0) null else part.substring(at + 1).ifEmpty { null })
        }
}
