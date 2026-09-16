package com.musicplayer.offline.data

import android.content.Context

enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class AccentColor { CYAN, ELECTRIC_BLUE }
enum class ArtworkSize { SMALL, MEDIUM, LARGE }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.CYAN,
    val resumeLastTrack: Boolean = true,
    val resumePosition: Boolean = true,
    val autoResume: Boolean = false,
    val restoreShuffle: Boolean = true,
    val restoreRepeat: Boolean = true,
    val showShortSongs: Boolean = true,
    val minimumDurationSeconds: Int = 0,
    val showUnknownFiles: Boolean = true,
    val showMiniPlayer: Boolean = true,
    val artworkSize: ArtworkSize = ArtworkSize.MEDIUM,
    val animationsEnabled: Boolean = true
)

object AppSettingsCodec {
    fun encode(value: AppSettings): String = listOf(
        value.themeMode.name, value.accentColor.name,
        value.resumeLastTrack, value.resumePosition, value.autoResume,
        value.restoreShuffle, value.restoreRepeat, value.showShortSongs,
        value.minimumDurationSeconds.coerceIn(0, 600), value.showUnknownFiles,
        value.showMiniPlayer, value.artworkSize.name, value.animationsEnabled
    ).joinToString("|")

    fun decode(raw: String?): AppSettings {
        val p = raw?.split('|') ?: return AppSettings()
        if (p.size < 13) return AppSettings()
        return runCatching {
            AppSettings(
                ThemeMode.valueOf(p[0]), decodeAccent(p[1]),
                p[2].toBooleanStrict(), p[3].toBooleanStrict(), p[4].toBooleanStrict(),
                p[5].toBooleanStrict(), p[6].toBooleanStrict(), p[7].toBooleanStrict(),
                p[8].toInt().coerceIn(0, 600), p[9].toBooleanStrict(),
                p[10].toBooleanStrict(), ArtworkSize.valueOf(p[11]), p[12].toBooleanStrict()
            )
        }.getOrDefault(AppSettings())
    }

    private fun decodeAccent(raw: String): AccentColor = when (raw) {
        "BLUE", "CYAN" -> AccentColor.CYAN
        "ELECTRIC_BLUE" -> AccentColor.ELECTRIC_BLUE
        else -> AccentColor.CYAN
    }
}

class AppSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    fun load(): AppSettings = AppSettingsCodec.decode(preferences.getString("settings", null))
    fun save(value: AppSettings): AppSettings {
        preferences.edit().putString("settings", AppSettingsCodec.encode(value)).apply()
        return value
    }
}
