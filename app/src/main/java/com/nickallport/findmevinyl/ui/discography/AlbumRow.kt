package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.nickallport.findmevinyl.ui.common.ListCard
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
    onOpen: () -> Unit = {},
    onToggleWanted: () -> Unit = {},
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    checkingFormats: Boolean = false
) {
    // Owned albums are teal and wanted ones are orange, so a list can be scanned for either.
    val backgroundColor = when {
        album.owned -> MaterialTheme.colorScheme.primaryContainer
        album.wanted -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    ListCard(
        modifier = modifier.testTag("album_row"),
        onClick = onOpen,
        container = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverImage(albumId = album.id, loadCover = loadCover)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = album.title, style = MaterialTheme.typography.titleSmall)
                FormatLabels(
                    albumId = album.id,
                    formats = album.formats,
                    stale = album.formatsStale,
                    loadFormats = loadFormats,
                    checkingWhenUnknown = checkingFormats
                )
                // An owned album is never wanted. Rows saved before that rule can have both flags.
                if (album.wanted && !album.owned) {
                    Text(
                        text = "Wanted",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (album.owned) {
                    Pill(
                        text = "Owned",
                        tag = "owned_toggle",
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        onClick = onToggleOwned
                    )
                } else {
                    // On the orange card of a wanted album the pill would vanish if it were orange too.
                    Pill(
                        text = "+ Owned",
                        tag = "owned_toggle",
                        container = if (album.wanted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer,
                        content = if (album.wanted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onToggleOwned
                    )
                    // Once it is wanted there is nothing more to want. It comes off the list on the Wants page.
                    if (!album.wanted) {
                        Pill(
                            text = "+ Want",
                            tag = "wanted_toggle",
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onToggleWanted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, tag: String, container: Color, content: Color, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}