package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.discography.ReleaseFormats
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
 * tried again the next time the row scrolls into view, not in a loop. [userFormats] are added to what is found. With [checkingWhenUnknown],
 * an album that has no stored formats says "Checking formats…" while its lookup runs.
 */
@Composable
fun FormatLabels(
    albumId: String,
    formats: List<String>?,
    stale: Boolean,
    loadFormats: suspend (String) -> List<String>?,
    modifier: Modifier = Modifier,
    checkingWhenUnknown: Boolean = false,
    /** Formats the user added for their own copy. They are shown with the others, and are not looked up. */
    userFormats: List<String> = emptyList()
) {
    val currentLoadFormats by rememberUpdatedState(loadFormats)
    var found by remember(albumId) { mutableStateOf<List<String>?>(null) }
    var attempted by remember(albumId) { mutableStateOf(false) }
    val needsLookup = formats == null || stale

    LaunchedEffect(albumId, needsLookup) {
        if (needsLookup) {
            found = currentLoadFormats(albumId)
            attempted = true
        }
    }

    val shown = ((found ?: formats).orEmpty() + userFormats).distinct()
        .sortedBy { ReleaseFormats.displayOrder.indexOf(it) }
    if (shown.isNotEmpty()) {
        Text(
            text = shown.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("format_labels")
        )
    } else if (checkingWhenUnknown && formats == null && !attempted) {
        // Only while the lookup of an album with no stored formats is running.
        Text(
            text = "Checking formats…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("checking_formats")
        )
    }
}
