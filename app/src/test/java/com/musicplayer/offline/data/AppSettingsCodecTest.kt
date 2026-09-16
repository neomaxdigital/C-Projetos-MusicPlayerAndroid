package com.musicplayer.offline.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsCodecTest {
    @Test fun settingsRoundTripPreservesEveryOption() {
        val expected = AppSettings(
            themeMode = ThemeMode.LIGHT, accentColor = AccentColor.ELECTRIC_BLUE,
            resumeLastTrack = false, resumePosition = false, autoResume = true,
            restoreShuffle = false, restoreRepeat = false, showShortSongs = false,
            minimumDurationSeconds = 42, showUnknownFiles = false, showMiniPlayer = false,
            artworkSize = ArtworkSize.LARGE, animationsEnabled = false
        )
        assertEquals(expected, AppSettingsCodec.decode(AppSettingsCodec.encode(expected)))
    }

    @Test fun invalidSettingsFallBackSafely() {
        assertEquals(AppSettings(), AppSettingsCodec.decode("invalid"))
    }

    @Test fun legacyBlueAccentMigratesToCyan() {
        val base = AppSettingsCodec.encode(AppSettings(accentColor = AccentColor.CYAN))
        assertEquals(AccentColor.CYAN, AppSettingsCodec.decode(base.replace("|CYAN|", "|BLUE|")).accentColor)
    }

    @Test fun supportedAndInvalidAccentValuesDecodeSafely() {
        val base = AppSettingsCodec.encode(AppSettings(accentColor = AccentColor.CYAN))
        assertEquals(AccentColor.CYAN, AppSettingsCodec.decode(base).accentColor)
        assertEquals(AccentColor.ELECTRIC_BLUE, AppSettingsCodec.decode(base.replace("|CYAN|", "|ELECTRIC_BLUE|")).accentColor)
        assertEquals(AccentColor.CYAN, AppSettingsCodec.decode(base.replace("|CYAN|", "|INVALID|")).accentColor)
    }
}
