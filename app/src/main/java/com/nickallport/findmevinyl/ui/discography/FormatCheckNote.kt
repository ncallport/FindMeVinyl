package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Where the check of all of an artist's album formats is. */
sealed class FormatCheckState {
    object Idle : FormatCheckState()

    /** [totalPages] is 0 until the first page has arrived. */
    data class Running(val donePages: Int, val totalPages: Int) : FormatCheckState()

    object Failed : FormatCheckState()
}

/** One line under the format chips: how far the check has got, or that it failed, with a button to try again. */
@Composable
fun FormatCheckNote(state: FormatCheckState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        FormatCheckState.Idle -> Unit
        is FormatCheckState.Running -> Text(
            text = if (state.totalPages > 0) "Checking formats… ${state.donePages} of ${state.totalPages}" else "Checking formats…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("format_check_progress")
        )
        FormatCheckState.Failed -> Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("format_check_failed"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Couldn't check all the formats",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry, modifier = Modifier.testTag("format_check_retry")) { Text("Try again") }
        }
    }
}
