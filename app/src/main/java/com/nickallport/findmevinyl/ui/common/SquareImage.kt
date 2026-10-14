package com.nickallport.findmevinyl.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
 * A 56dp rounded square showing the picture for [key], or the record placeholder until it has
 * loaded and when there is none. Used for album covers and artist pictures. It is decorative,
 * because the name is shown next to it.
 *
 * When the key changes, as when a list reuses a row, the old picture is dropped and the new one is
 * loaded. [onShownChange] is told when a picture appears (true) and when it goes away (false), by
 * being replaced or by the row leaving the screen.
 */
@Composable
fun SquareImage(
    key: String,
    load: suspend (String) -> Bitmap?,
    imageTag: String,
    placeholderTag: String,
    modifier: Modifier = Modifier,
    onShownChange: (Boolean) -> Unit = {}
) {
    val currentLoad by rememberUpdatedState(load)
    var picture by remember(key) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(key) {
        picture = currentLoad(key)
    }

    val shaped = modifier
        .size(56.dp)
        .clip(RoundedCornerShape(4.dp))

    val loaded = picture
    if (loaded != null) {
        val currentOnShownChange by rememberUpdatedState(onShownChange)
        DisposableEffect(loaded) {
            currentOnShownChange(true)
            onDispose { currentOnShownChange(false) }
        }
        Image(
            bitmap = remember(loaded) { loaded.asImageBitmap() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = shaped.testTag(imageTag)
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_cover_placeholder),
            contentDescription = null,
            modifier = shaped.testTag(placeholderTag)
        )
    }
}
