@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.musicplayer.offline.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

object PlaybackRuntime {
    private var player: ExoPlayer? = null
    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (::appContext.isInitialized) EqualizerManager.attach(appContext, audioSessionId)
        }
    }
    private lateinit var appContext: Context

    fun bind(context: Context, exoPlayer: ExoPlayer) {
        appContext = context.applicationContext
        player = exoPlayer
        exoPlayer.addListener(listener)
        EqualizerManager.attach(appContext, exoPlayer.audioSessionId)
        SleepTimerManager.bind(exoPlayer)
    }

    fun unbind() {
        player?.removeListener(listener)
        SleepTimerManager.unbind()
        EqualizerManager.release()
        player = null
    }
}
