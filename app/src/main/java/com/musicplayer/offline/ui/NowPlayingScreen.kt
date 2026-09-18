package com.musicplayer.offline.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Size
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.musicplayer.offline.R
import com.musicplayer.offline.music.AudioFileSupport
import com.musicplayer.offline.music.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NowPlayingScreen(
    song: Song?,
    player: Player?,
    position: Long,
    duration: Long,
    playing: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    favorite: Boolean,
    toggleFavorite: () -> Unit,
    openQueue: () -> Unit,
    openEqualizer: () -> Unit = {},
    openLyrics: () -> Unit = {},
    requestConversion: (Song, Boolean) -> Unit = { _, _ -> },
    back: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val navigationBarInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val horizontalPadding = if (maxWidth < 360.dp) 14.dp else 22.dp
    val usableHeight = (maxHeight - 64.dp - navigationBarInset).coerceAtLeast(420.dp)
    val heightFraction = ((usableHeight.value - 500f) / 300f).coerceIn(0f, 1f)
    val compact = usableHeight < 620.dp

    // Responsive metrics: preserve the current visual hierarchy, but scale the elements
    // that consume vertical space so playback controls stay above the system bar on
    // short phones and still look balanced on tall phones.
    val artMaxByWidth = (maxWidth - horizontalPadding * 2).coerceAtLeast(150.dp)
    val artTarget = adaptiveDp(170.dp, 340.dp, heightFraction)
    val artSize = minOf(artMaxByWidth, artTarget, 420.dp)
    val topGap = adaptiveDp(6.dp, 18.dp, heightFraction)
    val artworkTextGap = adaptiveDp(12.dp, 24.dp, heightFraction)
    val titleSize = adaptiveSp(22f, 28f, heightFraction)
    val titleLineHeight = adaptiveSp(26f, 32f, heightFraction)
    val artistSize = adaptiveSp(14f, 17f, heightFraction)
    val actionsGap = adaptiveDp(10.dp, 22.dp, heightFraction)
    val controlsGap = adaptiveDp(8.dp, 18.dp, heightFraction)
    val playButtonSize = adaptiveDp(68.dp, 84.dp, heightFraction)
    val bottomGap = adaptiveDp(6.dp, 14.dp, heightFraction)
    var selectedTab by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    val item = player?.currentMediaItem
    val title = song?.title ?: item?.mediaMetadata?.title?.toString() ?: "Selecione uma música"
    val artist = song?.artist ?: item?.mediaMetadata?.artist?.toString().orEmpty()
    val share: () -> Unit = {
        song?.let {
            if (AudioFileSupport.isWav(it)) requestConversion(it, true) else shareAudio(context, it)
        }
        Unit
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(back) {
                Icon(Icons.Default.KeyboardArrowDown, "Fechar", modifier = Modifier.size(36.dp))
            }
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                divider = {},
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = PrimaryBlue
                    )
                }
            ) {
                listOf("Música", "Letra").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onBackground else TextMuted,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            Box {
                IconButton({ menuOpen = true }) { Icon(Icons.Default.MoreVert, "Mais opções") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Compartilhar") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        enabled = song != null,
                        onClick = { menuOpen = false; share() }
                    )
                    song?.takeIf(AudioFileSupport::isWav)?.let { wavSong ->
                        DropdownMenuItem(
                            text = { Text("Converter para MP3") },
                            leadingIcon = { Icon(Icons.Default.Sync, null) },
                            onClick = { menuOpen = false; requestConversion(wavSong, false) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Equalizador") },
                        leadingIcon = { Icon(Icons.Default.Tune, null) },
                        onClick = { menuOpen = false; openEqualizer() }
                    )
                    DropdownMenuItem(
                        text = { Text("Fila") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                        onClick = { menuOpen = false; openQueue() }
                    )
                }
            }
        }

        if (selectedTab == 0) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = navigationBarInset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(topGap))
                NowPlayingArtwork(song = song, size = artSize)
                Spacer(Modifier.height(artworkTextGap))
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (artist.isNotBlank()) {
                    Text(
                        artist,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = TextMuted,
                        fontSize = artistSize,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(actionsGap))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NowPlayingAction(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Curtir",
                        active = favorite,
                        enabled = song != null || item != null,
                        onClick = toggleFavorite
                    )
                    NowPlayingAction(Icons.Default.Tune, "Equalizador", onClick = openEqualizer)
                    NowPlayingAction(Icons.Default.Share, "Compartilhar", enabled = song != null, onClick = share)
                    NowPlayingAction(Icons.AutoMirrored.Filled.QueueMusic, "Fila", onClick = openQueue)
                }
                Spacer(Modifier.height(actionsGap))
                PlaybackProgress(player, position, duration)
                Spacer(Modifier.height(controlsGap))
                PlaybackControls(player, playing, shuffleEnabled, repeatMode, playButtonSize)
                Spacer(Modifier.height(bottomGap))
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = navigationBarInset)
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) { LyricsContent(song, position) }
                PlaybackProgress(player, position, duration)
                Spacer(Modifier.height(8.dp))
                PlaybackControls(player, playing, shuffleEnabled, repeatMode, adaptiveDp(64.dp, 76.dp, heightFraction))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun NowPlayingArtwork(song: Song?, size: Dp) {
    val context = LocalContext.current
    val pixelSize = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    var artwork by remember(song?.id, pixelSize) { mutableStateOf(song?.artwork) }
    var resolved by remember(song?.id, pixelSize) { mutableStateOf(song?.artwork != null) }

    LaunchedEffect(song?.id, pixelSize) {
        artwork = song?.artwork ?: song?.uri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(uri, Size(pixelSize, pixelSize), null)
                }.getOrNull()
            }
        }
        resolved = true
    }

    val shape = RoundedCornerShape(22.dp)
    when {
        artwork != null -> Image(
            bitmap = artwork!!.asImageBitmap(),
            contentDescription = "Capa de ${song?.title.orEmpty()}",
            modifier = Modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop
        )
        resolved -> TurntableArtwork(size = size)
        else -> Box(Modifier.size(size).clip(shape).background(SurfaceRaised))
    }
}

