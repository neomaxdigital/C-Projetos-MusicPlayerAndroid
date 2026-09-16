package com.musicplayer.offline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.offline.music.LocalPlaylist
import com.musicplayer.offline.music.Song
import android.net.Uri
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(
    playlists: List<LocalPlaylist>,
    songs: List<Song>,
    onCreate: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val songsById = remember(songs) { songs.associateBy { it.id } }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<LocalPlaylist?>(null) }
    var deleting by remember { mutableStateOf<LocalPlaylist?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Suas playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button({ creating = true }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Nova")
            }
        }
        if (playlists.isEmpty()) {
            EmptyLibraryPage("Playlists", "Crie uma playlist para organizar suas músicas.", Icons.AutoMirrored.Filled.PlaylistPlay)
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    val representative = playlist.songIds.firstNotNullOfOrNull(songsById::get)
                    PlaylistCard(
                        playlist = playlist,
                        artwork = representative?.artwork,
                        artworkUri = representative?.uri,
                        onOpen = { onOpen(playlist.id) },
                        onRename = { renaming = playlist },
                        onDelete = { deleting = playlist }
                    )
                }
            }
        }
    }
    if (creating) NamePlaylistDialog("Nova playlist", "Criar", onDismiss = { creating = false }) {
        creating = false
        onCreate(it)
    }
    renaming?.let { playlist ->
        NamePlaylistDialog("Renomear playlist", "Salvar", playlist.name, onDismiss = { renaming = null }) {
            renaming = null
            onRename(playlist.id, it)
        }
    }
    deleting?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir playlist?") },
            text = { Text("A playlist “${playlist.name}” será excluída. As músicas continuarão no dispositivo.") },
            confirmButton = { TextButton({ deleting = null; onDelete(playlist.id) }) { Text("Excluir") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: LocalPlaylist,
    songs: List<Song>,
    currentSongId: Long?,
    shuffleEnabled: Boolean,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onTogglePlay: (List<Song>) -> Unit,
    onToggleShuffle: (List<Song>) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onRemove: (Long) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val songsById = remember(songs) { songs.associateBy { it.id } }
    var orderedIds by remember(playlist.id) { mutableStateOf(playlist.songIds) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val latestOrderedIds by rememberUpdatedState(orderedIds)
    LaunchedEffect(playlist.songIds, draggedId) {
        if (draggedId == null && orderedIds != playlist.songIds) orderedIds = playlist.songIds
    }
    val playlistSongs = orderedIds.mapNotNull(songsById::get)
    val cover = playlistSongs.firstOrNull()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item(key = "playlist_header") {
            PlaylistHero(
                playlist = playlist,
                songs = playlistSongs,
                cover = cover,
                shuffleEnabled = shuffleEnabled,
                isPlaying = isPlaying,
                onBack = onBack,
                onToggleShuffle = onToggleShuffle,
                onTogglePlay = onTogglePlay
            )
        }
        if (playlistSongs.isEmpty()) {
            item(key = "empty_playlist") {
                EmptyLibraryPage("Playlist vazia", "Adicione músicas pelo menu de três pontos.", Icons.AutoMirrored.Filled.PlaylistPlay)
            }
        } else {
            itemsIndexed(playlistSongs, key = { _, song -> "$SONG_KEY_PREFIX${song.id}" }) { _, song ->
                val dragging = draggedId == song.id
                Row(
                    Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .shadow(if (dragging) 7.dp else 0.dp, RoundedCornerShape(14.dp))
                        .background(if (dragging) SurfaceRaised else AppBackground, RoundedCornerShape(14.dp))
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            alpha = if (dragging) 0.9f else 1f
                            scaleX = if (dragging) 1.015f else 1f
                            scaleY = if (dragging) 1.015f else 1f
                        }
                        .pointerInput(song.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = song.id
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = { draggedId = null; dragOffset = 0f },
                                onDragEnd = { draggedId = null; dragOffset = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val visible = listState.layoutInfo.visibleItemsInfo
                                    val active = visible.firstOrNull { it.key == "$SONG_KEY_PREFIX${song.id}" }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val activeCenter = active.offset + active.size / 2 + dragOffset
                                    val target = visible
                                        .filter { it.key.toString().startsWith(SONG_KEY_PREFIX) && it.key != active.key }
                                        .minByOrNull { kotlin.math.abs(activeCenter - (it.offset + it.size / 2)) }
                                    if (target != null && activeCenter >= target.offset && activeCenter <= target.offset + target.size) {
                                        val targetId = target.key.toString().removePrefix(SONG_KEY_PREFIX).toLongOrNull()
                                        val from = latestOrderedIds.indexOf(song.id)
                                        val to = latestOrderedIds.indexOf(targetId)
                                        if (from >= 0 && to >= 0 && from != to) {
                                            orderedIds = latestOrderedIds.toMutableList().apply { add(to, removeAt(from)) }
                                            dragOffset += (active.offset - target.offset).toFloat()
                                            onMove(from, to)
                                        }
                                    }
                                    val viewport = listState.layoutInfo.viewportEndOffset
                                    when {
                                        activeCenter < listState.layoutInfo.viewportStartOffset + 120 -> scope.launch { listState.scrollBy(-24f) }
                                        activeCenter > viewport - 120 -> scope.launch { listState.scrollBy(24f) }
                                    }
                                }
                            )
                        }
                        .clickable { if (draggedId == null) onPlaySong(song, playlistSongs) }
                        .padding(start = 8.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DragHandle, "Segure e arraste para reordenar", tint = TextMuted, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    ArtworkImage(song.artwork, 54.dp, sourceUri = song.uri)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = if (song.id == currentSongId) PrimaryBlue else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(song.artist, song.album).filter { it.isNotBlank() }.distinct().joinToString(" • "), color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton({ onRemove(song.id) }) { Icon(Icons.Default.Delete, "Remover da playlist", tint = TextMuted) }
                }
            }
        }
    }
}

