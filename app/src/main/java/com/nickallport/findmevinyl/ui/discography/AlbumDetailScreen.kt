package com.nickallport.findmevinyl.ui.discography

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.nickallport.findmevinyl.discography.TrackSection
import com.nickallport.findmevinyl.discography.TrackSections
import com.nickallport.findmevinyl.ui.common.SquareImage

sealed class AlbumDetailState {
    object Loading : AlbumDetailState()
    object Failed : AlbumDetailState()
    data class Loaded(val details: AlbumDetails) : AlbumDetailState()
}

/**
 * The page for one album: cover, title, artist, owned and want buttons, then what MusicBrainz knows
 * about the album's earliest vinyl pressing. The title and artist come from the caller so they show
 * while the details load.
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
    modifier: Modifier = Modifier
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
        }

        Spacer(Modifier.height(16.dp))
        when (state) {
            AlbumDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.testTag("detail_loading"))
            AlbumDetailState.Failed -> {
                Text("Couldn't load the details", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry, modifier = Modifier.testTag("detail_retry")) {
                    Text("Try again")
                }
            }
            is AlbumDetailState.Loaded -> LoadedDetails(state.details)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadedDetails(details: AlbumDetails) {
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
