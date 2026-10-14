package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.ui.common.SquareImage

/** The album cover, or the record placeholder until the cover has loaded and when there is none. */
@Composable
fun CoverImage(
    albumId: String,
    loadCover: suspend (String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    SquareImage(
        key = albumId,
        load = loadCover,
        imageTag = "cover_image",
        placeholderTag = "cover_placeholder",
        modifier = modifier
    )
}
