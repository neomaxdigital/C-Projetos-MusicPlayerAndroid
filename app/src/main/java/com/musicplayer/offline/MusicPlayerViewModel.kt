package com.musicplayer.offline

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.musicplayer.offline.data.UserLibraryRepository
import com.musicplayer.offline.data.PlaylistRepository
import com.musicplayer.offline.data.AppSettings
import com.musicplayer.offline.data.AppSettingsRepository
import com.musicplayer.offline.music.LocalPlaylist
import com.musicplayer.offline.music.LibrarySongMerge
import com.musicplayer.offline.music.Song
import com.musicplayer.offline.music.SongSort
import com.musicplayer.offline.music.sortedByOption

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val recentIds: List<Long> = emptyList(),
    val playCounts: Map<Long, Int> = emptyMap(),
    val playlists: List<LocalPlaylist> = emptyList(),
    val sort: SongSort = SongSort.TITLE,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val loadError: String? = null
) {
    val sortedSongs: List<Song> get() = songs.sortedByOption(sort)
    val favoriteSongs: List<Song> get() = favoriteIds.mapNotNull { id -> songs.find { it.id == id } }
    val recentSongs: List<Song> get() = recentIds.mapNotNull { id -> songs.find { it.id == id } }
    val mostPlayedSongs: List<Song> get() = songs
        .filter { (playCounts[it.id] ?: 0) > 0 }
        .sortedWith(compareByDescending<Song> { playCounts[it.id] ?: 0 }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}

class MusicPlayerViewModel(
    private val userLibrary: UserLibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {
    var state by mutableStateOf(
        LibraryUiState(
            favoriteIds = userLibrary.favoriteIds(),
            recentIds = userLibrary.recentIds(),
            playCounts = userLibrary.playCounts(),
            playlists = playlistRepository.playlists(),
            settings = settingsRepository.load()
        )
    )
        private set

    fun setSongs(songs: List<Song>) {
        val validIds = songs.mapTo(hashSetOf()) { it.id }
        val (favorites, recents, playCounts) = userLibrary.retainOnly(validIds)
        state = state.copy(
            songs = songs,
            favoriteIds = favorites,
            recentIds = recents,
            playCounts = playCounts,
            playlists = playlistRepository.retainOnly(validIds),
            isLoading = false,
            loadError = null
        )
    }

    /** Adds a selected SAF document to the visible library before the background refresh ends. */
    fun addSongImmediately(song: Song): Boolean {
        val merged = LibrarySongMerge.merge(state.songs, listOf(song))
        if (merged.size == state.songs.size) return false
        setSongs(merged)
        return true
    }

    fun beginLibraryLoad() { state = state.copy(isLoading = true, loadError = null) }

    fun setLibraryError(message: String) { state = state.copy(isLoading = false, loadError = message) }

    fun removeUnavailableSong(songId: Long) { setSongs(state.songs.filterNot { it.id == songId }) }

    fun setSort(sort: SongSort) {
        state = state.copy(sort = sort)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        state = state.copy(settings = settingsRepository.save(transform(state.settings)))
    }

    fun toggleFavorite(songId: Long) {
        state = state.copy(favoriteIds = userLibrary.toggleFavorite(songId))
    }

    fun recordRecent(songId: Long) {
        state = state.copy(recentIds = userLibrary.recordRecent(songId))
    }

    fun recordPlay(songId: Long) {
        state = state.copy(playCounts = userLibrary.recordPlay(songId))
    }

    fun createPlaylist(name: String) {
        state = state.copy(playlists = playlistRepository.create(name))
    }

    fun createPlaylistWithSong(name: String, songId: Long) {
        if (name.isBlank()) return
        val previousIds = state.playlists.mapTo(mutableSetOf()) { it.id }
        val created = playlistRepository.create(name)
        val playlistId = created.firstOrNull { it.id !in previousIds }?.id ?: return
        state = state.copy(playlists = playlistRepository.addSong(playlistId, songId))
    }

    fun renamePlaylist(playlistId: String, name: String) {
        state = state.copy(playlists = playlistRepository.rename(playlistId, name))
    }

    fun deletePlaylist(playlistId: String) {
        state = state.copy(playlists = playlistRepository.delete(playlistId))
    }

    fun addSongToPlaylist(playlistId: String, songId: Long) {
        state = state.copy(playlists = playlistRepository.addSong(playlistId, songId))
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        state = state.copy(playlists = playlistRepository.removeSong(playlistId, songId))
    }

    fun movePlaylistSong(playlistId: String, from: Int, to: Int) {
        state = state.copy(playlists = playlistRepository.moveSong(playlistId, from, to))
    }
}
