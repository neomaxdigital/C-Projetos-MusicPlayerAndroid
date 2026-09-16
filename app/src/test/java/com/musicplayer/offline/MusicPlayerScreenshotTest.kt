package com.musicplayer.offline

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class MusicPlayerScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:style/Theme.Material.NoActionBar",
        showSystemUi = false
    )

    @Test fun telaPrincipalMusicas() = paparazzi.snapshot { MainMusicPreviewContent() }
    @Test fun reproduzindoAgora() = paparazzi.snapshot { NowPlayingPreviewContent() }
    @Test fun miniPlayer() = paparazzi.snapshot { MiniPlayerPreviewContent() }
    @Test fun navegacaoInferior() = paparazzi.snapshot { BottomNavigationPreviewContent() }
}
