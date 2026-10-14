package com.nickallport.findmevinyl.ui.artist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** The credit Wikimedia Commons asks for. Show it while at least one artist picture is on screen. */
@Composable
fun PhotoCredit(modifier: Modifier = Modifier) {
    Text(
        text = "Artist photos: Wikimedia Commons",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("photo_credit")
    )
}
