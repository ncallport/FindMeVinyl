package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class AlbumUiModel(
    val id: String,
    val title: String,
    val owned: Boolean,
    val wanted: Boolean,
    /** Labels such as LP and CD. Null: not looked up yet. Empty: looked up, none known. */
    val formats: List<String>? = null,
    /** The stored formats are old enough to look up again. They are still shown meanwhile. */
    val formatsStale: Boolean = false
)

@Composable
fun AlbumRow(
    album: AlbumUiModel,
    onToggleOwned: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleWanted: () -> Unit = {},
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    checkingFormats: Boolean = false
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(albumId = album.id, loadCover = loadCover)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = album.title)
            FormatLabels(
                albumId = album.id,
                formats = album.formats,
                stale = album.formatsStale,
                loadFormats = loadFormats,
                checkingWhenUnknown = checkingFormats
            )
            if (album.owned) {
                Text(text = "Owned")
            }
            if (album.wanted) {
                Text(text = "Want")
            }
        }
        if (!album.owned) {
            Text(
                text = "+ Want",
                modifier = Modifier
                    .testTag("wanted_toggle")
                    .clickable { onToggleWanted() }
                    .padding(8.dp)
            )
        }
    }
}