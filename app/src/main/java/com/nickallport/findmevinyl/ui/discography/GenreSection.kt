package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.genre.GenreNames
import com.nickallport.findmevinyl.genre.GenreOption
import com.nickallport.findmevinyl.ui.brand.RecordSpinner

/**
 * The genre of an artist or a group: a line saying which is chosen, or else which is [suggested] (the genre with the most
 * votes), and a button that opens the genres to choose from ([loadOptions] fetches them when it opens). [onSet] is told
 * the choice, or null to take the user's choice away, which goes back to the suggestion.
 */
@Composable
fun GenreSection(
    genre: String?,
    loadOptions: suspend () -> List<GenreOption>?,
    onSet: (String?) -> Unit,
    modifier: Modifier = Modifier,
    suggested: String? = null
) {
    var choosing by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Genre: " + when {
                genre != null -> GenreNames.display(genre)
                suggested != null -> GenreNames.display(suggested) + " (suggested)"
                else -> "none chosen"
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).testTag("genre_line")
        )
        TextButton(onClick = { choosing = true }, modifier = Modifier.testTag("genre_change")) {
            Text(if (genre == null && suggested == null) "Choose" else "Change")
        }
    }

    if (choosing) {
        GenreChooser(
            genre = genre,
            suggested = suggested,
            loadOptions = loadOptions,
            onSet = {
                choosing = false
                onSet(it)
            },
            onCancel = { choosing = false }
        )
    }
}

@Composable
private fun GenreChooser(
    genre: String?,
    suggested: String?,
    loadOptions: suspend () -> List<GenreOption>?,
    onSet: (String?) -> Unit,
    onCancel: () -> Unit
) {
    var loaded by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<List<GenreOption>?>(null) }
    var custom by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        options = loadOptions()
        loaded = true
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Choose a genre") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val found = options
                when {
                    !loaded -> RecordSpinner(size = 20.dp, modifier = Modifier.testTag("genre_loading"))
                    found == null -> Text(
                        "Couldn't get genres from MusicBrainz. You can type one below.",
                        modifier = Modifier.testTag("genre_failed")
                    )
                    found.isEmpty() -> Text(
                        "MusicBrainz lists no genres for this one. You can type one below.",
                        modifier = Modifier.testTag("genre_none_listed")
                    )
                    else -> found.forEach { option ->
                        Row(
                            modifier = Modifier
                                .testTag("genre_option_${option.name}")
                                .fillMaxWidth()
                                .clickable { onSet(option.name) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(GenreNames.display(option.name), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = when {
                                    option.name == genre -> "Chosen"
                                    genre == null && option.name == suggested -> "Suggested"
                                    option.votes == 1 -> "1 vote"
                                    else -> "${option.votes} votes"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Or type your own genre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("genre_custom")
                )
                TextButton(
                    onClick = { onSet(custom) },
                    enabled = custom.isNotBlank(),
                    modifier = Modifier.testTag("genre_custom_use")
                ) { Text("Use this genre") }
            }
        },
        confirmButton = {
            if (genre != null) {
                TextButton(onClick = { onSet(null) }, modifier = Modifier.testTag("genre_clear")) {
                    Text(if (suggested != null) "Use the suggested genre" else "Clear genre")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("genre_cancel")) { Text("Cancel") }
        }
    )
}
