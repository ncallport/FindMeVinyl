package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.ui.artist.ArtistImage

/**
 * The artist's name, and under it a line of buttons: Artists, which goes to the favourite artists
 * (when [onOpenArtists] is given), and Save or Saved.
 *
 * With [onChoosePhoto], the artist's picture is shown beside the name, with a caption under it that
 * says Add photo or Change photo, and tapping either asks for a photo from the user. [photoVersion]
 * changes when the photo has, so the picture is loaded again.
 */
@Composable
fun DiscographyHeader(
    artistName: String,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenArtists: (() -> Unit)? = null,
    artistId: String? = null,
    loadArtistImage: suspend (String) -> Bitmap? = { null },
    photoVersion: Int = 0,
    onChoosePhoto: (() -> Unit)? = null,
    /** Opens the page for joining this artist with others into a group. Without it there is no button. */
    onJoinArtists: (() -> Unit)? = null,
    /** The artist is already in a group, so the button edits that group. */
    inGroup: Boolean = false,
    /** False on the page of a group, which is always kept, so there is nothing to save. */
    showFavourite: Boolean = true,
    /** Opens the page for adding an album MusicBrainz does not list. Without it there is no button. */
    onAddAlbum: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (onChoosePhoto != null && artistId != null) {
            PhotoBlock(artistId, loadArtistImage, photoVersion, onChoosePhoto)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenArtists != null) {
                    OutlinedButton(onClick = onOpenArtists, modifier = Modifier.testTag("artists_button")) {
                        Text("Artists")
                    }
                }
                if (showFavourite) {
                    // Saved is the quieter style, since there is nothing left to do.
                    val colors = if (isFavourite) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
                    Button(
                        onClick = onToggleFavourite,
                        colors = colors,
                        modifier = Modifier.testTag("favourite_toggle")
                    ) {
                        Text(if (isFavourite) "Saved" else "Save")
                    }
                }
            }
            if (onJoinArtists != null) {
                OutlinedButton(
                    onClick = onJoinArtists,
                    modifier = Modifier.padding(top = 4.dp).testTag("join_artists_button")
                ) {
                    Text(if (inGroup) "Edit group" else "Join with another artist")
                }
            }
            if (onAddAlbum != null) {
                OutlinedButton(
                    onClick = onAddAlbum,
                    modifier = Modifier.padding(top = 4.dp).testTag("add_album_button")
                ) {
                    Text("Add an album")
                }
            }
        }
    }
}

@Composable
private fun PhotoBlock(
    artistId: String,
    loadArtistImage: suspend (String) -> Bitmap?,
    photoVersion: Int,
    onChoosePhoto: () -> Unit
) {
    // Whether a picture is showing, which decides whether the caption says Add or Change.
    var shown by remember(artistId, photoVersion) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(end = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onChoosePhoto)
            .testTag("change_photo"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The picture is asked for again when the photo changes, because the version is part of the key.
        ArtistImage(
            artistId = "$artistId#$photoVersion",
            loadImage = { loadArtistImage(artistId) },
            size = 80.dp,
            onShownChange = { shown = it }
        )
        Text(
            text = if (shown) "Change photo" else "Add photo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
