package com.musicplayer.offline.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Size
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.isActive
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
    val compact = maxHeight < 720.dp
    val horizontalPadding = if (maxWidth < 360.dp) 14.dp else 22.dp
    val artSize = minOf(maxWidth - horizontalPadding * 2, maxHeight * if (compact) .30f else .38f)
        .coerceIn(if (compact) 150.dp else 190.dp, 420.dp)
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
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (compact) 8.dp else 18.dp))
                NowPlayingArtwork(song = song, size = artSize, playing = playing)
                Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = if (compact) 24.sp else 28.sp,
                    lineHeight = if (compact) 28.sp else 32.sp,
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
                        fontSize = if (compact) 15.sp else 17.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
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
                Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
                PlaybackProgress(player, position, duration)
                Spacer(Modifier.height(if (compact) 8.dp else 18.dp))
                PlaybackControls(player, playing, shuffleEnabled, repeatMode, if (compact) 70.dp else 84.dp)
                Spacer(Modifier.height(18.dp))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = horizontalPadding)) {
                Box(Modifier.weight(1f).fillMaxWidth()) { LyricsContent(song, position) }
                PlaybackProgress(player, position, duration)
                Spacer(Modifier.height(8.dp))
                PlaybackControls(player, playing, shuffleEnabled, repeatMode, if (compact) 66.dp else 76.dp)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun NowPlayingArtwork(song: Song?, size: Dp, playing: Boolean) {
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
        resolved -> TurntableArtwork(size = size, playing = playing && song != null)
        else -> Box(Modifier.size(size).clip(shape).background(SurfaceRaised))
    }
}

@Composable
private fun TurntableArtwork(size: Dp, playing: Boolean) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 9_000, easing = LinearEasing)
                )
                rotation.snapTo(rotation.value % 360f)
            }
        } else {
            rotation.stop()
        }
    }

    val painter = painterResource(R.drawable.juke_turntable)
    val discSize = size * .72f
    val discLeft = (size - discSize) / 2
    val discTop = discLeft + size * .035f

    Box(Modifier.size(size).clip(RoundedCornerShape(22.dp))) {
        Image(
            painter = painter,
            contentDescription = "Toca-discos JUKE",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Box(
            modifier = Modifier
                .size(discSize)
                .align(Alignment.Center)
                .offset(y = size * .035f)
                .clip(CircleShape)
                .graphicsLayer { rotationZ = rotation.value }
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(size).offset(x = -discLeft, y = -discTop),
                contentScale = ContentScale.Fit
            )
        }
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(TurntableTonearmMask),
            contentScale = ContentScale.Fit
        )
    }
}

private val TurntableTonearmMask = GenericShape { shapeSize, _ ->
    val width = shapeSize.width
    val height = shapeSize.height
    moveTo(.80f * width, .10f * height)
    cubicTo(.89f * width, .10f * height, .93f * width, .15f * height, .93f * width, .23f * height)
    lineTo(.88f * width, .29f * height)
    cubicTo(.87f * width, .43f * height, .83f * width, .56f * height, .77f * width, .67f * height)
    lineTo(.80f * width, .70f * height)
    lineTo(.77f * width, .78f * height)
    lineTo(.69f * width, .86f * height)
    lineTo(.61f * width, .83f * height)
    lineTo(.62f * width, .77f * height)
    lineTo(.68f * width, .72f * height)
    cubicTo(.73f * width, .64f * height, .76f * width, .54f * height, .77f * width, .42f * height)
    lineTo(.78f * width, .30f * height)
    cubicTo(.73f * width, .26f * height, .72f * width, .21f * height, .73f * width, .17f * height)
    cubicTo(.74f * width, .12f * height, .76f * width, .10f * height, .80f * width, .10f * height)
    close()
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
