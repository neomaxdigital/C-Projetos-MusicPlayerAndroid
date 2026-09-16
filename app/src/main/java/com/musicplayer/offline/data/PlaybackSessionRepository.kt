package com.musicplayer.offline.data

import android.content.Context

data class PlaybackSnapshot(
    val queueIds: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0,
    val repeatMode: Int = 0,
    val shuffleEnabled: Boolean = false,
    val wasPlaying: Boolean = false
)

object PlaybackSnapshotCodec {
    fun encode(value: PlaybackSnapshot): String = listOf(
        value.queueIds.joinToString(","), value.currentIndex, value.positionMs,
        value.repeatMode, value.shuffleEnabled, value.wasPlaying
    ).joinToString(";")

    fun decode(raw: String?): PlaybackSnapshot? {
        if (raw.isNullOrBlank()) return null
        val p = raw.split(';')
        if (p.size != 6) return null
        return runCatching {
            PlaybackSnapshot(
                queueIds = p[0].split(',').filter(String::isNotBlank).map(String::toLong),
                currentIndex = p[1].toInt().coerceAtLeast(0),
                positionMs = p[2].toLong().coerceAtLeast(0),
                repeatMode = p[3].toInt().takeIf { it in 0..2 } ?: 0,
                shuffleEnabled = p[4].toBooleanStrict(),
                wasPlaying = p[5].toBooleanStrict()
            )
        }.getOrNull()
    }
}

class PlaybackSessionRepository(context: Context) {
    private val preferences = context.getSharedPreferences("playback_session", Context.MODE_PRIVATE)
    fun load(): PlaybackSnapshot? = PlaybackSnapshotCodec.decode(preferences.getString("snapshot", null))
    fun save(snapshot: PlaybackSnapshot) {
        preferences.edit().putString("snapshot", PlaybackSnapshotCodec.encode(snapshot)).apply()
    }
}
