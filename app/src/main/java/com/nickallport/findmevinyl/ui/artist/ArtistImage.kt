package com.nickallport.findmevinyl.ui.artist

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.ui.common.SquareImage

/**
 * The picture of an artist, or the record placeholder until it has loaded and when there is none.
 * [onShownChange] says when a picture appears and when it goes away.
 */
@Composable
fun ArtistImage(
    artistId: String,
    loadImage: suspend (String) -> Bitmap?,
    modifier: Modifier = Modifier,
    onShownChange: (Boolean) -> Unit = {}
) {
    SquareImage(
        key = artistId,
        load = loadImage,
        imageTag = "artist_image",
        placeholderTag = "artist_placeholder",
        modifier = modifier,
        onShownChange = onShownChange
    )
}
