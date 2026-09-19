package com.musicplayer.offline.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.musicplayer.offline.music.Song
import com.musicplayer.offline.music.AudioFileSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ArtworkCache {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun load(context: android.content.Context, uri: Uri, pixelSize: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$pixelSize"
        cache.get(key) ?: runCatching {
            context.contentResolver.loadThumbnail(uri, Size(pixelSize, pixelSize), null)
        }.getOrNull()?.also { cache.put(key, it) }
    }
}

val LocalConversionRequest = staticCompositionLocalOf<(Song, Boolean) -> Unit> { { _, _ -> } }

@Composable
fun ArtworkImage(artwork: Bitmap?, size: Dp, large: Boolean = false, modifier: Modifier = Modifier, sourceUri: Uri? = null) {
    val shape = RoundedCornerShape(if (large) 22.dp else 10.dp)
    val context = LocalContext.current
    val pixelSize = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    var loaded by remember(artwork, sourceUri, pixelSize) { mutableStateOf(artwork) }
    LaunchedEffect(artwork, sourceUri, pixelSize) {
        loaded = artwork ?: sourceUri?.let { ArtworkCache.load(context, it, pixelSize) }
    }
    if (loaded != null) {
        Image(loaded!!.asImageBitmap(), null, modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
    } else {
        Box(
            modifier.size(size).clip(shape).background(SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Album,
                null,
                tint = PrimaryBlue,
                modifier = Modifier.size(if (large) size / 3 else 26.dp)
            )
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArtist: () -> Unit,
    onAlbum: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onRemoveFromLibrary: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    val requestConversion = LocalConversionRequest.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(song.artwork, LocalListArtworkSize.current, sourceUri = song.uri)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton({ menuOpen = true }) {
                Icon(Icons.Default.MoreVert, "Opções de ${song.title}", tint = TextMuted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Tocar a seguir") },
                    leadingIcon = { Icon(Icons.Default.SkipNext, null) },
                    onClick = { menuOpen = false; onPlayNext() }
                )
                DropdownMenuItem(
                    text = { Text("Adicionar ao final da fila") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                    onClick = { menuOpen = false; onAddToQueue() }
                )
                DropdownMenuItem(
                    text = { Text("Adicionar à playlist") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                    onClick = { menuOpen = false; onAddToPlaylist() }
                )
                DropdownMenuItem(
                    text = { Text(if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos") },
                    leadingIcon = { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) },
                    onClick = { menuOpen = false; onToggleFavorite() }
                )
                DropdownMenuItem(
                    text = { Text("Abrir artista") },
                    onClick = { menuOpen = false; onArtist() }
                )
                DropdownMenuItem(
                    text = { Text("Abrir álbum") },
                    onClick = { menuOpen = false; onAlbum() }
                )
                if (AudioFileSupport.isWav(song)) {
                    DropdownMenuItem(
                        text = { Text("Converter para MP3") },
                        leadingIcon = { Icon(Icons.Default.Sync, null) },
                        onClick = { menuOpen = false; requestConversion(song, false) }
                    )
                }
                if (onRemoveFromLibrary != null) {
                    DropdownMenuItem(
                        text = { Text("Remover") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = {
                            menuOpen = false
                            onRemoveFromLibrary()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(song: Song?, player: Player?, playing: Boolean, open: () -> Unit) {
    val item = player?.currentMediaItem
    val title = song?.title ?: item?.mediaMetadata?.title?.toString() ?: "Selecione uma música"
    val artist = song?.artist ?: item?.mediaMetadata?.artist?.toString().orEmpty()
    Surface(
        color = SurfaceDark,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth().clickable(enabled = item != null || song != null, onClick = open)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkImage(song?.artwork, 46.dp, sourceUri = song?.uri)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(artist, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ if (playing) player?.pause() else player?.play() }, enabled = item != null) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pausar" else "Tocar")
            }
            IconButton({ player?.seekToNextMediaItem() }, enabled = (player?.mediaItemCount ?: 0) > 1) {
                Icon(Icons.Default.SkipNext, "Próxima")
            }
        }
    }
}

@Composable
fun AppBottomBar(selected: Destination, select: (Destination) -> Unit) {
    NavigationBar(Modifier.navigationBarsPadding(), containerColor = SurfaceDark) {
        listOf(
            Triple(Destination.HOME, Icons.Default.Home, "Início"),
            Triple(Destination.FAVORITES, Icons.Default.FavoriteBorder, "Favoritos"),
            Triple(Destination.RECENT, Icons.Default.History, "Recentes")
        ).forEach { (destination, icon, label) ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { select(destination) },
                icon = { Icon(icon, label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    indicatorColor = SurfaceRaised,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}
