package com.nickallport.findmevinyl.barcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.ui.barcode.BarcodeMatchUi
import com.nickallport.findmevinyl.ui.barcode.BarcodePageState
import com.nickallport.findmevinyl.ui.barcode.BarcodeScreen
import kotlinx.coroutines.launch

/**
 * Owns the state of the barcode page: the number typed, what the look up found and what was added, and takes the work as
 * suspend functions so tests can pass fakes. The number typed is kept over a rotation, and the result is looked up again
 * by the user when the page is lost.
 */
@Composable
fun BarcodeRoute(
    lookUp: suspend (String) -> BarcodeLookup,
    statusOf: suspend (BarcodeMatch) -> BarcodeStatus,
    addOwned: suspend (BarcodeMatch, format: String, takeOffWant: Boolean) -> BarcodeAdd,
    onOpenAlbum: (BarcodeMatch) -> Unit,
    modifier: Modifier = Modifier,
    /** Takes a photo of a barcode and reads it. Without it there is no Scan button. */
    scan: (suspend () -> ScanResult)? = null
) {
    var barcode by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<BarcodePageState>(BarcodePageState.Idle) }
    var added by remember { mutableStateOf(emptySet<String>()) }
    var busyAlbumId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Looks up a number, which is what the field holds or what a photo gave.
    fun lookUpNumber(number: String) {
        error = null
        state = BarcodePageState.Looking
        scope.launch {
            state = when (val result = lookUp(number)) {
                BarcodeLookup.BadNumber ->
                    BarcodePageState.Message("That is not a barcode number. Check the digits under the barcode.")
                BarcodeLookup.NotFound ->
                    BarcodePageState.Message("MusicBrainz has no album with that barcode. You can add it by hand from the artist's page.")
                is BarcodeLookup.Failed -> BarcodePageState.Message(result.message)
                is BarcodeLookup.Found -> BarcodePageState.Matches(result.matches.map { BarcodeMatchUi(it, statusOf(it)) })
            }
        }
    }

    BarcodeScreen(
        barcode = barcode,
        onBarcodeChange = { barcode = it },
        onLookUp = { lookUpNumber(barcode) },
        onScan = scan?.let { takePhoto ->
            {
                error = null
                scope.launch {
                    when (val result = takePhoto()) {
                        is ScanResult.Number -> {
                            barcode = result.text
                            lookUpNumber(result.text)
                        }
                        ScanResult.Unreadable -> state = BarcodePageState.Message(
                            "Could not read a barcode from that photo. Hold the phone close, with the barcode level and in good light, or type the number."
                        )
                        ScanResult.NoCamera -> state = BarcodePageState.Message("There is no camera app on this phone. Type the number instead.")
                        ScanResult.Cancelled -> Unit
                    }
                }
            }
        },
        state = state,
        added = added,
        busyAlbumId = busyAlbumId,
        error = error,
        onAdd = { match, format, takeOffWant ->
            if (busyAlbumId != null) return@BarcodeScreen
            busyAlbumId = match.releaseGroupId
            error = null
            scope.launch {
                when (val result = addOwned(match, format, takeOffWant)) {
                    is BarcodeAdd.Added -> added = added + match.releaseGroupId
                    is BarcodeAdd.Failed -> error = result.message
                }
                busyAlbumId = null
            }
        },
        onOpenAlbum = onOpenAlbum,
        modifier = modifier
    )
}
