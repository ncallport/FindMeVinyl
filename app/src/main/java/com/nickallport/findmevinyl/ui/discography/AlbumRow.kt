package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.discography.ReleaseStatuses
import androidx.compose.ui.text.style.TextOverflow
import com.nickallport.findmevinyl.discography.ReleaseFormats
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

/** One copy the user owns: a format label such as LP and a note, such as where it came from. */
data class AlbumCopy(val format: String, val note: String? = null)

data class AlbumUiModel(
    val id: String,
    val title: String,
    val owned: Boolean,
    val wanted: Boolean,
    /** Labels such as LP and CD. Null: not looked up yet. Empty: looked up, none known. */
    val formats: List<String>? = null,
    /** The stored formats are old enough to look up again. They are still shown meanwhile. */
    val formatsStale: Boolean = false,
    /** The statuses of the album's releases (see `ReleaseStatuses`). Null: not looked up yet. Empty: none listed. */
    val statuses: List<String>? = null,
    /** Formats the user says their own copy is on, added to [formats]. Labels, in the usual order. */
    val userFormats: List<String> = emptyList(),
    /** The MusicBrainz release the user chose as their copy, and a line that describes it. */
    val myPressingId: String? = null,
    val myPressingText: String? = null,
    /** The artist the album is filed under on MusicBrainz, when the page knows it (a group's members). */
    val artistName: String? = null,
    /** The format the album is wanted on (a label such as LP), or null for any. */
    val wantFormat: String? = null,
    /** The copies the user owns, each with its format and a note, in the order they were listed. */
    val copies: List<AlbumCopy> = emptyList(),
    /** For an album added by hand: the description and the tracklist the user typed. */
    val description: String? = null,
    val tracks: List<String> = emptyList()
) {
    /**
     * Every format the album is known to be on: MusicBrainz's and the user's, once each, in the usual order.
     * Null while neither is known. [formats] alone says whether the album has been looked up.
     */
    /**
     * Whether the album is a bootleg: its releases are known, none is official, and at least one is a bootleg. An
     * album with an official release is not flagged, even when it also has a bootleg.
     */
    val isBootleg: Boolean
        get() = statuses != null && ReleaseStatuses.OFFICIAL !in statuses && ReleaseStatuses.BOOTLEG in statuses

    val shownFormats: List<String>?
        get() = if (formats == null && userFormats.isEmpty()) {
            null
        } else {
            (formats.orEmpty() + userFormats).distinct().sortedBy { ReleaseFormats.displayOrder.indexOf(it) }
        }
}

@Composable
fun AlbumRow(
    album: AlbumUiModel,
    onToggleOwned: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
    onToggleWanted: () -> Unit = {},
    loadCover: suspend (String) -> Bitmap? = { null },
    loadFormats: suspend (String) -> List<String>? = { null },
    checkingFormats: Boolean = false,
    /** Shown under the title when the album has no [AlbumUiModel.artistName] of its own. */
    defaultArtistName: String? = null
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
                (album.artistName ?: defaultArtistName)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("album_artist")
                    )
                }
                FormatLabels(
                    albumId = album.id,
                    formats = album.formats,
                    userFormats = album.userFormats,
                    stale = album.formatsStale,
                    loadFormats = loadFormats,
                    checkingWhenUnknown = checkingFormats
                )
                if (album.isBootleg) {
                    Text(
                        text = "Bootleg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("album_bootleg")
                    )
                }
                // The copies the user owns, one to a line: "Owned on", the format, and the description if there is one.
                album.copies.forEachIndexed { index, copy ->
                    Text(
                        text = "Owned on " + if (copy.note.isNullOrBlank()) copy.format else "${copy.format}: ${copy.note}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("album_copy_$index")
                    )
                }
                // An album can be both: a copy you own and want another of. It keeps the owned colour.
                if (album.wanted) {
                    Text(
                        text = if (album.wantFormat != null) "Wanted on ${album.wantFormat}" else "Wanted",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (album.owned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.testTag("album_wanted_label")
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
                }
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