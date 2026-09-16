package com.musicplayer.offline.music

internal object QueueOrderRules {
    fun move(ids: List<Long>, from: Int, to: Int): List<Long> {
        if (from !in ids.indices || to !in ids.indices || from == to) return ids
        return ids.toMutableList().apply { add(to, removeAt(from)) }
    }
}
