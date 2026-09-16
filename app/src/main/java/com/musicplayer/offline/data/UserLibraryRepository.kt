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
}

class UserLibraryRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun favoriteIds(): Set<Long> = readIds(FAVORITES_KEY).toSet()

    fun recentIds(): List<Long> = readIds(RECENTS_KEY)

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

    fun retainOnly(validIds: Set<Long>): Pair<Set<Long>, List<Long>> {
        val previousFavorites = favoriteIds()
        val previousRecents = recentIds()
        if (validIds.isEmpty()) return previousFavorites to previousRecents
        val favorites = LibraryIdRules.retainFavorites(previousFavorites, validIds)
        val recents = LibraryIdRules.retainRecents(previousRecents, validIds)
        if (favorites != previousFavorites) writeIds(FAVORITES_KEY, favorites.toList())
        if (recents != previousRecents) writeIds(RECENTS_KEY, recents)
        return favorites to recents
    }

    private fun readIds(key: String): List<Long> = preferences.getString(key, null)
        ?.split(',')
        ?.mapNotNull(String::toLongOrNull)
        .orEmpty()

    private fun writeIds(key: String, ids: List<Long>) {
        preferences.edit().putString(key, ids.distinct().joinToString(",")).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "user_library"
        const val FAVORITES_KEY = "favorite_song_ids"
        const val RECENTS_KEY = "recent_song_ids"
    }
}
