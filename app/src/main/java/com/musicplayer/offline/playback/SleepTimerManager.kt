package com.musicplayer.offline.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SleepTimerMode { OFF, DEADLINE, END_OF_TRACK }

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val remainingMs: Long = 0,
    val fadeEnabled: Boolean = true
) { val active: Boolean get() = mode != SleepTimerMode.OFF }

object SleepTimerRules {
    fun remaining(deadlineElapsedMs: Long, nowElapsedMs: Long): Long = (deadlineElapsedMs - nowElapsedMs).coerceAtLeast(0)
    fun fadeFactor(remainingMs: Long, fadeWindowMs: Long = 10_000): Float =
        if (fadeWindowMs <= 0 || remainingMs >= fadeWindowMs) 1f else (remainingMs.toFloat() / fadeWindowMs).coerceIn(0f, 1f)
}

object SleepTimerManager {
    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = mutableState.asStateFlow()
    private var player: Player? = null
    private var deadline = 0L
    private var originalVolume = 1f
    private var targetMediaId: String? = null
    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mutableState.value.mode == SleepTimerMode.END_OF_TRACK && targetMediaId != null &&
                reason in setOf(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
            ) finish()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (mutableState.value.mode == SleepTimerMode.END_OF_TRACK && playbackState == Player.STATE_ENDED) finish()
        }
    }
    private val tick = object : Runnable {
        override fun run() {
            if (mutableState.value.mode != SleepTimerMode.DEADLINE) return
            val remaining = SleepTimerRules.remaining(deadline, SystemClock.elapsedRealtime())
            mutableState.value = mutableState.value.copy(remainingMs = remaining)
            if (mutableState.value.fadeEnabled) player?.volume = originalVolume * SleepTimerRules.fadeFactor(remaining)
            if (remaining == 0L) finish() else handler.postDelayed(this, 1_000)
        }
    }

    fun bind(player: Player) {
        this.player?.removeListener(listener)
        this.player = player
        originalVolume = player.volume
        player.addListener(listener)
    }

    fun startMinutes(minutes: Int, fade: Boolean = true) {
        if (minutes <= 0) return
        cancel()
        originalVolume = player?.volume ?: 1f
        deadline = SystemClock.elapsedRealtime() + minutes * 60_000L
        mutableState.value = SleepTimerState(SleepTimerMode.DEADLINE, minutes * 60_000L, fade)
        handler.post(tick)
    }

    fun startEndOfTrack(fade: Boolean = false) {
        cancel()
        originalVolume = player?.volume ?: 1f
        targetMediaId = player?.currentMediaItem?.mediaId
        if (targetMediaId != null) mutableState.value = SleepTimerState(SleepTimerMode.END_OF_TRACK, 0, fade)
    }

    fun cancel() {
        handler.removeCallbacks(tick)
        player?.volume = originalVolume
        deadline = 0L
        targetMediaId = null
        mutableState.value = SleepTimerState()
    }

    fun unbind() {
        cancel()
        player?.removeListener(listener)
        player = null
    }

    private fun finish() {
        handler.removeCallbacks(tick)
        player?.pause()
        player?.volume = originalVolume
        deadline = 0L
        targetMediaId = null
        mutableState.value = SleepTimerState()
    }
}
