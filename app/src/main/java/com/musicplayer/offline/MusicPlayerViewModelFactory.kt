package com.musicplayer.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.musicplayer.offline.data.UserLibraryRepository
import com.musicplayer.offline.data.PlaylistRepository
import com.musicplayer.offline.data.AppSettingsRepository

class MusicPlayerViewModelFactory(
    private val userLibrary: UserLibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MusicPlayerViewModel::class.java))
        return MusicPlayerViewModel(userLibrary, playlistRepository, settingsRepository) as T
    }
}
