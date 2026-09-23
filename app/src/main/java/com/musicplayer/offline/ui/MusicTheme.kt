package com.musicplayer.offline.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.musicplayer.offline.data.AccentColor
import com.musicplayer.offline.data.AppSettings
import com.musicplayer.offline.data.ThemeMode
import com.musicplayer.offline.data.ArtworkSize

val JukePrimary = Color(0xFF00C2FB)
val JukeElectricBlue = Color(0xFF3A86FF)
val JukeBackground = Color(0xFF00141C)
val JukeSurface = Color(0xFF001C27)
val JukeSurfaceVariant = Color(0xFF142738)
val JukeTextPrimary = Color(0xFFF4F7FA)
val JukeTextSecondary = Color(0xFFA8BACB)
val JukeOnPrimary = Color(0xFF00141C)
val JukePrimaryContainer = Color(0xFF003B50)
val JukeInactiveTrack = Color(0xFF263641)
val JukeInactiveThumb = Color(0xFFDCE5EC)
val JukeInactiveBorder = Color(0xFF52616D)

val LocalListArtworkSize = staticCompositionLocalOf { 54.dp }

val AppBackground: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val SurfaceDark: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val SurfaceRaised: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val PrimaryBlue: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val TextMuted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant

internal fun AccentColor.themeColor(): Color = when (this) {
    AccentColor.CYAN -> JukePrimary
    AccentColor.ELECTRIC_BLUE -> JukeElectricBlue
}

@Composable
fun MusicPlayerTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> systemDark }
    val accent = settings.accentColor.themeColor()
    val colors = if (dark) darkColorScheme(
        primary = accent,
        onPrimary = JukeOnPrimary,
        primaryContainer = JukePrimaryContainer,
        onPrimaryContainer = JukeTextPrimary,
        secondary = accent,
        onSecondary = JukeOnPrimary,
        secondaryContainer = accent,
        onSecondaryContainer = JukeOnPrimary,
        tertiary = accent,
        onTertiary = JukeOnPrimary,
        tertiaryContainer = JukePrimaryContainer,
        onTertiaryContainer = JukeTextPrimary,
        background = JukeBackground,
        onBackground = JukeTextPrimary,
        surface = JukeSurface,
        onSurface = JukeTextPrimary,
        surfaceVariant = JukeSurfaceVariant,
        onSurfaceVariant = JukeTextSecondary,
        surfaceTint = accent,
        outline = Color(0xFF6F8291),
        outlineVariant = Color(0xFF344A5B)
    ) else lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD4ECFF),
        onPrimaryContainer = Color(0xFF001D35),
        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = accent,
        onSecondaryContainer = Color(0xFF001F27),
        tertiary = accent,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD0F0FF),
        onTertiaryContainer = Color(0xFF001E2B),
        background = Color(0xFFF4F8FC),
        onBackground = Color(0xFF10202C),
        surface = Color.White,
        onSurface = Color(0xFF10202C),
        surfaceVariant = Color(0xFFE2EAF1),
        onSurfaceVariant = Color(0xFF526675),
        surfaceTint = accent,
        outline = Color(0xFF6F7F8B),
        outlineVariant = Color(0xFFBECAD3)
    )
    MaterialTheme(colorScheme = colors) {
        val artwork = when (settings.artworkSize) { ArtworkSize.SMALL -> 44.dp; ArtworkSize.MEDIUM -> 54.dp; ArtworkSize.LARGE -> 68.dp }
        CompositionLocalProvider(LocalListArtworkSize provides artwork, content = content)
    }
}
