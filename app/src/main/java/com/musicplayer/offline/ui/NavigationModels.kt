package com.musicplayer.offline.ui

enum class Destination { HOME, FAVORITES, RECENT }
enum class FeatureRoute { EQUALIZER, SLEEP_TIMER, LYRICS, SETTINGS }

enum class LibraryTab(val title: String) {
    SONGS("Faixas"),
    ARTISTS("Artistas"),
    ALBUMS("Álbuns"),
    PLAYLISTS("Playlists"),
    FOLDERS("Pastas")
}

enum class SmartPlaylist(val title: String) {
    FAVORITES("Favoritos"),
    LAST_ADDED("Última Edição"),
    RECENT_PLAYS("Reproduções Recentes"),
    MOST_PLAYED("As Mais Reproduzidas")
}

sealed interface LibraryRoute {
    data class Root(val tab: LibraryTab = LibraryTab.SONGS) : LibraryRoute
    data class Artist(val name: String) : LibraryRoute
    data class Album(val key: String) : LibraryRoute
    data class Playlist(val id: String) : LibraryRoute
    data class SmartPlaylistDetail(val playlist: SmartPlaylist) : LibraryRoute
    data class Folder(val path: String) : LibraryRoute
    data class Genre(val name: String) : LibraryRoute
    data object Genres : LibraryRoute
}
