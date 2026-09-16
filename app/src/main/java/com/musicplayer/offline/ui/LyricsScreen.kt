package com.musicplayer.offline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.offline.lyrics.LyricsDocument
import com.musicplayer.offline.lyrics.LyricsRepository
import com.musicplayer.offline.music.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LyricsScreen(song: Song?, positionMs: Long, animationsEnabled: Boolean = true, back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        FeatureTopBar("Letras", back)
        LyricsContent(song, positionMs, animationsEnabled)
    }
}

@Composable
fun LyricsContent(song: Song?, positionMs: Long, animationsEnabled: Boolean = true) {
    val context = LocalContext.current
    var document by remember(song?.id) { mutableStateOf<LyricsDocument?>(null) }
    var loading by remember(song?.id) { mutableStateOf(song != null) }
    LaunchedEffect(song?.id) {
        loading = true
        document = song?.let { withContext(Dispatchers.IO) { LyricsRepository(context).load(it) } }
        loading = false
    }
    val listState = rememberLazyListState()
    var automatic by remember { mutableStateOf(true) }
    val currentIndex = remember(document, positionMs) {
        document?.lines?.indexOfLast { it.timeMs != null && it.timeMs <= positionMs }?.coerceAtLeast(0) ?: 0
    }
    LaunchedEffect(currentIndex, automatic) {
        if (automatic && document?.synchronized == true && document!!.lines.isNotEmpty()) {
            if (animationsEnabled) listState.animateScrollToItem(currentIndex.coerceAtLeast(0)) else listState.scrollToItem(currentIndex.coerceAtLeast(0))
        }
    }
    Column(Modifier.fillMaxSize()) {
        song?.let { Text("${it.title} • ${it.artist}", color = TextMuted, modifier = Modifier.padding(horizontal = 20.dp)) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Rolagem automática", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            JukeSwitch(automatic, { automatic = it }, enabled = document?.synchronized == true)
            if (!automatic && document?.synchronized == true) IconButton({ automatic = true }) { Icon(Icons.Default.MyLocation, "Voltar à linha atual") }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
        } else if (document == null || document!!.lines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(36.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lyrics, null, tint = PrimaryBlue)
                    Text("Nenhuma letra disponível", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
                    Text("Adicione uma letra embutida ou um arquivo .lrc ao lado da música.", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                itemsIndexed(document!!.lines) { index, line ->
                    val active = document!!.synchronized && index == currentIndex
                    Text(
                        line.text.ifBlank { "♪" },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        color = if (active) PrimaryBlue else if (document!!.synchronized) TextMuted else MaterialTheme.colorScheme.onBackground,
                        fontSize = if (active) 21.sp else 17.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
