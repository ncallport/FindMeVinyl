package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun DiscographyHeader(
    artistName: String,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = artistName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
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