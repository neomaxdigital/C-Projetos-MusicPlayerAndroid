package com.musicplayer.offline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.musicplayer.offline.music.Song

private data class QueueEntry(val playerIndex: Int, val song: Song, val current: Boolean)

@Composable
fun QueueScreen(player: Player?, songs: List<Song>, revision: Int, onBack: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val timelineRevision = revision
    val songsById = remember(songs) { songs.associateBy { it.id } }
    val currentIndex = player?.currentMediaItemIndex?.coerceAtLeast(0) ?: 0
    val entries = if (player == null) emptyList() else (currentIndex until player.mediaItemCount).mapNotNull { index ->
        val id = player.getMediaItemAt(index).mediaId.toLongOrNull()
        songsById[id]?.let { QueueEntry(index, it, index == currentIndex) }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { SimpleBackHeader("Fila de reprodução", onBack) }
            TextButton(
                onClick = { clearQueueKeepingCurrent(player) },
                enabled = entries.size > 1,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.ClearAll, null)
                Spacer(Modifier.width(4.dp))
                Text("Limpar")
            }
        }
        if (entries.isEmpty()) {
            EmptyLibraryPage("Fila vazia", "Escolha uma música para iniciar a fila.", Icons.AutoMirrored.Filled.QueueMusic)
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                items(entries, key = { "${it.playerIndex}:${it.song.id}" }) { entry ->
                    val queuePosition = entries.indexOf(entry)
                    Row(
                        Modifier.fillMaxWidth().clickable { player?.seekToDefaultPosition(entry.playerIndex); player?.play() }
                            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkImage(entry.song.artwork, 50.dp, sourceUri = entry.song.uri)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.song.title,
                                color = if (entry.current) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (entry.current) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(if (entry.current) "Tocando agora" else entry.song.artist, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(
                            { player?.moveMediaItem(entry.playerIndex, entry.playerIndex - 1) },
                            enabled = !entry.current && queuePosition > 1
                        ) { Icon(Icons.Default.ArrowUpward, "Mover para cima", tint = TextMuted) }
                        IconButton(
                            { player?.moveMediaItem(entry.playerIndex, entry.playerIndex + 1) },
                            enabled = !entry.current && queuePosition < entries.lastIndex
                        ) { Icon(Icons.Default.ArrowDownward, "Mover para baixo", tint = TextMuted) }
                        IconButton({ player?.removeMediaItem(entry.playerIndex) }) {
                            Icon(Icons.Default.Delete, "Remover da fila", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
