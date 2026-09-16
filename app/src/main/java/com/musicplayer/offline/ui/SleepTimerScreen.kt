@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.musicplayer.offline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.musicplayer.offline.playback.SleepTimerManager
import com.musicplayer.offline.playback.SleepTimerMode
import com.musicplayer.offline.playback.SleepTimerState

@Composable
fun SleepTimerScreen(back: () -> Unit) {
    val timer by SleepTimerManager.state.collectAsState()
    var fade by remember(timer.fadeEnabled) { mutableStateOf(timer.fadeEnabled) }
    var customOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FeatureTopBar("Timer para dormir", back)
        Card(
            Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Bedtime, null, tint = PrimaryBlue)
                Spacer(Modifier.height(12.dp))
                Text(timerDescription(timer), fontWeight = FontWeight.Bold)
                if (timer.active) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(SleepTimerManager::cancel) { Text("Cancelar timer") }
                }
            }
        }
        SectionLabel("DESLIGAR EM")
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                FilterChip(false, { SleepTimerManager.startMinutes(minutes, fade) }, { Text("$minutes min") })
            }
            FilterChip(timer.mode == SleepTimerMode.END_OF_TRACK, { SleepTimerManager.startEndOfTrack(false) }, { Text("Fim da música") })
            FilterChip(false, { customOpen = true }, { Text("Personalizado") })
        }
        Row(Modifier.fillMaxWidth().clickable { fade = !fade }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Fade suave", fontWeight = FontWeight.SemiBold); Text("Reduz o volume nos 10 segundos finais", color = TextMuted) }
            JukeSwitch(fade, { fade = it })
        }
    }
    if (customOpen) CustomTimerDialog({ customOpen = false }) { minutes -> customOpen = false; SleepTimerManager.startMinutes(minutes, fade) }
}

@Composable
private fun CustomTimerDialog(dismiss: () -> Unit, start: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("Tempo personalizado") },
        text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) }, label = { Text("Minutos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { Button({ minutes?.let(start) }, enabled = minutes != null && minutes in 1..720) { Text("Iniciar") } },
        dismissButton = { TextButton(dismiss) { Text("Cancelar") } }
    )
}

private fun timerDescription(timer: SleepTimerState): String = when (timer.mode) {
    SleepTimerMode.OFF -> "Nenhum timer ativo"
    SleepTimerMode.END_OF_TRACK -> "Pausa ao fim da música atual"
    SleepTimerMode.DEADLINE -> "%02d:%02d restantes".format(timer.remainingMs / 60_000, (timer.remainingMs / 1_000) % 60)
}