@Composable
private fun PlaylistHero(
    playlist: LocalPlaylist,
    songs: List<Song>,
    cover: Song?,
    shuffleEnabled: Boolean,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onToggleShuffle: (List<Song>) -> Unit,
    onTogglePlay: (List<Song>) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            Spacer(Modifier.weight(1f))
        }
        Box(
            Modifier.size(176.dp).shadow(12.dp, RoundedCornerShape(26.dp)).background(SurfaceRaised, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) ArtworkImage(cover.artwork, 176.dp, large = true, sourceUri = cover.uri)
            else Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = PrimaryBlue, modifier = Modifier.size(72.dp))
        }
        Text(playlist.name, modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(songCountLabel(songs.size), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 5.dp), color = TextMuted)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                { onToggleShuffle(songs) },
                enabled = songs.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(contentColor = if (shuffleEnabled) Color.White else MaterialTheme.colorScheme.onPrimary)
            ) {
                Icon(Icons.Default.Shuffle, null)
                Spacer(Modifier.width(7.dp))
                Text("Aleatório")
            }
            Button(
                { onTogglePlay(songs) },
                enabled = songs.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(contentColor = if (isPlaying) Color.White else MaterialTheme.colorScheme.onPrimary)
            ) {
                Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, if (isPlaying) "Parar reprodução" else null)
                Spacer(Modifier.width(7.dp))
                Text("Reproduzir")
            }
        }
    }
}

private const val SONG_KEY_PREFIX = "playlist_song_"

@Composable
fun PlaylistPickerDialog(
    song: Song,
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar à playlist") },
        text = {
            Column {
                Text(song.title, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                playlists.forEach { playlist ->
                    val alreadyAdded = song.id in playlist.songIds
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !alreadyAdded) { onAdd(playlist.id) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = if (alreadyAdded) TextMuted else PrimaryBlue)
                        Spacer(Modifier.width(12.dp))
                        Text(playlist.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (alreadyAdded) Text("Adicionada", color = TextMuted, fontSize = 11.sp)
                    }
                }
                if (playlists.isNotEmpty()) Spacer(Modifier.height(8.dp))
                OutlinedTextField(newName, { newName = it }, label = { Text("Nova playlist") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton({ onCreateAndAdd(newName) }, enabled = newName.isNotBlank()) { Text("Criar e adicionar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun PlaylistCard(playlist: LocalPlaylist, artwork: android.graphics.Bitmap?, artworkUri: Uri?, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(artwork, 66.dp, sourceUri = artworkUri)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(songCountLabel(playlist.songIds.size), color = TextMuted, fontSize = 12.sp)
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Opções da playlist") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Renomear") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Excluir") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun NamePlaylistDialog(title: String, action: String, initial: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true) },
        confirmButton = { TextButton({ onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(action) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

private fun songCountLabel(count: Int) = if (count == 1) "1 música" else "$count músicas"
