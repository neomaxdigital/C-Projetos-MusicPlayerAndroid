package com.musicplayer.offline.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.musicplayer.offline.data.AccentColor
import com.musicplayer.offline.data.AppSettings
import com.musicplayer.offline.data.ArtworkSize
import com.musicplayer.offline.data.ThemeMode

// Official default JUKE redesign palette: neutral dark only.
// Existing AccentColor values are retained for settings/backward compatibility,
// but the default approved visual layer does not use blue/cyan.
val JukePrimary = Color(0xFFF2F2F2)
val JukeElectricBlue = Color(0xFFD8D8D8)
val JukeBackground = Color(0xFF0B0F12)
val JukeSurface = Color(0xFF14191D)
val JukeSurfaceVariant = Color(0xFF20262B)
val JukeTextPrimary = Color(0xFFF5F5F5)
val JukeTextSecondary = Color(0xFFAEB4BA)
val JukeOnPrimary = Color(0xFF111417)
val JukePrimaryContainer = Color(0xFF30363B)
val JukeInactiveTrack = Color(0xFF3D4348)
val JukeInactiveThumb = Color(0xFFF2F2F2)
val JukeInactiveBorder = Color(0xFF666D73)

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
    val dark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val accent = settings.accentColor.themeColor()
    val colors = if (dark) darkColorScheme(
        primary = accent,
        onPrimary = JukeOnPrimary,
        primaryContainer = JukePrimaryContainer,
        onPrimaryContainer = JukeTextPrimary,
        secondary = accent,
        onSecondary = JukeOnPrimary,
        secondaryContainer = JukeSurfaceVariant,
        onSecondaryContainer = JukeTextPrimary,
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
        surfaceTint = Color.Transparent,
        outline = Color(0xFF62686D),
        outlineVariant = Color(0xFF363C41)
    ) else lightColorScheme(
        primary = Color(0xFF30363B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3E5E7),
        onPrimaryContainer = Color(0xFF151719),
        secondary = Color(0xFF50565B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE5E7E9),
        onSecondaryContainer = Color(0xFF1A1C1E),
        tertiary = Color(0xFF6A7075),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE6E8EA),
        onTertiaryContainer = Color(0xFF1A1C1E),
        background = Color(0xFFF4F5F6),
        onBackground = Color(0xFF17191B),
        surface = Color.White,
        onSurface = Color(0xFF17191B),
        surfaceVariant = Color(0xFFE4E6E8),
        onSurfaceVariant = Color(0xFF5D6368),
        surfaceTint = Color.Transparent,
        outline = Color(0xFF747A80),
        outlineVariant = Color(0xFFC6CACD)
    )
    MaterialTheme(colorScheme = colors) {
        val artwork = when (settings.artworkSize) {
            ArtworkSize.SMALL -> 44.dp
            ArtworkSize.MEDIUM -> 54.dp
            ArtworkSize.LARGE -> 68.dp
        }
        CompositionLocalProvider(LocalListArtworkSize provides artwork, content = content)
    }
}
