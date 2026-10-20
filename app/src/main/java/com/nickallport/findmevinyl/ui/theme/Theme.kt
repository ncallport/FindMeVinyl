package com.nickallport.findmevinyl.ui.theme

import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Off: the app keeps its own colours, the teal and orange of the icon, in both light and dark,
 * and does not take them from the wallpaper.
 */
const val USE_DYNAMIC_COLOR = false

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = TealDeep,
    onPrimary = Color.White,
    primaryContainer = TealTint,
    onPrimaryContainer = TealInk,
    secondary = BrandOrange,
    onSecondary = OrangeInk,
    secondaryContainer = OrangeTint,
    onSecondaryContainer = OrangeInk,
    tertiary = BrandTeal,
    onTertiary = TealBlack,
    background = Paper,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    surfaceVariant = PaperVariant,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFF857F76),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = TealInk,
    primaryContainer = TealDark,
    onPrimaryContainer = TealPale,
    secondary = OrangeLight,
    onSecondary = OrangeInk,
    secondaryContainer = OrangeDark,
    onSecondaryContainer = OrangeTint,
    tertiary = BrandTeal,
    onTertiary = TealBlack,
    background = Charcoal,
    onBackground = Chalk,
    surface = CharcoalCard,
    onSurface = Chalk,
    surfaceVariant = CharcoalVariant,
    onSurfaceVariant = ChalkSoft,
    outline = Color(0xFF8A9A9F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun FindMeVinylTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * The colour of the "next ten" wants on the want list: the wanted colour faded, half way to the background, so it reads
 * as the same colour but less important than the top ten. Text on it is the usual text colour of the page.
 */
fun fadedWantContainer(scheme: ColorScheme): Color = lerp(scheme.background, scheme.secondaryContainer, 0.5f)
