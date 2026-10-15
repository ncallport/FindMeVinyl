package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel
import kotlinx.coroutines.launch

@Composable
fun WantListRoute(
    load: suspend () -> List<WantedAlbumUiModel>,
    exportText: suspend () -> String,
    onShareText: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMoveUp: suspend (String) -> Unit = {},
    onMoveDown: suspend (String) -> Unit = {}
) {
    var wanted by remember { mutableStateOf<List<WantedAlbumUiModel>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        wanted = load()
    }

    val loaded = wanted
    if (loaded == null) {
        CircularProgressIndicator(modifier = modifier.testTag("loading_indicator"))
    } else {
        WantListScreen(
            wanted = loaded,
            onShare = {
                coroutineScope.launch {
                    onShareText(exportText())
                }
            },
            modifier = modifier,
            // After a move the list is read again, so it shows what is stored.
            onMoveUp = { id -> coroutineScope.launch { onMoveUp(id); wanted = load() } },
            onMoveDown = { id -> coroutineScope.launch { onMoveDown(id); wanted = load() } }
        )
    }
}