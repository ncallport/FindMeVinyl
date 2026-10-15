package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The artist's name, and under it a line of buttons: Artists, which goes to the favourite artists
 * (when [onOpenArtists] is given), and Save or Saved.
 */
@Composable
fun DiscographyHeader(
    artistName: String,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenArtists: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = artistName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onOpenArtists != null) {
                OutlinedButton(onClick = onOpenArtists, modifier = Modifier.testTag("artists_button")) {
                    Text("Artists")
                }
            }
            // Saved is the quieter style, since there is nothing left to do.
            val colors = if (isFavourite) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
            Button(
                onClick = onToggleFavourite,
                colors = colors,
                modifier = Modifier.testTag("favourite_toggle")
            ) {
                Text(if (isFavourite) "Saved" else "Save")
            }
        }
    }
}
