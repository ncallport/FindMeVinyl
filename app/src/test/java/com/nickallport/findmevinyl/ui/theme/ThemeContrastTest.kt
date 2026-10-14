package com.nickallport.findmevinyl.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG contrast: 4.5 is the minimum for body text, 3.0 for large text and controls. */
class ThemeContrastTest {

    private fun luminance(color: Color): Double {
        fun channel(v: Float): Double = v.toDouble().let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertReadable(name: String, scheme: ColorScheme) {
        val body = listOf(
            "onBackground on background" to (scheme.onBackground to scheme.background),
            "onSurface on surface" to (scheme.onSurface to scheme.surface),
            "onSurfaceVariant on surface" to (scheme.onSurfaceVariant to scheme.surface),
            "onSurfaceVariant on background" to (scheme.onSurfaceVariant to scheme.background),
            "onPrimary on primary" to (scheme.onPrimary to scheme.primary),
            "onSecondary on secondary" to (scheme.onSecondary to scheme.secondary),
            "onPrimaryContainer on primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondaryContainer on secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "onSurface on surfaceVariant" to (scheme.onSurface to scheme.surfaceVariant),
            "primary text on background" to (scheme.primary to scheme.background),
            "primary text on surface" to (scheme.primary to scheme.surface),
            "onTertiary on tertiary" to (scheme.onTertiary to scheme.tertiary),
            "onError on error" to (scheme.onError to scheme.error)
        )
        body.forEach { (label, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue("$name: $label is only ${"%.2f".format(ratio)}:1", ratio >= 4.5)
        }
        val large = contrast(scheme.secondary, scheme.background)
        assertTrue("$name: secondary on background is only ${"%.2f".format(large)}:1", large >= 3.0)
    }

    @Test
    fun `the light theme is readable`() = assertReadable("light", LightColorScheme)

    @Test
    fun `the dark theme is readable`() = assertReadable("dark", DarkColorScheme)

    @Test
    fun `the two themes really are light and dark`() {
        assertTrue(luminance(LightColorScheme.background) > 0.5)
        assertTrue(luminance(DarkColorScheme.background) < 0.05)
    }

    @Test
    fun `the brand colours are the ones in the icon`() {
        assertEquals(0xFF1B9AAA.toInt(), BrandTeal.toArgb())
        assertEquals(0xFFE4572E.toInt(), BrandOrange.toArgb())
    }

    @Test
    fun `both themes use the brand teal and orange`() {
        assertEquals(BrandOrange.toArgb(), LightColorScheme.secondary.toArgb())
        assertEquals(BrandTeal.toArgb(), LightColorScheme.tertiary.toArgb())
        assertEquals(BrandTeal.toArgb(), DarkColorScheme.tertiary.toArgb())
    }

    @Test
    fun `the app does not take its colours from the wallpaper`() {
        assertEquals(false, USE_DYNAMIC_COLOR)
    }
}
