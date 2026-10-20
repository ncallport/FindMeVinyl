package com.nickallport.findmevinyl.ui.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small icons for the front screen buttons, drawn from path data so no icon library is needed. */
object Glyphs {
    private fun glyph(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }.build()

    /** A person, for artists. */
    val Person: ImageVector by lazy {
        glyph(
            "person",
            "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"
        )
    }

    /** A heart, for the want list. */
    val Heart: ImageVector by lazy {
        glyph(
            "heart",
            "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z"
        )
    }

    /** A magnifying glass, for search fields. */
    val Magnifier: ImageVector by lazy {
        glyph(
            "magnifier",
            "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"
        )
    }

    /** An arrow pointing up, for moving a row up a list. */
    val ArrowUp: ImageVector by lazy {
        glyph("arrow_up", "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8 -8,8z")
    }

    /** An arrow pointing down, for moving a row down a list. */
    val ArrowDown: ImageVector by lazy {
        glyph("arrow_down", "M20,12l-1.41,-1.41L13,16.17V4h-2v12.17l-5.58,-5.59L4,12l8,8 8,-8z")
    }

    /** Two arrows pointing up, for sending a row to the top of a list. */
    val DoubleArrowUp: ImageVector by lazy {
        glyph(
            "double_arrow_up",
            "M4,11l1.41,1.41L11,6.83V16h2V6.83l5.58,5.59L20,11l-8,-8 -8,8zM4,18l1.41,1.41L11,13.83V22h2v-8.17l5.58,5.59L20,18l-8,-8 -8,8z"
        )
    }

    /** A bin, for removing something. */
    val Delete: ImageVector by lazy {
        glyph(
            "delete",
            "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"
        )
    }

    /** A map pin, for shops. */
    val Pin: ImageVector by lazy {
        glyph(
            "pin",
            "M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z"
        )
    }
}
