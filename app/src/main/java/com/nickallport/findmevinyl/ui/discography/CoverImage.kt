package com.nickallport.findmevinyl.ui.discography

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.R

/**
 * The album cover, or the record placeholder until the cover has loaded and when there is none.
 * It is decorative, because the album title is shown next to it.
 */
@Composable
fun CoverImage(
    albumId: String,
    loadCover: suspend (String) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val currentLoadCover by rememberUpdatedState(loadCover)
    var cover by remember(albumId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(albumId) {
        cover = currentLoadCover(albumId)
    }

    val shaped = modifier
        .size(56.dp)
        .clip(RoundedCornerShape(4.dp))

    val loaded = cover
    if (loaded != null) {
        Image(
            bitmap = remember(loaded) { loaded.asImageBitmap() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = shaped.testTag("cover_image")
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_cover_placeholder),
            contentDescription = null,
            modifier = shaped.testTag("cover_placeholder")
        )
    }
}
