package com.musicplayer.offline.ui

enum class Destination { HOME, FAVORITES, RECENT }
enum class FeatureRoute { EQUALIZER, SLEEP_TIMER, LYRICS, SETTINGS }

enum class LibraryTab(val title: String) {
    SONGS("Músicas"),
    ARTISTS("Artistas"),
    ALBUMS("Álbuns"),
    PLAYLISTS("Playlists")
}

sealed interface LibraryRoute {
    data class Root(val tab: LibraryTab = LibraryTab.SONGS) : LibraryRoute
    data class Artist(val name: String) : LibraryRoute
    data class Album(val key: String) : LibraryRoute
    data class Playlist(val id: String) : LibraryRoute
    data class Folder(val path: String) : LibraryRoute
    data class Genre(val name: String) : LibraryRoute
    data object Folders : LibraryRoute
    data object Genres : LibraryRoute
}
