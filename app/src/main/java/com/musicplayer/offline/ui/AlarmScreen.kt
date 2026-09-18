package com.musicplayer.offline.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.musicplayer.offline.alarm.AlarmDayLabels
import com.musicplayer.offline.alarm.AlarmScheduler
import com.musicplayer.offline.alarm.AlarmSourceType
import com.musicplayer.offline.alarm.AlarmTrack
import com.musicplayer.offline.alarm.MusicAlarm
import com.musicplayer.offline.alarm.MusicAlarmRepository
import com.musicplayer.offline.music.LocalPlaylist
import com.musicplayer.offline.music.Song
import java.util.Locale

@Composable
fun AlarmScreen(
    songs: List<Song>,
    playlists: List<LocalPlaylist>,
    back: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { MusicAlarmRepository(context) }
    var alarms by remember { mutableStateOf(repository.alarms()) }
    var editing by remember { mutableStateOf<MusicAlarm?>(null) }
    val exactAllowed = AlarmScheduler.canScheduleExact(context)
    var notificationsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = it }

    Column(Modifier.fillMaxSize()) {
        FeatureTopBar("Despertador", back)

        if (!exactAllowed) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Permissão para horário exato", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Ative alarmes exatos para o despertador tocar no horário escolhido.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            AlarmScheduler.exactAlarmSettingsIntent(context)?.let(context::startActivity)
                        }
                    ) { Text("Permitir alarmes exatos") }
                }
            }
        }

        if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Notificações do despertador", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Permita notificações para usar os botões Soneca e Parar quando o alarme tocar.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    ) { Text("Permitir notificações") }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Button(
                    onClick = { editing = MusicAlarm() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Novo despertador")
                }
            }

            if (alarms.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Alarm, null, tint = PrimaryBlue, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Nenhum despertador", fontWeight = FontWeight.Bold)
                            Text("Crie um alarme e acorde com sua música.", color = TextMuted)
                        }
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onOpen = { editing = alarm },
                        onToggle = { enabled ->
                            if (enabled && (alarm.tracks.isEmpty() || !AlarmScheduler.canScheduleExact(context))) {
                                Toast.makeText(
                                    context,
                                    if (alarm.tracks.isEmpty()) "Escolha uma música ou playlist primeiro."
                                    else "Permita alarmes exatos para ativar.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val updated = alarm.copy(enabled = enabled)
                                alarms = repository.upsert(updated)
                                if (enabled) AlarmScheduler.schedule(context, updated)
                                else AlarmScheduler.cancel(context, alarm.id)
                            }
                        },
                        onDelete = {
                            AlarmScheduler.cancel(context, alarm.id)
                            alarms = repository.delete(alarm.id)
                        }
                    )
                }
            }
        }
    }

    editing?.let { alarm ->
        AlarmEditorDialog(
            initial = alarm,
            songs = songs,
            playlists = playlists,
            onDismiss = { editing = null },
            onSave = { edited ->
                val finalAlarm = if (
                    edited.enabled &&
                    (!AlarmScheduler.canScheduleExact(context) || edited.tracks.isEmpty())
                ) edited.copy(enabled = false) else edited

                alarms = repository.upsert(finalAlarm)
                if (finalAlarm.enabled) AlarmScheduler.schedule(context, finalAlarm)
                else AlarmScheduler.cancel(context, finalAlarm.id)

                if (edited.enabled && !finalAlarm.enabled) {
                    Toast.makeText(
                        context,
                        if (edited.tracks.isEmpty()) "Escolha uma música ou playlist."
                        else "Alarme salvo. Permita alarmes exatos para ativar.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                editing = null
            }
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: MusicAlarm,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(alarm.label, fontWeight = FontWeight.SemiBold)
                Text(AlarmDayLabels.summary(alarm.days), color = TextMuted, fontSize = 12.sp)
                Text(
                    alarm.sourceTitle,
                    color = if (alarm.tracks.isEmpty()) TextMuted else PrimaryBlue,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            JukeSwitch(alarm.enabled, onToggle)
            IconButton(onDelete) {
                Icon(Icons.Default.Delete, "Excluir despertador", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun AlarmEditorDialog(
    initial: MusicAlarm,
    songs: List<Song>,
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onSave: (MusicAlarm) -> Unit
) {
    val context = LocalContext.current
    val songsById = remember(songs) { songs.associateBy { it.id } }

    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var hour by remember(initial.id) { mutableIntStateOf(initial.hour) }
    var minute by remember(initial.id) { mutableIntStateOf(initial.minute) }
    var days by remember(initial.id) { mutableStateOf(initial.days) }
    var sourceType by remember(initial.id) { mutableStateOf(initial.sourceType) }
    var sourceId by remember(initial.id) { mutableStateOf(initial.sourceId) }
    var sourceTitle by remember(initial.id) { mutableStateOf(initial.sourceTitle) }
    var tracks by remember(initial.id) { mutableStateOf(initial.tracks) }
    var gradual by remember(initial.id) { mutableStateOf(initial.gradualVolume) }
    var volume by remember(initial.id) { mutableFloatStateOf(initial.volumePercent.toFloat()) }
    var snooze by remember(initial.id) { mutableIntStateOf(initial.snoozeMinutes) }
    var vibrate by remember(initial.id) { mutableStateOf(initial.vibrate) }
    var pickingSource by remember { mutableStateOf<AlarmSourceType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.sourceId.isBlank()) "Novo despertador" else "Editar despertador") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m -> hour = h; minute = m },
                            hour,
                            minute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nome") }
                )

                Spacer(Modifier.height(12.dp))
                Text("Dias", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    AlarmDayLabels.ordered.forEach { (day, short) ->
                        FilterChip(
                            selected = day in days,
                            onClick = {
                                days = if (day in days) days - day else days + day
                            },
                            label = { Text(short) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Som", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sourceType == AlarmSourceType.SONG,
                        onClick = { sourceType = AlarmSourceType.SONG; pickingSource = AlarmSourceType.SONG },
                        label = { Text("Música") },
                        leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                    )
                    FilterChip(
                        selected = sourceType == AlarmSourceType.PLAYLIST,
                        onClick = { sourceType = AlarmSourceType.PLAYLIST; pickingSource = AlarmSourceType.PLAYLIST },
                        label = { Text("Playlist") },
                        leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) }
                    )
                }
                TextButton(onClick = { pickingSource = sourceType }) {
                    Text(sourceTitle)
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Volume gradual", fontWeight = FontWeight.SemiBold)
                        Text("Aumenta suavemente por cerca de 30 segundos.", color = TextMuted, fontSize = 11.sp)
                    }
                    JukeSwitch(gradual, { gradual = it })
                }

                Text("Volume máximo: " + volume.toInt() + "%", fontSize = 12.sp)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 10f..100f
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibrar", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    JukeSwitch(vibrate, { vibrate = it })
                }

                Spacer(Modifier.height(8.dp))
                Text("Soneca", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15).forEach { minutes ->
                        FilterChip(
                            selected = snooze == minutes,
                            onClick = { snooze = minutes },
                            label = { Text(minutes.toString() + " min") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        days.isEmpty() -> Toast.makeText(context, "Escolha pelo menos um dia.", Toast.LENGTH_SHORT).show()
                        tracks.isEmpty() -> Toast.makeText(context, "Escolha uma música ou playlist.", Toast.LENGTH_SHORT).show()
                        else -> onSave(
                            initial.copy(
                                label = label.trim().ifBlank { "Despertador" },
                                hour = hour,
                                minute = minute,
                                days = days,
                                sourceType = sourceType,
                                sourceId = sourceId,
                                sourceTitle = sourceTitle,
                                tracks = tracks,
                                gradualVolume = gradual,
                                volumePercent = volume.toInt().coerceIn(10, 100),
                                snoozeMinutes = snooze,
                                vibrate = vibrate
                            )
                        )
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )

    pickingSource?.let { pickerType ->
        SourcePickerDialog(
            type = pickerType,
            songs = songs,
            playlists = playlists,
            onDismiss = { pickingSource = null },
            onSong = { song ->
                sourceType = AlarmSourceType.SONG
                sourceId = song.id.toString()
                sourceTitle = song.title
                tracks = listOf(song.toAlarmTrack())
                pickingSource = null
            },
            onPlaylist = { playlist ->
                val selectedSongs = playlist.songIds.mapNotNull(songsById::get)
                if (selectedSongs.isEmpty()) {
                    Toast.makeText(context, "Esta playlist está vazia.", Toast.LENGTH_SHORT).show()
                } else {
                    sourceType = AlarmSourceType.PLAYLIST
                    sourceId = playlist.id
                    sourceTitle = playlist.name
                    tracks = selectedSongs.map(Song::toAlarmTrack)
                    pickingSource = null
                }
            }
        )
    }
}

@Composable
private fun SourcePickerDialog(
    type: AlarmSourceType,
    songs: List<Song>,
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onSong: (Song) -> Unit,
    onPlaylist: (LocalPlaylist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == AlarmSourceType.SONG) "Escolher música" else "Escolher playlist") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                if (type == AlarmSourceType.SONG) {
                    items(songs, key = { it.id }) { song ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSong(song) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = PrimaryBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPlaylist(playlist) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlaylistPlay, null, tint = PrimaryBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (playlist.songIds.size == 1) "1 música" else playlist.songIds.size.toString() + " músicas",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

private fun Song.toAlarmTrack(): AlarmTrack = AlarmTrack(
    id = id,
    uri = uri.toString(),
    title = title,
    artist = artist,
    mimeType = mimeType
)
