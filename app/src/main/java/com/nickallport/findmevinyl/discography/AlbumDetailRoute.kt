package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.ui.discography.AlbumDetailScreen
import com.nickallport.findmevinyl.ui.discography.PickedArtist
import com.nickallport.findmevinyl.ui.discography.AlbumDetailState
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.launch

/**
 * [loadDetails] gives the details of the album's pressing, or null when they could not be had. Try
 * again asks for them again. [loadAlbum] reads the saved owned and want flags, and is asked again
 * after each tap, so the buttons show what is stored. A tap does not reload the details.
 *
 * Choosing another pressing asks [loadPressing] for the album's details with that pressing shown. The
 * page stays as it is meanwhile, one choice at a time, and a pressing that cannot be loaded says so.
 */
@Composable
fun AlbumDetailRoute(
    albumId: String,
    title: String,
    artistName: String,
    loadDetails: suspend (String) -> AlbumDetails?,
    loadAlbum: suspend (String) -> AlbumUiModel?,
    onToggleOwned: suspend (albumId: String, currentlyOwned: Boolean) -> Unit,
    onToggleWanted: suspend (albumId: String, currentlyWanted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    loadCover: suspend (String) -> Bitmap? = { null },
    loadPressing: suspend (albumId: String, releaseId: String) -> AlbumDetails? = { _, _ -> null },
    /** Keeps the copies the user owns. The album is read again after it. */
    saveCopies: suspend (albumId: String, copies: List<AlbumCopy>) -> Unit = { _, _ -> },
    /** Keeps the pressing the user chose as their copy, or takes it away with null. The album is read again after it. */
    saveMyPressing: suspend (albumId: String, pressing: Pressing?) -> Unit = { _, _ -> },
    /** Keeps the format a wanted album is wanted on, or null for any. The album is read again after it. */
    saveWantFormat: suspend (albumId: String, format: String?) -> Unit = { _, _ -> },
    /** Opens the artist's page. When null the artist's name is plain text. */
    onOpenArtist: (() -> Unit)? = null,
    /** Opens the page for changing an album added by hand. Only given for one. */
    onEditAlbum: (() -> Unit)? = null,
    /** Asks the user for a cover image and keeps it, saying whether it did. Only given for an album added by hand. */
    chooseCover: (suspend (albumId: String) -> Boolean)? = null,
    /** Takes the cover image away. */
    removeCover: (suspend (albumId: String) -> Unit)? = null,
    /** The artists the user chose for tracks, shown over MusicBrainz's credits. */
    loadTrackCredits: suspend (albumId: String) -> List<TrackCredit> = { emptyList() },
    /** Finds artists for the track artist chooser. Without it the tracks' artists cannot be changed. */
    suggestTrackArtists: (suspend (String) -> List<ArtistSuggestion>)? = null,
    /** Keeps the artist chosen for a track (disc, place on it, title), or with null takes it off. Credits are read again after it. */
    setTrackArtist: suspend (albumId: String, discIndex: Int, trackIndex: Int, title: String, artist: PickedArtist?) -> Unit = { _, _, _, _, _ -> }
) {
    var state by remember(albumId) { mutableStateOf<AlbumDetailState>(AlbumDetailState.Loading) }
    var album by remember(albumId) { mutableStateOf<AlbumUiModel?>(null) }
    // Counts the taps on Try again. Each one starts the load again.
    var attempt by remember(albumId) { mutableStateOf(0) }
    var switchingTo by remember(albumId) { mutableStateOf<String?>(null) }
    var switchFailed by remember(albumId) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(albumId, attempt) {
        switchingTo = null
        switchFailed = false
        // An album added by hand is not on MusicBrainz, so nothing is asked.
        if (CustomAlbums.isCustom(albumId)) {
            state = AlbumDetailState.AddedByHand
            return@LaunchedEffect
        }
        state = AlbumDetailState.Loading
        state = loadDetails(albumId)?.let { AlbumDetailState.Loaded(it) } ?: AlbumDetailState.Failed
    }

    LaunchedEffect(albumId) {
        album = loadAlbum(albumId)
    }

    var credits by remember(albumId) { mutableStateOf<List<TrackCredit>>(emptyList()) }
    LaunchedEffect(albumId) {
        credits = loadTrackCredits(albumId)
    }

    // The page opens on the pressing the user chose as theirs, if it is one of those listed and not the first.
    // It is tried once for each load, so a pressing the user then looks at is not switched back, and a pressing
    // that cannot be loaded leaves the first one showing without a complaint.
    var triedMine by remember(albumId, attempt) { mutableStateOf<String?>(null) }
    LaunchedEffect(state, album) {
        val loaded = (state as? AlbumDetailState.Loaded)?.details ?: return@LaunchedEffect
        val mine = album?.takeIf { it.owned }?.myPressingId ?: return@LaunchedEffect
        if (triedMine == mine || switchingTo != null) return@LaunchedEffect
        if (loaded.pressingId == mine || loaded.pressings.none { it.releaseId == mine }) return@LaunchedEffect
        triedMine = mine
        switchingTo = mine
        loadPressing(albumId, mine)?.let { state = AlbumDetailState.Loaded(it) }
        switchingTo = null
    }

    // The artists the user chose go over MusicBrainz's on whichever pressing is showing.
    val loaded = state as? AlbumDetailState.Loaded
    val shownState = loaded?.copy(
        details = TrackCredits.apply(loaded.details, credits), switchingTo = switchingTo, switchFailed = switchFailed
    ) ?: state

    AlbumDetailScreen(
        albumId = albumId,
        title = title,
        artistName = artistName,
        album = album,
        state = shownState,
        onRetry = { attempt++ },
        onSelectPressing = { releaseId ->
            if (switchingTo == null) {
                switchFailed = false
                switchingTo = releaseId
                coroutineScope.launch {
                    val chosen = loadPressing(albumId, releaseId)
                    if (chosen != null) state = AlbumDetailState.Loaded(chosen) else switchFailed = true
                    switchingTo = null
                }
            }
        },
        onToggleOwned = {
            val current = album ?: return@AlbumDetailScreen
            coroutineScope.launch {
                onToggleOwned(albumId, current.owned)
                album = loadAlbum(albumId)
            }
        },
        onToggleWanted = {
            val current = album ?: return@AlbumDetailScreen
            coroutineScope.launch {
                onToggleWanted(albumId, current.wanted)
                album = loadAlbum(albumId)
            }
        },
        onSaveCopy = { copies ->
            coroutineScope.launch {
                saveCopies(albumId, copies)
                album = loadAlbum(albumId)
            }
        },
        suggestTrackArtists = suggestTrackArtists,
        onSetTrackArtist = { discIndex, trackIndex, trackTitle, artist ->
            coroutineScope.launch {
                setTrackArtist(albumId, discIndex, trackIndex, trackTitle, artist)
                credits = loadTrackCredits(albumId)
            }
        },
        onOpenArtist = onOpenArtist,
        onEditAlbum = onEditAlbum,
        onChooseCover = chooseCover?.let { choose -> { coroutineScope.launch { if (choose(albumId)) album = loadAlbum(albumId) } } },
        onRemoveCover = removeCover?.let { remove -> { coroutineScope.launch { remove(albumId); album = loadAlbum(albumId) } } },
        onSetWantFormat = { format ->
            coroutineScope.launch {
                saveWantFormat(albumId, format)
                album = loadAlbum(albumId)
            }
        },
        onMarkMyPressing = { pressing ->
            coroutineScope.launch {
                saveMyPressing(albumId, pressing)
                album = loadAlbum(albumId)
            }
        },
        loadCover = loadCover,
        modifier = modifier
    )
}
