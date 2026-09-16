@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.musicplayer.offline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.offline.playback.EqualizerManager

@Composable
fun EqualizerScreen(back: () -> Unit) {
    val state by EqualizerManager.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            FeatureTopBar("Equalizador", back)
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = PrimaryBlue)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(if (state.enabled) "Equalizador ligado" else "Equalizador desligado", fontWeight = FontWeight.Bold)
                        Text(state.message ?: "Sessão de áudio atual", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    JukeSwitch(state.enabled, EqualizerManager::setEnabled, enabled = state.available)
                }
            }
        }
        if (state.available) {
            item {
                SectionLabel("PRESETS")
                androidx.compose.foundation.layout.FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EqualizerManager.presetNames.forEach { preset ->
                        FilterChip(state.preset == preset, { EqualizerManager.applyPreset(preset) }, { Text(preset) })
                    }
                }
                Spacer(Modifier.height(18.dp))
                SectionLabel("BANDAS")
            }
            items(state.bandLevels.size) { index ->
                val hz = state.frequenciesHz.getOrElse(index) { 0 }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(formatFrequency(hz), fontWeight = FontWeight.SemiBold)
                        Text("${state.bandLevels[index] / 100f} dB", color = TextMuted)
                    }
                    Slider(
                        value = state.bandLevels[index].toFloat(),
                        onValueChange = { EqualizerManager.setBand(index, it.toInt()) },
                        valueRange = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                        enabled = state.enabled
                    )
                }
            }
            if (state.bassSupported) item { StrengthControl("Bass Boost", state.bassStrength, state.enabled, EqualizerManager::setBassStrength) }
            if (state.virtualizerSupported) item { StrengthControl("Virtualizer", state.virtualizerStrength, state.enabled, EqualizerManager::setVirtualizerStrength) }
        }
    }
}

@Composable
private fun StrengthControl(title: String, value: Int, enabled: Boolean, set: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.SemiBold); Text("${value / 10}%", color = TextMuted) }
        Slider(value.toFloat(), { set(it.toInt()) }, enabled = enabled, valueRange = 0f..1000f)
    }
}

@Composable
fun FeatureTopBar(title: String, back: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionLabel(text: String) = Text(
    text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    color = PrimaryBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
)

private fun formatFrequency(hz: Int) = if (hz >= 1000) "${hz / 1000f} kHz" else "$hz Hz"
