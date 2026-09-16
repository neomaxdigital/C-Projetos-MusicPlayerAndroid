package com.musicplayer.offline.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsCodecTest {
    @Test fun settingsRoundTripPreservesEveryOption() {
        val expected = AppSettings(
            themeMode = ThemeMode.LIGHT, accentColor = AccentColor.BLUE,
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

    @Test fun legacyAccentChoicesMigrateToOfficialJukeBlue() {
        val base = AppSettingsCodec.encode(AppSettings(accentColor = AccentColor.BLUE))
        assertEquals(AccentColor.BLUE, AppSettingsCodec.decode(base.replace("|BLUE|", "|GREEN|")).accentColor)
        assertEquals(AccentColor.BLUE, AppSettingsCodec.decode(base.replace("|BLUE|", "|PURPLE|")).accentColor)
        assertEquals(AccentColor.BLUE, AppSettingsCodec.decode(base.replace("|BLUE|", "|CYAN|")).accentColor)
    }
}
