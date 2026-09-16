package com.musicplayer.offline.music

data class LocalPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>
)

internal object PlaylistRules {
    fun addSong(playlist: LocalPlaylist, songId: Long): LocalPlaylist =
        if (songId in playlist.songIds) playlist else playlist.copy(songIds = playlist.songIds + songId)

    fun removeSong(playlist: LocalPlaylist, songId: Long): LocalPlaylist =
        playlist.copy(songIds = playlist.songIds.filterNot { it == songId })

    fun retainSongs(playlist: LocalPlaylist, validIds: Set<Long>): LocalPlaylist =
        playlist.copy(songIds = playlist.songIds.filter(validIds::contains))

    fun moveSong(playlist: LocalPlaylist, from: Int, to: Int): LocalPlaylist {
        if (from !in playlist.songIds.indices || to !in playlist.songIds.indices || from == to) return playlist
        val reordered = playlist.songIds.toMutableList()
        val item = reordered.removeAt(from)
        reordered.add(to, item)
        return playlist.copy(songIds = reordered)
    }
}
