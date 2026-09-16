package com.musicplayer.offline.data

import android.content.Context

internal object LibraryIdRules {
    fun toggleFavorite(ids: Set<Long>, songId: Long): Set<Long> =
        ids.toMutableSet().apply { if (!add(songId)) remove(songId) }

    fun addRecent(ids: List<Long>, songId: Long, limit: Int = 100): List<Long> {
        if (ids.firstOrNull() == songId) return ids
        return buildList {
            add(songId)
            ids.filterTo(this) { it != songId }
        }.take(limit)
    }

    fun retainFavorites(ids: Set<Long>, validIds: Set<Long>): Set<Long> = ids.filterTo(linkedSetOf(), validIds::contains)
    fun retainRecents(ids: List<Long>, validIds: Set<Long>): List<Long> = ids.filter(validIds::contains)

    fun retainPlayCounts(counts: Map<Long, Int>, validIds: Set<Long>): Map<Long, Int> =
        counts.filterKeys(validIds::contains).filterValues { it > 0 }
}

class UserLibraryRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun favoriteIds(): Set<Long> = readIds(FAVORITES_KEY).toSet()

    fun recentIds(): List<Long> = readIds(RECENTS_KEY)

    fun playCounts(): Map<Long, Int> = preferences.getString(PLAY_COUNTS_KEY, null)
        ?.split(',')
        ?.mapNotNull { entry ->
            val (id, count) = entry.split(':', limit = 2).let { it.getOrNull(0)?.toLongOrNull() to it.getOrNull(1)?.toIntOrNull() }
            id?.takeIf { count != null && count > 0 }?.let { it to count!! }
        }
        ?.toMap()
        .orEmpty()

    fun toggleFavorite(songId: Long): Set<Long> {
        val updated = LibraryIdRules.toggleFavorite(favoriteIds(), songId)
        writeIds(FAVORITES_KEY, updated.toList())
        return updated
    }

    fun recordRecent(songId: Long): List<Long> {
        val current = recentIds()
        val updated = LibraryIdRules.addRecent(current, songId)
        if (updated !== current) writeIds(RECENTS_KEY, updated)
        return updated
    }

    fun recordPlay(songId: Long): Map<Long, Int> {
        val updated = playCounts().toMutableMap().apply {
            this[songId] = (this[songId] ?: 0).coerceAtMost(Int.MAX_VALUE - 1) + 1
        }
        writePlayCounts(updated)
        return updated
    }

    fun retainOnly(validIds: Set<Long>): Triple<Set<Long>, List<Long>, Map<Long, Int>> {
        val previousFavorites = favoriteIds()
        val previousRecents = recentIds()
        val previousPlayCounts = playCounts()
        if (validIds.isEmpty()) return Triple(previousFavorites, previousRecents, previousPlayCounts)
        val favorites = LibraryIdRules.retainFavorites(previousFavorites, validIds)
        val recents = LibraryIdRules.retainRecents(previousRecents, validIds)
        val playCounts = LibraryIdRules.retainPlayCounts(previousPlayCounts, validIds)
        if (favorites != previousFavorites) writeIds(FAVORITES_KEY, favorites.toList())
        if (recents != previousRecents) writeIds(RECENTS_KEY, recents)
        if (playCounts != previousPlayCounts) writePlayCounts(playCounts)
        return Triple(favorites, recents, playCounts)
    }

    private fun readIds(key: String): List<Long> = preferences.getString(key, null)
        ?.split(',')
        ?.mapNotNull(String::toLongOrNull)
        .orEmpty()

    private fun writeIds(key: String, ids: List<Long>) {
        preferences.edit().putString(key, ids.distinct().joinToString(",")).apply()
    }

    private fun writePlayCounts(counts: Map<Long, Int>) {
        preferences.edit().putString(
            PLAY_COUNTS_KEY,
            counts.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        ).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "user_library"
        const val FAVORITES_KEY = "favorite_song_ids"
        const val RECENTS_KEY = "recent_song_ids"
        const val PLAY_COUNTS_KEY = "song_play_counts"
    }
}
