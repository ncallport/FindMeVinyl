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
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.favourites.GroupMember
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import com.nickallport.findmevinyl.ui.discography.AddAlbumScreen
import kotlinx.coroutines.launch

/**
 * The page for adding an album MusicBrainz does not list, or, with [editingAlbumId], for changing one added earlier
 * ([loadDraft] fills the form, a spinner shows meanwhile). [save] keeps the draft and gives back the album's id, or null
 * when it could not, and [delete] removes the album. [onSaved] and [onDeleted] tell the page what happened, so it can move on.
 */
@Composable
fun AddAlbumRoute(
    artistName: String,
    editingAlbumId: String?,
    loadDraft: suspend (String) -> AlbumDraft?,
    save: suspend (AlbumDraft) -> String?,
    delete: suspend () -> Unit,
    showHiddenNote: Boolean,
    onOpenSettings: () -> Unit,
    onSaved: (AlbumDraft, albumId: String) -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** The artists of the group whose page this is, to choose which one the album is by. Empty for an artist. */
    loadMembers: suspend () -> List<GroupMember> = { emptyList() },
    /** The kind the form starts on for a new album: live, or soundtrack on the Soundtracks page. */
    defaultCategory: DiscographyCategory = DiscographyCategory.LIVE_ALBUM,
    /** Finds artists for the tracks. Without it the tracks cannot be given an artist. */
    suggestTrackArtists: (suspend (String) -> List<ArtistSuggestion>)? = null
) {
    // Null while an album being changed, or the artists of a group, are loaded.
    var loaded by remember(editingAlbumId) { mutableStateOf(false) }
    var members by remember(editingAlbumId) { mutableStateOf(emptyList<GroupMember>()) }
    var draft by remember(editingAlbumId) { mutableStateOf<AlbumDraft?>(null) }
    var error by remember(editingAlbumId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editingAlbumId) {
        members = loadMembers()
        if (editingAlbumId != null) draft = loadDraft(editingAlbumId)
        loaded = true
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
            members = members,
            defaultCategory = defaultCategory,
            suggestTrackArtists = suggestTrackArtists,
            onSave = { new ->
                scope.launch {
                    val id = save(new)
                    if (id != null) onSaved(new, id) else error = "Could not save that. Check the title and the year."
                }
            },
            onCancel = onCancel,
            onDelete = { scope.launch { delete(); onDeleted() } },
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
    }
}
