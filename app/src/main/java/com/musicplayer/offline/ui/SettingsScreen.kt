@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.musicplayer.offline.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.offline.data.AccentColor
import com.musicplayer.offline.data.AppSettings
import com.musicplayer.offline.data.ArtworkSize
import com.musicplayer.offline.data.ThemeMode

@Composable
fun SettingsScreen(settings: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit, rescan: () -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val versionName = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FeatureTopBar("Configurações", back)
        SectionLabel("APARÊNCIA")
        SettingsCard {
            Text("Tema", fontWeight = FontWeight.SemiBold)
            ChoiceRow(ThemeMode.entries, settings.themeMode, { themeLabel(it) }) { update { s -> s.copy(themeMode = it) } }
            Spacer(Modifier.height(12.dp))
            Text("Cor de destaque", fontWeight = FontWeight.SemiBold)
            AccentColorChoiceRow(settings.accentColor) { update { s -> s.copy(accentColor = it) } }
        }
        SectionLabel("REPRODUÇÃO")
        SettingsCard {
            SettingSwitch("Retomar última faixa", "Restaura a fila sem tocar automaticamente", settings.resumeLastTrack) { value -> update { it.copy(resumeLastTrack = value) } }
            SettingSwitch("Retomar posição", "Volta ao ponto onde você parou", settings.resumePosition) { value -> update { it.copy(resumePosition = value) } }
            SettingSwitch("Reproduzir ao abrir", "Só toca automaticamente quando habilitado", settings.autoResume) { value -> update { it.copy(autoResume = value) } }
            SettingSwitch("Restaurar aleatório", "Mantém o estado de shuffle", settings.restoreShuffle) { value -> update { it.copy(restoreShuffle = value) } }
            SettingSwitch("Restaurar repetição", "Mantém o modo repeat", settings.restoreRepeat) { value -> update { it.copy(restoreRepeat = value) } }
        }
        SectionLabel("BIBLIOTECA")
        SettingsCard {
            SettingSwitch("Mostrar músicas muito curtas", "Quando desligado, usa a duração mínima", settings.showShortSongs) { value -> update { it.copy(showShortSongs = value) } }
            Text("Duração mínima: ${settings.minimumDurationSeconds}s", fontWeight = FontWeight.SemiBold)
            Slider(
                settings.minimumDurationSeconds.toFloat(),
                { value -> update { it.copy(minimumDurationSeconds = value.toInt()) } },
                valueRange = 0f..180f, enabled = !settings.showShortSongs
            )
            SettingSwitch("Mostrar arquivos desconhecidos", "Inclui áudios que o MediaStore não marcou como música", settings.showUnknownFiles) { value -> update { it.copy(showUnknownFiles = value) } }
            Button(rescan, Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Text(" Reescanear músicas") }
        }
        SectionLabel("INTERFACE")
        SettingsCard {
            SettingSwitch("Mostrar mini player", "Mantém os controles na base da biblioteca", settings.showMiniPlayer) { value -> update { it.copy(showMiniPlayer = value) } }
            Text("Tamanho das capas", fontWeight = FontWeight.SemiBold)
            ChoiceRow(ArtworkSize.entries, settings.artworkSize, { artworkLabel(it) }) { update { s -> s.copy(artworkSize = it) } }
            SettingSwitch("Animações da interface", "Transições e rolagens suaves", settings.animationsEnabled) { value -> update { it.copy(animationsEnabled = value) } }
        }
        SectionLabel("SOBRE")
        SettingsCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                JukeCircularLogo(Modifier.size(72.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    JukeHorizontalLogo(Modifier.width(150.dp).height(50.dp))
                    Text("JUKE Mp3 Player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text("Versão $versionName", color = TextMuted)
            Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", color = TextMuted)
            Text("Reprodução: AndroidX Media3. Equalização: APIs nativas Android AudioEffect.", color = TextMuted)
            Text("Conversão MP3: LAME 3.100 (LGPL) via JNA (Apache 2.0).", color = TextMuted)
            Text("Aplicativo para reprodução offline de arquivos do usuário.", color = TextMuted)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)
    ) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, value: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { changed(!value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        JukeSwitch(value, changed)
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, change: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        values.forEach { value -> FilterChip(value == selected, { change(value) }, { Text(label(value)) }) }
    }
}

@Composable
private fun AccentColorChoiceRow(selected: AccentColor, change: (AccentColor) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        AccentColor.entries.forEach { accent ->
            val isSelected = accent == selected
            val accentColor = accent.themeColor()
            FilterChip(
                selected = isSelected,
                onClick = { change(accent) },
                label = { Text(accentLabel(accent), color = if (isSelected) Color.White else JukeTextPrimary) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceRaised.copy(alpha = 0.68f),
                    labelColor = JukeTextPrimary,
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.White
                ),
                border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = accentColor
                )
            )
        }
    }
}

private fun accentLabel(value: AccentColor) = when (value) {
    AccentColor.CYAN -> "Ciano JUKE"
    AccentColor.ELECTRIC_BLUE -> "Azul Elétrico"
}
private fun artworkLabel(value: ArtworkSize) = when (value) { ArtworkSize.SMALL -> "Pequena"; ArtworkSize.MEDIUM -> "Média"; ArtworkSize.LARGE -> "Grande" }
private fun themeLabel(value: ThemeMode) = when (value) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.DARK -> "Escuro"; ThemeMode.LIGHT -> "Claro" }
