package com.nickallport.findmevinyl.ui.brand

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickallport.findmevinyl.ui.theme.BrandOrange
import com.nickallport.findmevinyl.ui.theme.BrandTeal

private val RecordBlack = Color(0xFF141414)
private val Groove = Color(0xFF303030)

private const val NAME = "Find Me Vinyl"

/**
 * The record from the app icon: a teal badge, a black disc with grooves and a sheen, and an
 * orange label with a spindle hole, centred at [center] and drawn inside [radius].
 */
private fun DrawScope.drawRecord(center: Offset, radius: Float) {
    drawCircle(BrandTeal, radius, center)
    val disc = radius * 0.80f
    drawCircle(RecordBlack, disc, center)
    listOf(0.87f, 0.72f, 0.57f).forEach { f ->
        drawCircle(Groove, disc * f, center, style = Stroke(width = radius * 0.012f))
    }
    // The sheen, two faint arcs on opposite sides of the disc.
    val sheen = Stroke(width = radius * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    val box = androidx.compose.ui.geometry.Size(disc * 1.6f, disc * 1.6f)
    val topLeft = Offset(center.x - disc * 0.8f, center.y - disc * 0.8f)
    drawArc(Color.White.copy(alpha = 0.2f), 200f, 40f, false, topLeft, box, style = sheen)
    drawArc(Color.White.copy(alpha = 0.2f), 20f, 40f, false, topLeft, box, style = sheen)
    drawCircle(BrandOrange, disc * 0.37f, center)
    drawCircle(RecordBlack, disc * 0.06f, center)
}

/**
 * The big logo for the front screen: the record, with "Find Me Vinyl" in script curving over the
 * top of it, like the closing title of an old cartoon. It is drawn in code, with Android's built in
 * script font, so it needs no image file and no font file.
 */
@Composable
fun FindMeVinylLogo(modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.secondary.toArgb()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.08f)
            .testTag("logo_large")
            .semantics { contentDescription = NAME }
    ) {
        val w = size.width
        val center = Offset(w / 2f, size.height * 0.60f)
        val badge = w * 0.30f
        drawRecord(center, badge)

        val textRadius = badge + w * 0.06f
        val paint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = w * 0.12f
            typeface = Typeface.create("cursive", Typeface.BOLD)
        }
        // The text runs clockwise over the top, from the left (180 degrees) to the right (360).
        val sweep = 200f
        val arc = Path().apply {
            addArc(
                RectF(center.x - textRadius, center.y - textRadius, center.x + textRadius, center.y + textRadius),
                170f, sweep
            )
        }
        val arcLength = (Math.PI * textRadius * sweep / 180.0).toFloat()
        val offset = ((arcLength - paint.measureText(NAME)) / 2f).coerceAtLeast(0f)
        drawContext.canvas.nativeCanvas.drawTextOnPath(NAME, arc, offset, 0f, paint)
    }
}

/**
 * The slim bar at the top of every screen except the front one: a small record and the name in
 * script. With [onHome] it is a button that goes back to the front screen.
 */
@Composable
fun Masthead(onHome: (() -> Unit)?, modifier: Modifier = Modifier) {
    val description = if (onHome != null) "$NAME, go to the front screen" else NAME
    val clickable = if (onHome != null) Modifier.clickable { onHome() } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("masthead")
            .semantics(mergeDescendants = true) { contentDescription = description }
            // Focus moved here after the keyboard's search key and left a grey band on the bar.
            .focusProperties { canFocus = false }
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            drawRecord(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = NAME,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
