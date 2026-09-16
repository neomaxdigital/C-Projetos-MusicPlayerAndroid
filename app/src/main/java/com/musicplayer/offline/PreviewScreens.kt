package com.musicplayer.offline

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.musicplayer.offline.music.Song
import com.musicplayer.offline.music.SongSort
import com.musicplayer.offline.ui.AppBackground
import com.musicplayer.offline.ui.AppBottomBar
import com.musicplayer.offline.ui.Destination
import com.musicplayer.offline.ui.LibraryRootScreen
import com.musicplayer.offline.ui.LibraryTab
import com.musicplayer.offline.ui.MiniPlayer
import com.musicplayer.offline.ui.MusicPlayerTheme
import com.musicplayer.offline.ui.NowPlayingScreen

private const val PreviewPhone = "spec:width=411dp,height=891dp,dpi=420"

internal fun previewSongs(): List<Song> = listOf(
    Song(1, "O Digno de Louvor", "Fernando Ramos", Uri.parse("content://preview/song/1"), null, "Momentos de Fé", 1, 276_000, 10, 1),
    Song(2, "Restaura Senhor", "Louvor Musical", Uri.parse("content://preview/song/2"), null, "Adoração", 2, 241_000, 20, 2),
    Song(3, "Deus Está Aqui", "Ministério Adoração", Uri.parse("content://preview/song/3"), null, "Vida Nova", 3, 218_000, 30, 3),
    Song(4, "Pela Fé Seguirei", "Gospel Essencial", Uri.parse("content://preview/song/4"), null, "Esperança", 4, 232_000, 40, 4),
    Song(5, "O Bom Pastor", "Fernando Ramos", Uri.parse("content://preview/song/5"), null, "Momentos de Fé", 1, 205_000, 50, 5),
    Song(6, "Grandes Coisas", "Ministério Vida", Uri.parse("content://preview/song/6"), null, "Gratidão", 5, 259_000, 60, 6),
    Song(7, "Meu Refúgio", "Louvor Musical", Uri.parse("content://preview/song/7"), null, "Adoração", 2, 225_000, 70, 7),
    Song(8, "Santo é o Senhor", "Gospel Essencial", Uri.parse("content://preview/song/8"), null, "Esperança", 4, 247_000, 80, 8)
)

@Composable
internal fun PreviewTheme(content: @Composable () -> Unit) = MusicPlayerTheme(content = content)

@Composable
internal fun MainMusicPreviewContent() = PreviewTheme {
    Surface(Modifier.fillMaxSize(), color = AppBackground) {
        LibraryRootScreen(
            songs = previewSongs(),
            tab = LibraryTab.SONGS,
            sort = SongSort.TITLE,
            shuffleEnabled = false,
            currentSongId = 1,
            favoriteIds = setOf(1),
            onTab = {},
            onSort = {},
            onShuffle = {},
            onSearch = {},
            onSong = { _, _ -> },
            onToggleFavorite = {},
            onArtist = {},
            onAlbum = {},
            onPlayNext = {},
            onAddToQueue = {},
            onAddToPlaylist = {},
            onFolders = {},
            onGenres = {},
            onQueue = {},
            onEqualizer = {},
            onSleepTimer = {},
            onSettings = {},
            onAddFolder = {},
            onAddSong = {},
            onRefreshLibrary = {},
            playlistContent = {}
        )
    }
}

@Composable
internal fun NowPlayingPreviewContent() = PreviewTheme {
    Surface(Modifier.fillMaxSize(), color = AppBackground) {
        NowPlayingScreen(
            song = previewSongs().first(), player = null,
            position = 102_000L, duration = 276_000L, playing = true,
            shuffleEnabled = true, repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL,
            favorite = true, toggleFavorite = {}, openQueue = {}, back = {}
        )
    }
}

@Composable
internal fun MiniPlayerPreviewContent() = PreviewTheme {
    Column(Modifier.fillMaxSize().background(AppBackground), verticalArrangement = Arrangement.Bottom) {
        MiniPlayer(previewSongs().first(), player = null, playing = true, open = {})
    }
}

@Composable
internal fun BottomNavigationPreviewContent() = PreviewTheme {
    Column(Modifier.fillMaxSize().background(AppBackground), verticalArrangement = Arrangement.Bottom) {
        AppBottomBar(selected = Destination.HOME, select = {})
    }
}

@Preview(name = "Tela principal - Músicas", device = PreviewPhone, showSystemUi = true)
@Composable private fun MainMusicPreview() = MainMusicPreviewContent()

@Preview(name = "Reproduzindo agora", device = PreviewPhone, showSystemUi = true)
@Composable private fun NowPlayingPreview() = NowPlayingPreviewContent()

@Preview(name = "Mini player", device = PreviewPhone, showBackground = true, backgroundColor = 0xFF07121B)
@Composable private fun MiniPlayerPreview() = MiniPlayerPreviewContent()

@Preview(name = "Navegação inferior", device = PreviewPhone, showBackground = true, backgroundColor = 0xFF07121B)
@Composable private fun BottomNavigationPreview() = BottomNavigationPreviewContent()
