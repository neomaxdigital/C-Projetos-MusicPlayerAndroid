package com.musicplayer.offline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.musicplayer.offline.music.Song
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class QueueEntry(
    val stableKey: String,
    val song: Song,
    val current: Boolean
)

@Composable
fun QueueScreen(player: Player?, songs: List<Song>, revision: Int, onBack: () -> Unit) {
    val songsById = remember(songs) { songs.associateBy { it.id } }
    val currentIndex = player?.currentMediaItemIndex?.coerceAtLeast(0) ?: 0

    val sourceEntries = remember(player, songsById, revision, currentIndex) {
        if (player == null) {
            emptyList()
        } else {
            (currentIndex until player.mediaItemCount).mapNotNull { index ->
                val id = player.getMediaItemAt(index).mediaId.toLongOrNull()
                songsById[id]?.let { song ->
                    QueueEntry(
                        stableKey = "queue_\${index}_\${song.id}",
                        song = song,
                        current = index == currentIndex
                    )
                }
            }
        }
    }

    var queueBaseIndex by remember(player) { mutableIntStateOf(currentIndex) }
    var orderedEntries by remember(player) { mutableStateOf(sourceEntries) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val latestEntries by rememberUpdatedState(orderedEntries)

    LaunchedEffect(sourceEntries, currentIndex, draggedKey) {
        if (draggedKey == null) {
            queueBaseIndex = currentIndex
            orderedEntries = sourceEntries
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                SimpleBackHeader("Fila de reprodução", onBack)
            }
            TextButton(
                onClick = { clearQueueKeepingCurrent(player) },
                enabled = orderedEntries.size > 1,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.ClearAll, null)
                Spacer(Modifier.width(4.dp))
                Text("Limpar")
            }
        }

        if (orderedEntries.isEmpty()) {
            EmptyLibraryPage(
                "Fila vazia",
                "Escolha uma música para iniciar a fila.",
                Icons.AutoMirrored.Filled.QueueMusic
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(
                    orderedEntries,
                    key = { _, entry -> entry.stableKey }
                ) { queuePosition, entry ->
                    val dragging = draggedKey == entry.stableKey

                    Row(
                        Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .shadow(if (dragging) 7.dp else 0.dp, RoundedCornerShape(14.dp))
                            .background(
                                if (dragging) SurfaceRaised else AppBackground,
                                RoundedCornerShape(14.dp)
                            )
                            .graphicsLayer {
                                translationY = if (dragging) dragOffset else 0f
                                alpha = if (dragging) 0.9f else 1f
                                scaleX = if (dragging) 1.015f else 1f
                                scaleY = if (dragging) 1.015f else 1f
                            }
                            .pointerInput(entry.stableKey, entry.current) {
                                if (!entry.current) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedKey = entry.stableKey
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragCancel = {
                                            draggedKey = null
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggedKey = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y

                                            val visible = listState.layoutInfo.visibleItemsInfo
                                            val active = visible.firstOrNull { it.key == entry.stableKey }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val activeCenter = active.offset + active.size / 2 + dragOffset

                                            val target = visible
                                                .filter { candidate ->
                                                    candidate.key != active.key &&
                                                        latestEntries.indexOfFirst {
                                                            it.stableKey == candidate.key
                                                        } > 0
                                                }
                                                .minByOrNull {
                                                    abs(activeCenter - (it.offset + it.size / 2))
                                                }

                                            if (
                                                target != null &&
                                                activeCenter >= target.offset &&
                                                activeCenter <= target.offset + target.size
                                            ) {
                                                val from = latestEntries.indexOfFirst {
                                                    it.stableKey == entry.stableKey
                                                }
                                                val to = latestEntries.indexOfFirst {
                                                    it.stableKey == target.key
                                                }

                                                if (from > 0 && to > 0 && from != to) {
                                                    player?.moveMediaItem(
                                                        queueBaseIndex + from,
                                                        queueBaseIndex + to
                                                    )
                                                    orderedEntries = latestEntries.toMutableList().apply {
                                                        add(to, removeAt(from))
                                                    }
                                                    dragOffset += (active.offset - target.offset).toFloat()
                                                }
                                            }

                                            val viewportStart = listState.layoutInfo.viewportStartOffset
                                            val viewportEnd = listState.layoutInfo.viewportEndOffset
                                            when {
                                                activeCenter < viewportStart + 120 ->
                                                    scope.launch { listState.scrollBy(-24f) }
                                                activeCenter > viewportEnd - 120 ->
                                                    scope.launch { listState.scrollBy(24f) }
                                            }
                                        }
                                    )
                                }
                            }
                            .clickable {
                                if (draggedKey == null) {
                                    player?.seekToDefaultPosition(queueBaseIndex + queuePosition)
                                    player?.play()
                                }
                            }
                            .padding(start = 8.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            if (entry.current) "Faixa atual" else "Segure e arraste para reordenar",
                            tint = if (entry.current) TextMuted.copy(alpha = 0.45f) else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
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
                            Text(
                                if (entry.current) "Tocando agora" else entry.song.artist,
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton({
                            player?.removeMediaItem(queueBaseIndex + queuePosition)
                        }) {
                            Icon(Icons.Default.Delete, "Remover da fila", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
