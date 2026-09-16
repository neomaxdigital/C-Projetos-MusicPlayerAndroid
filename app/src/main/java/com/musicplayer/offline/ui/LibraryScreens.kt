package com.musicplayer.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.offline.music.AlbumGroup
import com.musicplayer.offline.music.ArtistGroup
import com.musicplayer.offline.music.Song
import com.musicplayer.offline.music.SongSort
import com.musicplayer.offline.music.asAlbums
import com.musicplayer.offline.music.asArtists

@Composable
fun LibraryRootScreen(
    songs: List<Song>,
    tab: LibraryTab,
    sort: SongSort,
    shuffleEnabled: Boolean,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onTab: (LibraryTab) -> Unit,
    onSort: (SongSort) -> Unit,
    onShuffle: () -> Unit,
    onSearch: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onFolders: () -> Unit,
    onGenres: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit,
    onSleepTimer: () -> Unit,
    onSettings: () -> Unit,
    playlistContent: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LibraryTopBar(tab.title, onSearch, onFolders, onGenres, onQueue, onEqualizer, onSleepTimer, onSettings)
        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = AppBackground,
            divider = {},
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[tab.ordinal]),
                    color = PrimaryBlue
                )
            }
        ) {
            LibraryTab.entries.forEach { item ->
                Tab(
                    selected = tab == item,
                    onClick = { onTab(item) },
                    text = {
                        Text(
                            item.title,
                            color = if (tab == item) MaterialTheme.colorScheme.onSurface else TextMuted,
                            fontWeight = if (tab == item) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        when (tab) {
            LibraryTab.SONGS -> SongsList(
                songs, sort, shuffleEnabled, currentSongId, favoriteIds, onSort, onShuffle, onSong, onToggleFavorite, onArtist, onAlbum,
                onPlayNext, onAddToQueue, onAddToPlaylist
            )
            LibraryTab.ARTISTS -> ArtistsList(songs.asArtists(), onArtist)
            LibraryTab.ALBUMS -> AlbumsGrid(songs.asAlbums()) { onAlbum(it.songs.first()) }
            LibraryTab.PLAYLISTS -> playlistContent()
        }
    }
}

@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        DetailTopBar("Artista", onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(artist.artwork, 86.dp, sourceUri = artist.artworkUri)
            Spacer(Modifier.width(18.dp))
            Column {
                Text(artist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(songCount(artist.songs.size), color = TextMuted)
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
            items(artist.songs, key = { it.id }) { song ->
                SongRow(
                    song, currentSongId == song.id, song.id in favoriteIds,
                    { onSong(song, artist.songs) }, { onToggleFavorite(song) }, {}, { onAlbum(song) },
                    { onPlayNext(song) }, { onAddToQueue(song) }, { onAddToPlaylist(song) }
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    album: AlbumGroup,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        DetailTopBar("Álbum", onBack)
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArtworkImage(album.artwork, 184.dp, large = true, sourceUri = album.artworkUri)
            Spacer(Modifier.height(14.dp))
            Text(album.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${album.artist} • ${songCount(album.songs.size)}", color = TextMuted)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
            items(album.songs, key = { it.id }) { song ->
                SongRow(
                    song, currentSongId == song.id, song.id in favoriteIds,
                    { onSong(song, album.songs) }, { onToggleFavorite(song) }, { onArtist(song.artist) }, {},
                    { onPlayNext(song) }, { onAddToQueue(song) }, { onAddToPlaylist(song) }
                )
            }
        }
    }
}

@Composable
fun CollectionScreen(
    title: String,
    songs: List<Song>,
    emptyMessage: String,
    icon: ImageVector,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onSearch: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LibraryTopBar(title, onSearch = onSearch)
        if (songs.isEmpty()) {
            EmptyLibraryPage(title, emptyMessage, icon)
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song, currentSongId == song.id, song.id in favoriteIds,
                        { onSong(song, songs) }, { onToggleFavorite(song) }, { onArtist(song.artist) }, { onAlbum(song) },
                        { onPlayNext(song) }, { onAddToQueue(song) }, { onAddToPlaylist(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    songs: List<Song>,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalized = query.trim()
    val songResults = remember(songs, normalized) {
        if (normalized.isBlank()) emptyList() else songs.filter {
            it.title.contains(normalized, true) || it.artist.contains(normalized, true) || it.album.contains(normalized, true)
        }
    }
    val artistResults = remember(songs, normalized) {
        if (normalized.isBlank()) emptyList() else songs.asArtists().filter { it.name.contains(normalized, true) }
    }
    val albumResults = remember(songs, normalized) {
        if (normalized.isBlank()) emptyList() else songs.asAlbums().filter { it.name.contains(normalized, true) || it.artist.contains(normalized, true) }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Músicas, artistas e álbuns") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(18.dp)
            )
        }
        when {
            normalized.isBlank() -> EmptyLibraryPage("Pesquisar", "Digite para buscar na biblioteca offline.", Icons.Default.Search)
            songResults.isEmpty() && artistResults.isEmpty() && albumResults.isEmpty() -> EmptyLibraryPage("Nenhum resultado", "Tente outro nome de música, artista ou álbum.", Icons.Default.Search)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if (songResults.isNotEmpty()) {
                    item { SectionTitle("Músicas") }
                    items(songResults, key = { "song:${it.id}" }) { song ->
                        SongRow(
                            song, currentSongId == song.id, song.id in favoriteIds,
                            { onSong(song, songResults) }, { onToggleFavorite(song) }, { onArtist(song.artist) }, { onAlbum(song) },
                            { onPlayNext(song) }, { onAddToQueue(song) }, { onAddToPlaylist(song) }
                        )
                    }
                }
                if (artistResults.isNotEmpty()) {
                    item { SectionTitle("Artistas") }
                    items(artistResults, key = { "artist:${it.name}" }) { artist -> ArtistResultRow(artist, onArtist) }
                }
                if (albumResults.isNotEmpty()) {
                    item { SectionTitle("Álbuns") }
                    items(albumResults, key = { "album:${it.key}" }) { album -> AlbumResultRow(album) { onAlbum(album.songs.first()) } }
                }
            }
        }
    }
}

@Composable
private fun SongsList(
    songs: List<Song>,
    sort: SongSort,
    shuffleEnabled: Boolean,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onSort: (SongSort) -> Unit,
    onShuffle: () -> Unit,
    onSong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyLibraryPage("Sua biblioteca está vazia", "Nenhuma música foi encontrada no dispositivo.", Icons.AutoMirrored.Filled.QueueMusic)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 10.dp)) {
        item { ShuffleAndSortRow(sort, shuffleEnabled, onSort, onShuffle) }
        items(songs, key = { it.id }) { song ->
            SongRow(
                song, currentSongId == song.id, song.id in favoriteIds,
                { onSong(song, songs) }, { onToggleFavorite(song) }, { onArtist(song.artist) }, { onAlbum(song) },
                { onPlayNext(song) }, { onAddToQueue(song) }, { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun ShuffleAndSortRow(sort: SongSort, shuffleEnabled: Boolean, onSort: (SongSort) -> Unit, shuffle: () -> Unit) {
    var sortMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(onClick = shuffle).padding(horizontal = 12.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shuffle, null, tint = if (shuffleEnabled) Color.White else PrimaryBlue)
            Spacer(Modifier.width(14.dp))
            Text("Ordem aleatória", fontWeight = FontWeight.Medium)
        }
        Box {
            Row(Modifier.clickable { sortMenu = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(sort.label, color = TextMuted, fontSize = 12.sp)
                Icon(Icons.Default.ArrowDropDown, "Ordenar", tint = TextMuted)
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                SongSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, color = if (option == sort) PrimaryBlue else Color.Unspecified) },
                        onClick = { sortMenu = false; onSort(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsList(artists: List<ArtistGroup>, onArtist: (String) -> Unit) {
    if (artists.isEmpty()) {
        EmptyLibraryPage("Artistas", "Nenhum artista encontrado.", Icons.Default.Person)
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(artists, key = { it.name }) { artist ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onArtist(artist.name) },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtworkImage(artist.artwork, 66.dp, sourceUri = artist.artworkUri)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(artist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(songCount(artist.songs.size), color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsGrid(albums: List<AlbumGroup>, onAlbum: (AlbumGroup) -> Unit) {
    if (albums.isEmpty()) {
        EmptyLibraryPage("Álbuns", "Nenhum álbum encontrado.", Icons.Default.Album)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(albums, key = { it.key }) { album ->
            Column(Modifier.fillMaxWidth().clickable { onAlbum(album) }) {
                Box(Modifier.fillMaxWidth().background(SurfaceRaised, RoundedCornerShape(16.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
                    ArtworkImage(album.artwork, 142.dp, large = true, sourceUri = album.artworkUri)
                }
                Spacer(Modifier.height(8.dp))
                Text(album.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(songCount(album.songs.size), color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LibraryTopBar(
    title: String,
    onSearch: () -> Unit,
    onFolders: (() -> Unit)? = null,
    onGenres: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onEqualizer: (() -> Unit)? = null,
    onSleepTimer: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    var moreOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        JukeHorizontalLogo(Modifier.width(88.dp).height(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onSearch) { Icon(Icons.Default.Search, "Pesquisar") }
        if (onFolders != null && onGenres != null && onQueue != null) Box {
            IconButton({ moreOpen = true }) { Icon(Icons.Default.MoreVert, "Mais opções da biblioteca") }
            DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                DropdownMenuItem(text = { Text("Pastas") }, leadingIcon = { Icon(Icons.Default.Folder, null) }, onClick = { moreOpen = false; onFolders() })
                DropdownMenuItem(text = { Text("Gêneros") }, leadingIcon = { Icon(Icons.Default.Category, null) }, onClick = { moreOpen = false; onGenres() })
                DropdownMenuItem(text = { Text("Fila de reprodução") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }, onClick = { moreOpen = false; onQueue() })
                if (onEqualizer != null) DropdownMenuItem(text = { Text("Equalizador") }, leadingIcon = { Icon(Icons.Default.GraphicEq, null) }, onClick = { moreOpen = false; onEqualizer() })
                if (onSleepTimer != null) DropdownMenuItem(text = { Text("Timer para dormir") }, leadingIcon = { Icon(Icons.Default.Bedtime, null) }, onClick = { moreOpen = false; onSleepTimer() })
                if (onSettings != null) DropdownMenuItem(text = { Text("Configurações") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { moreOpen = false; onSettings() })
            }
        }
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArtistResultRow(artist: ArtistGroup, open: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { open(artist.name) }.padding(horizontal = 20.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtworkImage(artist.artwork, 52.dp, sourceUri = artist.artworkUri)
        Spacer(Modifier.width(14.dp))
        Column { Text(artist.name, fontWeight = FontWeight.SemiBold); Text(songCount(artist.songs.size), color = TextMuted, fontSize = 12.sp) }
    }
}

@Composable
private fun AlbumResultRow(album: AlbumGroup, open: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = open).padding(horizontal = 20.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtworkImage(album.artwork, 52.dp, sourceUri = album.artworkUri)
        Spacer(Modifier.width(14.dp))
        Column { Text(album.name, fontWeight = FontWeight.SemiBold); Text("${album.artist} • ${songCount(album.songs.size)}", color = TextMuted, fontSize = 12.sp) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp), color = PrimaryBlue, fontWeight = FontWeight.Bold)
}

@Composable
fun EmptyLibraryPage(title: String, message: String, icon: ImageVector) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(message, color = TextMuted)
            }
        }
    }
}

private fun songCount(count: Int) = if (count == 1) "1 música" else "$count músicas"
