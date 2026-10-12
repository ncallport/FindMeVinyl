package com.nickallport.findmevinyl.ui.wantlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.wantlist.WantedAlbumUiModel

@Composable
fun WantListScreen(
    wanted: List<WantedAlbumUiModel>,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (wanted.isEmpty()) {
        Text(
            text = "Your want list is empty",
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val byArtist = wanted.groupBy { it.artistName }

    Column(modifier = modifier) {
        Button(
            onClick = onShare,
            modifier = Modifier
                .testTag("share_button")
                .padding(16.dp)
        ) {
            Text("Share")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            byArtist.forEach { (artistName, albums) ->
                item {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    )
                }
                items(albums) { album ->
                    Text(
                        text = album.albumTitle,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}