package com.nickallport.findmevinyl.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.brand.Glyphs

/**
 * Up and down arrows for a row in a list the user orders by hand. [name] says what is being moved,
 * for screen readers. An arrow is off when the row is already at that end of its list.
 */
@Composable
fun MoveButtons(
    name: String,
    upTag: String,
    downTag: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.testTag(upTag).size(40.dp)) {
            Icon(
                Glyphs.ArrowUp,
                contentDescription = "Move $name up",
                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.testTag(downTag).size(40.dp)) {
            Icon(
                Glyphs.ArrowDown,
                contentDescription = "Move $name down",
                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
