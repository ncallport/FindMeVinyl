package com.nickallport.findmevinyl.ui.discography

import com.nickallport.findmevinyl.discography.PressingSummary
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.discography.AlbumDetails
import com.nickallport.findmevinyl.discography.CountryNames
import com.nickallport.findmevinyl.discography.Pressing
import com.nickallport.findmevinyl.discography.TrackSection
import com.nickallport.findmevinyl.discography.TrackSections
import com.nickallport.findmevinyl.ui.common.SquareImage

sealed class AlbumDetailState {
    object Loading : AlbumDetailState()
    object Failed : AlbumDetailState()
    /**
     * [switchingTo] is the release being loaded after the user chose another pressing, and
     * [switchFailed] says the last such choice could not be loaded. The details stay as they were.
     */
    data class Loaded(
        val details: AlbumDetails,
        val switchingTo: String? = null,
        val switchFailed: Boolean = false
    ) : AlbumDetailState()
}

/**
 * The page for one album: cover, title, artist, owned and want buttons, then what MusicBrainz knows
 * about one pressing of it (the earliest vinyl one to start with), the album's rating and a list of
 * its other pressings. Tapping one of them shows that pressing instead. The title and artist come
 * from the caller so they show while the details load.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    title: String,
    artistName: String,
    album: AlbumUiModel?,
    state: AlbumDetailState,
    onRetry: () -> Unit,
    onToggleOwned: () -> Unit,
    onToggleWanted: () -> Unit,
    loadCover: suspend (String) -> Bitmap?,
    modifier: Modifier = Modifier,
    onSelectPressing: (String) -> Unit = {},
    /** Saves the formats and the note the user gives for their own copy of an album they own. */
    onSaveCopy: (formats: List<String>, note: String) -> Unit = { _, _ -> },
    /** Chooses a pressing as the user's own copy, or takes the choice away with null. */
    onMarkMyPressing: (Pressing?) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SquareImage(
            key = albumId,
            load = loadCover,
            imageTag = "detail_cover_image",
            placeholderTag = "detail_cover_placeholder",
            size = 220.dp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("detail_title")
        )
        Text(
            text = artistName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.testTag("detail_artist")
        )

        if (album != null) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (album.owned) {
                    Button(onClick = onToggleOwned, modifier = Modifier.testTag("detail_owned_toggle")) {
                        Text("Owned")
                    }
                } else {
                    OutlinedButton(onClick = onToggleOwned, modifier = Modifier.testTag("detail_owned_toggle")) {
                        Text("Mark as owned")
                    }
                    OutlinedButton(onClick = onToggleWanted, modifier = Modifier.testTag("detail_want_toggle")) {
                        Text(if (album.wanted) "Wanted" else "+ Want")
                    }
                }
            }
            if (album.owned) {
                Spacer(Modifier.height(16.dp))
                val details = (state as? AlbumDetailState.Loaded)?.details
                MyCopySection(
                    album = album,
                    onSave = onSaveCopy,
                    shownPressing = details?.pressings?.firstOrNull { it.releaseId == details.pressingId },
                    onMarkPressing = onMarkMyPressing
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        when (state) {
            AlbumDetailState.Loading -> RecordSpinner(modifier = Modifier.testTag("detail_loading"))
            AlbumDetailState.Failed -> {
                Text("Couldn't load the details", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry, modifier = Modifier.testTag("detail_retry")) {
                    Text("Try again")
                }
            }
            is AlbumDetailState.Loaded -> LoadedDetails(state, onSelectPressing, myPressingId = album?.takeIf { it.owned }?.myPressingId)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadedDetails(state: AlbumDetailState.Loaded, onSelectPressing: (String) -> Unit, myPressingId: String? = null) {
    val details = state.details
    if (!details.hasContent) {
        Text(
            text = "MusicBrainz has no details for this album yet",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("detail_empty")
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        details.firstReleaseDate?.let {
            Text(
                text = "First released $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        details.rating?.let {
            Text(
                text = "Rating ${it.text()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("detail_rating")
            )
            Spacer(Modifier.height(8.dp))
        }

        val label = listOfNotNull(details.label, details.catalogNumber).joinToString(", ")
        InfoRow("Released", details.releaseDate)
        InfoRow("Country", details.country?.let { CountryNames.of(it) })
        InfoRow("Label", label.ifEmpty { null })
        InfoRow("Format", details.format)

        if (details.genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                details.genres.forEach { genre ->
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .testTag("genre_chip")
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        val multipleDiscs = details.discs.size > 1
        details.discs.forEachIndexed { index, disc ->
            TrackSections.of(disc, multipleDiscs, index + 1).forEach { section ->
                SectionHeader(section)
                section.tracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .testTag("track_row")
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track.number.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp)
                        )
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        track.lengthText()?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("track_length")
                            )
                        }
                    }
                }
            }
        }

        // With one pressing there is nothing to choose between.
        if (details.pressings.size > 1) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Pressings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Tap one to see its tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.switchFailed) {
                Text(
                    text = "Couldn't load that pressing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp).testTag("pressing_failed")
                )
            }
            Spacer(Modifier.height(8.dp))
            details.pressings.forEach { pressing ->
                PressingRow(
                    pressing = pressing,
                    shown = pressing.releaseId == details.pressingId,
                    mine = pressing.releaseId == myPressingId,
                    loading = pressing.releaseId == state.switchingTo,
                    canChoose = state.switchingTo == null,
                    onChoose = { onSelectPressing(pressing.releaseId) }
                )
            }
        }
    }
}

@Composable
private fun PressingRow(pressing: Pressing, shown: Boolean, mine: Boolean, loading: Boolean, canChoose: Boolean, onChoose: () -> Unit) {
    val container = if (shown) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val content = if (shown) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val id = pressing.releaseId

    val title = PressingSummary.title(pressing)
    val detail = PressingSummary.detail(pressing)

    Row(
        modifier = Modifier
            .testTag("pressing_row_$id")
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(enabled = !shown && canChoose, onClick = onChoose)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).testTag("pressing_row")) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = content,
                modifier = Modifier.testTag("pressing_title_$id")
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content,
                    modifier = Modifier.testTag("pressing_detail_$id")
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (loading) {
                RecordSpinner(modifier = Modifier.testTag("pressing_loading_$id"), size = 20.dp)
            } else if (shown) {
                Text(
                    text = "Showing",
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    modifier = Modifier.testTag("pressing_shown_$id")
                )
            }
            // The pressing the user chose as their own copy.
            if (mine) {
                Text(
                    text = "Mine",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (shown) content else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("pressing_mine_$id")
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(section: TrackSection) {
    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (section.title != null) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .testTag("track_section")
                    .weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        section.totalText()?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("section_total")
            )
        }
    }
}

@Composable
private fun InfoRow(name: String, value: String?) {
    if (value == null) return
    Row(
        modifier = Modifier
            .testTag("info_$name")
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
