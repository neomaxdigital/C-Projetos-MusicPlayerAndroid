package com.musicplayer.offline.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.offline.playback.EqualizerManager
import com.musicplayer.offline.playback.EqualizerUiState
import kotlin.math.roundToInt

private val DisplayBands = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
private val DisplayBandDescriptions = listOf("Graves", "Baixos", "Médios", "Agudos", "Brilho")

@Composable
fun EqualizerScreen(back: () -> Unit) {
    val state by EqualizerManager.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        EqualizerHeader(state.enabled, state.available, EqualizerManager::setEnabled, back)
        PresetsSection(state)
        ManualAdjustmentSection(state)
        SubwooferSection(state)
        state.message?.takeIf { !state.available }?.let { message ->
            Text(
                message,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EqualizerHeader(enabled: Boolean, available: Boolean, setEnabled: (Boolean) -> Unit, back: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().statusBarsPadding().height(76.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(back, Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = PrimaryBlue)
        }
        Text("Equalizador", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        JukeSwitch(enabled, setEnabled, enabled = available, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun PresetsSection(state: EqualizerUiState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Predefinições", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Som para cada momento", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
        EqualizerManager.presetNames.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { preset ->
                    val selected = state.preset == preset
                    Card(
                        modifier = Modifier.weight(1f).height(54.dp).clickable(enabled = state.available) {
                            EqualizerManager.applyPreset(preset)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) PrimaryBlue.copy(alpha = 0.23f) else SurfaceDark
                        ),
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(preset, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun ManualAdjustmentSection(state: EqualizerUiState) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ajuste manual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = EqualizerManager::reset, enabled = state.available) {
                    Text("Redefinir", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Refresh, "Redefinir", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            if (state.bandLevels.isEmpty()) {
                Text("Inicie uma música para preparar o equalizador desta sessão.", color = TextMuted, modifier = Modifier.padding(12.dp))
            } else {
                VerticalBandControls(state)
            }
        }
    }
}

@Composable
private fun VerticalBandControls(state: EqualizerUiState) {
    val bands = minOf(DisplayBands.size, state.bandLevels.size)
    Row(Modifier.fillMaxWidth().height(286.dp)) {
        Column(
            Modifier.width(52.dp).height(210.dp).padding(top = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("+12 dB", color = TextMuted, fontSize = 12.sp)
            Text("0 dB", color = TextMuted, fontSize = 12.sp)
            Text("-12 dB", color = TextMuted, fontSize = 12.sp)
        }
        Box(Modifier.weight(1f).height(286.dp)) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 105.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                repeat(bands) { index ->
                    VerticalBandSlider(
                        label = DisplayBands[index],
                        description = DisplayBandDescriptions[index],
                        value = state.bandLevels[index],
                        min = state.minLevel,
                        max = state.maxLevel,
                        enabled = state.enabled,
                        onValueChange = { EqualizerManager.setBand(index, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalBandSlider(
    label: String,
    description: String,
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val inactiveTrack = if (enabled) SurfaceRaised else SurfaceRaised.copy(alpha = 0.58f)
    val activeTrack = if (enabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.42f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val updateFromPosition: (Float, Float) -> Unit = { y, height ->
            onValueChange(verticalLevel(y, height, min, max))
        }
        Canvas(
            Modifier.width(38.dp).height(212.dp)
                .pointerInput(enabled, min, max) {
                    if (enabled) {
                        detectTapGestures { offset ->
                            updateFromPosition(offset.y, size.height.toFloat())
                        }
                    }
                }
                .pointerInput(enabled, min, max) {
                    if (enabled) {
                        detectVerticalDragGestures { change, _ ->
                            updateFromPosition(change.position.y, size.height.toFloat())
                            change.consume()
                        }
                    }
                }
        ) {
            val top = size.height * 0.07f
            val bottom = size.height * 0.93f
            val fraction = (value.coerceIn(min, max) - min).toFloat() / (max - min).coerceAtLeast(1)
            val thumbY = bottom - ((bottom - top) * fraction)
            val centerX = size.width / 2f
            drawLine(inactiveTrack, Offset(centerX, top), Offset(centerX, bottom), 5.dp.toPx(), StrokeCap.Round)
            drawLine(activeTrack, Offset(centerX, bottom), Offset(centerX, thumbY), 5.dp.toPx(), StrokeCap.Round)
            drawCircle(activeTrack.copy(alpha = 0.72f), 10.dp.toPx(), Offset(centerX, thumbY))
            drawCircle(onSurface, 7.5.dp.toPx(), Offset(centerX, thumbY))
        }
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
        Text(description, color = TextMuted, fontSize = 10.sp, maxLines = 1)
    }
}

private fun verticalLevel(y: Float, height: Float, min: Int, max: Int): Int {
    val top = height * 0.07f
    val bottom = height * 0.93f
    val fraction = ((bottom - y) / (bottom - top)).coerceIn(0f, 1f)
    return (min + ((max - min) * fraction)).roundToInt().coerceIn(min, max)
}

@Composable
private fun SubwooferSection(state: EqualizerUiState) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Subwoofer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mais impacto nos graves e mais batida.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                JukeSwitch(
                    state.bassEnabled,
                    EqualizerManager::setBassEnabled,
                    enabled = state.available && state.bassSupported
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("0", color = TextMuted)
                SubwooferSlider(
                    value = (state.bassStrength / 10f).coerceIn(0f, 100f),
                    onValueChange = { EqualizerManager.setBassStrength((it * 10).toInt()) },
                    enabled = state.available && state.bassSupported && state.bassEnabled,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Text("100", color = TextMuted)
            }
            if (!state.bassSupported) {
                Text("Reforço de graves indisponível para esta sessão de áudio.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SubwooferSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val activeTrack = if (enabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.42f)
    val inactiveTrack = if (enabled) SurfaceRaised else SurfaceRaised.copy(alpha = 0.58f)
    val thumbColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
    val thumbRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }

    Canvas(
        modifier.height(38.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { offset ->
                        onValueChange(horizontalSliderValue(offset.x, size.width.toFloat(), thumbRadiusPx))
                    }
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectHorizontalDragGestures { change, _ ->
                        onValueChange(horizontalSliderValue(change.position.x, size.width.toFloat(), thumbRadiusPx))
                        change.consume()
                    }
                }
            }
    ) {
        val thumbRadius = 10.dp.toPx()
        val start = thumbRadius
        val end = size.width - thumbRadius
        val centerY = size.height / 2f
        val thumbX = start + ((end - start) * (value / 100f).coerceIn(0f, 1f))

        drawLine(inactiveTrack, Offset(start, centerY), Offset(end, centerY), 5.dp.toPx(), StrokeCap.Round)
        drawLine(activeTrack, Offset(start, centerY), Offset(thumbX, centerY), 5.dp.toPx(), StrokeCap.Round)
        drawCircle(activeTrack.copy(alpha = 0.72f), thumbRadius, Offset(thumbX, centerY))
        drawCircle(thumbColor, 7.5.dp.toPx(), Offset(thumbX, centerY))
    }
}

private fun horizontalSliderValue(x: Float, width: Float, thumbRadius: Float): Float {
    val start = thumbRadius
    val end = (width - thumbRadius).coerceAtLeast(start + 1f)
    return (((x - start) / (end - start)) * 100f).coerceIn(0f, 100f)
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
    text,
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    color = PrimaryBlue,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.Bold
)
