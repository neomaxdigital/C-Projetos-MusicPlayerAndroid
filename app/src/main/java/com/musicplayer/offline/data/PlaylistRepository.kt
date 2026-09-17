package com.musicplayer.offline.data

import android.content.Context
import com.musicplayer.offline.music.LocalPlaylist
import com.musicplayer.offline.music.PlaylistRules
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

internal object PlaylistCodec {
    fun encode(playlists: List<LocalPlaylist>): String = playlists.joinToString("\n") { playlist ->
        val name = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(playlist.name.toByteArray(StandardCharsets.UTF_8))
        listOf(playlist.id, name, playlist.songIds.joinToString(",")).joinToString("|")
    }

    fun decode(value: String?): List<LocalPlaylist> = value.orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('|', limit = 3)
        if (parts.size != 3 || parts[0].isBlank()) return@mapNotNull null
        runCatching {
            val name = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            val songIds = parts[2].split(',').mapNotNull(String::toLongOrNull).distinct()
            LocalPlaylist(parts[0], name, songIds)
        }.getOrNull()
    }.toList()
}

class PlaylistRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun playlists(): List<LocalPlaylist> = PlaylistCodec.decode(preferences.getString(PLAYLISTS_KEY, null))

    fun create(name: String): List<LocalPlaylist> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return playlists()
        return save(playlists() + LocalPlaylist(UUID.randomUUID().toString(), cleanName, emptyList()))
    }

    fun rename(playlistId: String, name: String): List<LocalPlaylist> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return playlists()
        return save(playlists().map { if (it.id == playlistId) it.copy(name = cleanName) else it })
    }

    fun delete(playlistId: String): List<LocalPlaylist> = save(playlists().filterNot { it.id == playlistId })

    fun addSong(playlistId: String, songId: Long): List<LocalPlaylist> =
        save(playlists().map { if (it.id == playlistId) PlaylistRules.addSong(it, songId) else it })

    fun removeSong(playlistId: String, songId: Long): List<LocalPlaylist> =
        save(playlists().map { if (it.id == playlistId) PlaylistRules.removeSong(it, songId) else it })

    fun moveSong(playlistId: String, from: Int, to: Int): List<LocalPlaylist> =
        save(playlists().map { if (it.id == playlistId) PlaylistRules.moveSong(it, from, to) else it })

    fun retainOnly(validIds: Set<Long>): List<LocalPlaylist> {
        val current = playlists()
        val updated = current.map { PlaylistRules.retainSongs(it, validIds) }
        return if (updated == current) current else save(updated)
    }

    private fun save(playlists: List<LocalPlaylist>): List<LocalPlaylist> {
        preferences.edit().putString(PLAYLISTS_KEY, PlaylistCodec.encode(playlists)).apply()
        return playlists
    }

    private companion object {
        const val PREFERENCES_NAME = "user_library"
        const val PLAYLISTS_KEY = "local_playlists_v1"
    }
}
