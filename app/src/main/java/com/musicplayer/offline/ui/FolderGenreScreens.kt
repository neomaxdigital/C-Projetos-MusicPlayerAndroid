package com.musicplayer.offline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.offline.music.FolderGroup
import com.musicplayer.offline.music.GenreGroup
import com.musicplayer.offline.music.Song
import android.net.Uri

@Composable
fun FoldersScreen(folders: List<FolderGroup>, onBack: () -> Unit, onOpen: (String) -> Unit) {
    GroupBrowser(
        title = "Pastas",
        groups = folders.map { GroupItem(it.path, it.name, it.path, it.songs, it.artwork, it.artworkUri) },
        emptyMessage = "Nenhuma pasta com áudio foi encontrada.",
        onBack = onBack,
        onOpen = onOpen
    )
}

/**
 * A hierarchical view of the folders already accessible to the library. Android only exposes
 * folders containing audio through MediaStore, plus folders granted through SAF; selecting
 * "Procurar pasta" is the way to grant another location.
 */
@Composable
fun FolderBrowserScreen(
    songs: List<Song>,
    currentPath: String?,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddQueue: (Song) -> Unit,
    onAddPlaylist: (Song) -> Unit
) {
    val normalizedPath = currentPath.orEmpty().trim().trim('/')
    val childPaths = songs.asDirectChildFolders(normalizedPath)
    val songsHere = songs.filter { it.normalizedFolderPath() == normalizedPath }
    val title = normalizedPath.substringAfterLast('/').ifBlank { "Pastas" }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SimpleBackHeader(title, onBack, normalizedPath.takeIf { it.isNotBlank() })
        if (childPaths.isEmpty() && songsHere.isEmpty()) {
            EmptyLibraryPage("Pasta", "Nenhuma música foi encontrada nesta pasta.", Icons.Default.Folder)
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                items(childPaths, key = { "folder:$it" }) { path ->
                    val count = songs.count { song ->
                        val songPath = song.normalizedFolderPath()
                        songPath == path || songPath.startsWith("$path/")
                    }
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clickable { onOpenFolder(path) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = PrimaryBlue)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(path.substringAfterLast('/'), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(songCount(count), color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
                items(songsHere, key = { "song:${it.id}" }) { song ->
                    SongRow(
                        song, song.id == currentSongId, song.id in favoriteIds,
                        { onSong(song, songsHere) }, { onFavorite(song) }, { onArtist(song.artist) }, { onAlbum(song) },
                        { onPlayNext(song) }, { onAddQueue(song) }, { onAddPlaylist(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun GenresScreen(genres: List<GenreGroup>, onBack: () -> Unit, onOpen: (String) -> Unit) {
    GroupBrowser(
        title = "Gêneros",
        groups = genres.map { GroupItem(it.name, it.name, "", it.songs, it.artwork, it.artworkUri) },
        emptyMessage = "Nenhum gênero foi encontrado.",
        onBack = onBack,
        onOpen = onOpen
    )
}

@Composable
fun FolderDetailScreen(
    folder: FolderGroup,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddQueue: (Song) -> Unit,
    onAddPlaylist: (Song) -> Unit
) = GroupDetail(
    title = folder.name,
    subtitle = folder.path,
    songs = folder.songs,
    currentSongId = currentSongId,
    favoriteIds = favoriteIds,
    onBack = onBack,
    onSong = onSong,
    onFavorite = onFavorite,
    onArtist = onArtist,
    onAlbum = onAlbum,
    onPlayNext = onPlayNext,
    onAddQueue = onAddQueue,
    onAddPlaylist = onAddPlaylist
)

@Composable
fun GenreDetailScreen(
    genre: GenreGroup,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddQueue: (Song) -> Unit,
    onAddPlaylist: (Song) -> Unit
) = GroupDetail(
    title = genre.name,
    subtitle = songCount(genre.songs.size),
    songs = genre.songs,
    currentSongId = currentSongId,
    favoriteIds = favoriteIds,
    onBack = onBack,
    onSong = onSong,
    onFavorite = onFavorite,
    onArtist = onArtist,
    onAlbum = onAlbum,
    onPlayNext = onPlayNext,
    onAddQueue = onAddQueue,
    onAddPlaylist = onAddPlaylist
)

private data class GroupItem(
    val key: String,
    val name: String,
    val subtitle: String,
    val songs: List<Song>,
    val artwork: android.graphics.Bitmap?,
    val artworkUri: Uri?
)

@Composable
private fun GroupBrowser(title: String, groups: List<GroupItem>, emptyMessage: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SimpleBackHeader(title, onBack)
        if (groups.isEmpty()) {
            EmptyLibraryPage(title, emptyMessage, if (title == "Pastas") Icons.Default.Folder else Icons.Default.Category)
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(groups, key = { it.key }) { group ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpen(group.key) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ArtworkImage(group.artwork, 64.dp, sourceUri = group.artworkUri)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(group.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (group.subtitle.isNotBlank()) Text(group.subtitle, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(songCount(group.songs.size), color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupDetail(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddQueue: (Song) -> Unit,
    onAddPlaylist: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SimpleBackHeader(title, onBack, subtitle)
        LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song, song.id == currentSongId, song.id in favoriteIds,
                    { onSong(song, songs) }, { onFavorite(song) }, { onArtist(song.artist) }, { onAlbum(song) },
                    { onPlayNext(song) }, { onAddQueue(song) }, { onAddPlaylist(song) }
                )
            }
        }
    }
}

@Composable
fun SimpleBackHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun songCount(count: Int) = if (count == 1) "1 música" else "$count músicas"

private fun List<Song>.asDirectChildFolders(parentPath: String): List<String> =
    mapNotNull { song ->
        val path = song.normalizedFolderPath()
        when {
            path.isBlank() || path == parentPath -> null
            parentPath.isBlank() -> path.substringBefore('/').takeIf(String::isNotBlank)
            path.startsWith("$parentPath/") -> "$parentPath/${path.removePrefix("$parentPath/").substringBefore('/')}"
            else -> null
        }
    }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

private fun Song.normalizedFolderPath(): String = relativePath.trim().trim('/')
