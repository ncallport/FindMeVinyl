package com.nickallport.findmevinyl.discography

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import com.nickallport.findmevinyl.ui.discography.AddAlbumScreen
import kotlinx.coroutines.launch

/**
 * The page for adding an album MusicBrainz does not list, or, with [editingAlbumId], for changing one added earlier
 * ([loadDraft] fills the form, a spinner shows meanwhile). [save] keeps the draft and says whether it could, [delete]
 * removes the album. [onSaved] and [onDeleted] tell the page what happened, so it can move on.
 */
@Composable
fun AddAlbumRoute(
    artistName: String,
    editingAlbumId: String?,
    loadDraft: suspend (String) -> AlbumDraft?,
    save: suspend (AlbumDraft) -> Boolean,
    delete: suspend () -> Unit,
    showHiddenNote: Boolean,
    onOpenSettings: () -> Unit,
    onSaved: (AlbumDraft) -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Null while an album being changed is loaded.
    var loaded by remember(editingAlbumId) { mutableStateOf(editingAlbumId == null) }
    var draft by remember(editingAlbumId) { mutableStateOf<AlbumDraft?>(null) }
    var error by remember(editingAlbumId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editingAlbumId) {
        if (editingAlbumId != null) {
            draft = loadDraft(editingAlbumId)
            loaded = true
        }
    }

    when {
        !loaded -> RecordSpinner(modifier = modifier.fillMaxWidth().padding(16.dp).testTag("loading_indicator"))
        editingAlbumId != null && draft == null -> Text(
            text = "That album is not there any more.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(16.dp).testTag("add_album_missing")
        )
        else -> AddAlbumScreen(
            artistName = artistName,
            editing = editingAlbumId != null,
            initial = draft,
            showHiddenNote = showHiddenNote,
            error = error,
            onSave = { new ->
                scope.launch {
                    if (save(new)) onSaved(new) else error = "Could not save that. Check the title and the year."
                }
            },
            onCancel = onCancel,
            onDelete = { scope.launch { delete(); onDeleted() } },
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
    }
}