@Composable
private fun TurntableArtwork(size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.juke_turntable_static),
            contentDescription = "Toca-discos JUKE",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun RowScope.NowPlayingAction(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val iconColor = when {
        !enabled -> TextMuted.copy(alpha = .40f)
        active -> PrimaryBlue
        else -> PrimaryBlue
    }
    val labelColor = if (enabled) TextMuted else TextMuted.copy(alpha = .40f)

    Column(
        Modifier.weight(1f).clickable(enabled = enabled, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = iconColor, modifier = Modifier.size(30.dp))
        Text(
            label,
            color = labelColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaybackProgress(player: Player?, position: Long, duration: Long) {
    val activeTrack = PrimaryBlue
    val inactiveTrack = JukePrimaryContainer.copy(alpha = .78f)

    Slider(
        value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
        onValueChange = { player?.seekTo((it * duration).toLong()) },
        enabled = duration > 0,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = activeTrack,
            inactiveTrackColor = inactiveTrack
        ),
        track = { sliderState ->
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                val centerY = size.height / 2f
                drawLine(
                    color = inactiveTrack,
                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeTrack,
                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width * sliderState.value, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
            }
        },
        thumb = {
            Box(
                Modifier
                    .size(20.dp)
                    .shadow(5.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(2.dp, PrimaryBlue.copy(alpha = .75f), CircleShape)
            )
        }
    )
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(formatTime(position), color = TextMuted, fontSize = 12.sp)
        Text(formatTime(duration), color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PlaybackControls(player: Player?, playing: Boolean, shuffleEnabled: Boolean, repeatMode: Int, playSize: Dp) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        IconButton({ player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }) {
            Icon(Icons.Default.Shuffle, "Aleatório", tint = if (shuffleEnabled) PrimaryBlue else TextMuted)
        }
        IconButton({ player?.seekToPreviousMediaItem() }, Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(36.dp))
        }
        IconButton(
            {
                player?.let {
                    if (playing) it.pause() else {
                        if (it.playbackState == Player.STATE_IDLE) it.prepare()
                        it.play()
                    }
                }
            },
            Modifier
                .size(playSize)
                .clip(CircleShape)
                .border(1.dp, PrimaryBlue, CircleShape)
                .background(SurfaceDark)
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (playing) "Pausar" else "Tocar",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
        IconButton({ player?.seekToNextMediaItem() }, Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipNext, "Próxima", tint = Color.White, modifier = Modifier.size(36.dp))
        }
        IconButton({
            player?.let {
                it.repeatMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }) {
            Icon(
                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                "Repetir",
                tint = if (repeatMode == Player.REPEAT_MODE_OFF) TextMuted else PrimaryBlue
            )
        }
    }
}

private fun adaptiveDp(min: Dp, max: Dp, fraction: Float): Dp =
    min + (max - min) * fraction.coerceIn(0f, 1f)

private fun adaptiveSp(min: Float, max: Float, fraction: Float) =
    (min + (max - min) * fraction.coerceIn(0f, 1f)).sp

private fun shareAudio(context: Context, song: Song) {
    runCatching {
        require(song.uri.scheme == "content") { "URI de áudio não compartilhável" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = song.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, song.uri)
            clipData = ClipData.newUri(context.contentResolver, song.title, song.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar música"))
    }.onFailure {
        Toast.makeText(context, "Não foi possível compartilhar esta música.", Toast.LENGTH_SHORT).show()
    }
}
