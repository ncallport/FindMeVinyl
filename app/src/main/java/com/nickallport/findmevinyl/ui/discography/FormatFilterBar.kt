package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.discography.FormatOption

/**
 * Chips to show only the albums that came out on one format: All, then one chip for each format
 * found so far with the number of albums that have it. Tapping the chosen chip, or All, clears the
 * choice. While a format is chosen, a line says how many albums have not been looked up yet, since
 * those stay in the list until their lookup tells whether they match.
 */
@Composable
fun FormatFilterBar(
    options: List<FormatOption>,
    selected: String?,
    unchecked: Int,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // With no format known yet there is nothing to filter by.
    if (options.isEmpty() && selected == null) return

    // A chosen format that no album has any more still gets a chip, so the choice can be cleared.
    val chips = if (selected != null && options.none { it.label == selected }) {
        options + FormatOption(selected, 0)
    } else {
        options
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("format_chip_all")
            )
            chips.forEach { option ->
                FilterChip(
                    selected = option.label == selected,
                    onClick = { onSelect(if (option.label == selected) null else option.label) },
                    label = { Text("${option.label} ${option.count}") },
                    modifier = Modifier.testTag("format_chip_${option.label}")
                )
            }
        }
        if (selected != null && unchecked > 0) {
            Text(
                text = "$unchecked not checked yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).testTag("unchecked_note")
            )
        }
    }
}
