package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import kotlinx.coroutines.delay

/** An artist the user chose for a track. [id] is a MusicBrainz id, or null for a name typed by hand. */
data class PickedArtist(val id: String?, val name: String)

/** How long typing must pause before the artists are looked up. */
const val TRACK_ARTIST_WAIT_MILLIS = 700L

/**
 * Chooses the artist of one track: type a name to see artists (the user's favourites, then MusicBrainz's), tap one,
 * or use what was typed as it is when MusicBrainz does not have the artist. [onReset] puts MusicBrainz's own credit back
 * and is only offered when there is something to put back.
 */
@Composable
fun TrackArtistDialog(
    trackTitle: String,
    suggest: suspend (String) -> List<ArtistSuggestion>,
    onPick: (PickedArtist) -> Unit,
    onCancel: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<ArtistSuggestion>>(emptyList()) }

    LaunchedEffect(text) {
        if (text.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(TRACK_ARTIST_WAIT_MILLIS)
        suggestions = suggest(text)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Artist for $trackTitle") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("track_artist_search")
                )
                suggestions.forEachIndexed { index, suggestion ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_artist_suggestion_$index")
                            .clickable { onPick(PickedArtist(suggestion.id, suggestion.name)) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = suggestion.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = suggestion.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (text.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_artist_typed")
                            .clickable { onPick(PickedArtist(null, text.trim())) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = "Use \"${text.trim()}\" as typed", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "For an artist MusicBrainz does not have. The album will not be on an artist's page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (onReset != null) {
                TextButton(onClick = onReset, modifier = Modifier.testTag("track_artist_reset")) { Text("Use MusicBrainz's artist") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("track_artist_cancel")) { Text("Cancel") }
        }
    )
}
