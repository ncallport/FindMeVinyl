package com.nickallport.findmevinyl.ui.owned

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.owned.OwnedAlbumUiModel
import com.nickallport.findmevinyl.ui.brand.RecordSpinner
import kotlinx.coroutines.launch

/** Loads the Owned list each time the page opens, with a spinner meanwhile, then shows [OwnedListScreen]. */
@Composable
fun OwnedListRoute(
    load: suspend () -> List<OwnedAlbumUiModel>,
    exportText: suspend () -> String,
    onShareText: (String) -> Unit,
    onOpenAlbum: (OwnedAlbumUiModel) -> Unit,
    modifier: Modifier = Modifier,
    loadCover: suspend (String) -> Bitmap? = { null }
) {
    var owned by remember { mutableStateOf<List<OwnedAlbumUiModel>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { owned = load() }

    val loaded = owned
    if (loaded == null) {
        RecordSpinner(modifier = modifier.fillMaxWidth().padding(16.dp).testTag("loading_indicator"))
    } else {
        OwnedListScreen(
            owned = loaded,
            onShare = { coroutineScope.launch { onShareText(exportText()) } },
            onOpenAlbum = onOpenAlbum,
            modifier = modifier,
            loadCover = loadCover
        )
    }
}
