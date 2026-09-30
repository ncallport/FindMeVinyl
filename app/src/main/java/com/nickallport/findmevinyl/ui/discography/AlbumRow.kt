package com.nickallport.findmevinyl.ui.discography

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class AlbumUiModel(
    val id: String,
    val title: String,
    val owned: Boolean,
    val wanted: Boolean
)

@Composable
fun AlbumRow(
    album: AlbumUiModel,
    onToggleOwned: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: () -> Unit = {}
) {
    val backgroundColor = if (album.owned) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .testTag("album_row")
            .fillMaxWidth()
            .clickable { onToggleOwned() }
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = album.title)
            if (album.owned) {
                Text(text = "Owned")
            }
            if (album.wanted) {
                Text(text = "Want")
            }
        }
        Text(
            text = "+ Want",
            modifier = Modifier
                .testTag("wanted_toggle")
                .clickable { onToggleWanted() }
                .padding(8.dp)
        )
    }
}