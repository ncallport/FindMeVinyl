package com.nickallport.findmevinyl.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// One turn takes just under two seconds, about the pace of a record at 33 rpm seen at a glance.
private const val TURN_MILLIS = 1_800

/**
 * The loading spinner: the record from the logo, going round. The sheen on the disc is what shows that
 * it turns. It is drawn in code like the logo, so it needs no image file, and it follows the system's
 * animation settings, so it stands still when animations are turned off. [size] is how wide it is.
 */
@Composable
fun RecordSpinner(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val turn = rememberInfiniteTransition(label = "record spinner")
    val angle by turn.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Box(
        modifier = modifier.semantics { contentDescription = "Loading" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size).rotate(angle)) {
            drawRecord(Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension / 2f)
        }
    }
}
