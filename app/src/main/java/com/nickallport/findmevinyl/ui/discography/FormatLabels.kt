package com.nickallport.findmevinyl.ui.discography

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * The formats an album came out on, such as "LP · CD · MC". Stored formats show at once. When there
 * are none, or they are stale, the formats are looked up. A failed lookup shows nothing new and is
 * tried again the next time the row scrolls into view, not in a loop.
 */
@Composable
fun FormatLabels(
    albumId: String,
    formats: List<String>?,
    stale: Boolean,
    loadFormats: suspend (String) -> List<String>?,
    modifier: Modifier = Modifier
) {
    val currentLoadFormats by rememberUpdatedState(loadFormats)
    var found by remember(albumId) { mutableStateOf<List<String>?>(null) }
    val needsLookup = formats == null || stale

    LaunchedEffect(albumId, needsLookup) {
        if (needsLookup) found = currentLoadFormats(albumId)
    }

    val shown = found ?: formats
    if (!shown.isNullOrEmpty()) {
        Text(
            text = shown.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("format_labels")
        )
    }
}
