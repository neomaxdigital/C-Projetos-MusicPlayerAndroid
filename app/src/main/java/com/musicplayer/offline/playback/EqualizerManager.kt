package com.musicplayer.offline.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val preset: String = "Flat",
    val frequenciesHz: List<Int> = emptyList(),
    val bandLevels: List<Int> = emptyList(),
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    val bassSupported: Boolean = false,
    val bassStrength: Int = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Int = 0,
    val message: String? = null
)

object EqualizerManager {
    val presetNames = listOf("Flat", "Bass Boost", "Treble Boost", "Rock", "Pop", "Electronic", "Vocal")
    private val mutableState = MutableStateFlow(EqualizerUiState(message = "Aguardando sessão de áudio"))
    val state: StateFlow<EqualizerUiState> = mutableState.asStateFlow()
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var appContext: Context? = null
    private var sessionId = 0

    fun attach(context: Context, audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == sessionId) return
        releaseEffects()
        appContext = context.applicationContext
        sessionId = audioSessionId
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val range = eq.bandLevelRange
            val frequencies = (0 until eq.numberOfBands.toInt()).map { eq.getCenterFreq(it.toShort()) / 1000 }
            equalizer = eq
            bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
            val saved = loadSettings(context, frequencies.size, range[0].toInt(), range[1].toInt())
            mutableState.value = saved.copy(
                available = true,
                frequenciesHz = frequencies,
                bassSupported = bassBoost?.strengthSupported == true,
                virtualizerSupported = virtualizer?.strengthSupported == true,
                message = null
            )
            applyAll()
        }.onFailure {
            releaseEffects()
            sessionId = 0
            mutableState.value = EqualizerUiState(message = "Equalizador nativo indisponível neste dispositivo")
        }
    }

    fun setEnabled(enabled: Boolean) {
        mutableState.value = mutableState.value.copy(enabled = enabled)
        applyAll(); persist()
    }

    fun setBand(index: Int, level: Int) {
        val current = mutableState.value
        if (index !in current.bandLevels.indices) return
        val levels = current.bandLevels.toMutableList().apply { this[index] = level.coerceIn(current.minLevel, current.maxLevel) }
        mutableState.value = current.copy(bandLevels = levels, preset = "Personalizado")
        runCatching { equalizer?.setBandLevel(index.toShort(), levels[index].toShort()) }
        persist()
    }

    fun applyPreset(name: String) {
        val current = mutableState.value
        val profile = presetProfile(name, current.frequenciesHz)
        val levels = profile.map { normalized ->
            val amplitude = if (normalized >= 0) current.maxLevel else -current.minLevel
            (normalized * amplitude).toInt().coerceIn(current.minLevel, current.maxLevel)
        }
        mutableState.value = current.copy(preset = name, bandLevels = levels)
        levels.forEachIndexed { index, level -> runCatching { equalizer?.setBandLevel(index.toShort(), level.toShort()) } }
        persist()
    }

    fun setBassStrength(value: Int) {
        mutableState.value = mutableState.value.copy(bassStrength = value.coerceIn(0, 1000))
        runCatching { bassBoost?.setStrength(mutableState.value.bassStrength.toShort()) }
        runCatching { bassBoost?.enabled = mutableState.value.enabled && mutableState.value.bassStrength > 0 }
        persist()
    }

    fun setVirtualizerStrength(value: Int) {
        mutableState.value = mutableState.value.copy(virtualizerStrength = value.coerceIn(0, 1000))
        runCatching { virtualizer?.setStrength(mutableState.value.virtualizerStrength.toShort()) }
        runCatching { virtualizer?.enabled = mutableState.value.enabled && mutableState.value.virtualizerStrength > 0 }
        persist()
    }

    fun release() {
        releaseEffects()
        sessionId = 0
        mutableState.value = EqualizerUiState(message = "Aguardando sessão de áudio")
    }

    private fun applyAll() {
        val value = mutableState.value
        runCatching { equalizer?.enabled = value.enabled }
        runCatching { bassBoost?.enabled = value.enabled && value.bassStrength > 0 }
        runCatching { virtualizer?.enabled = value.enabled && value.virtualizerStrength > 0 }
        value.bandLevels.forEachIndexed { index, level -> runCatching { equalizer?.setBandLevel(index.toShort(), level.toShort()) } }
        runCatching { bassBoost?.setStrength(value.bassStrength.toShort()) }
        runCatching { virtualizer?.setStrength(value.virtualizerStrength.toShort()) }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }; equalizer = null
        runCatching { bassBoost?.release() }; bassBoost = null
        runCatching { virtualizer?.release() }; virtualizer = null
    }

    private fun persist() {
        val context = appContext ?: return
        val value = mutableState.value
        context.getSharedPreferences("equalizer", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", value.enabled)
            .putString("preset", value.preset)
            .putString("levels", value.bandLevels.joinToString(","))
            .putInt("bass", value.bassStrength)
            .putInt("virtualizer", value.virtualizerStrength)
            .apply()
    }

    private fun loadSettings(context: Context, bands: Int, min: Int, max: Int): EqualizerUiState {
        val p = context.getSharedPreferences("equalizer", Context.MODE_PRIVATE)
        val levels = p.getString("levels", null)?.split(',')?.mapNotNull(String::toIntOrNull)
            ?.takeIf { it.size == bands } ?: List(bands) { 0 }
        return EqualizerUiState(
            enabled = p.getBoolean("enabled", false), preset = p.getString("preset", "Flat") ?: "Flat",
            bandLevels = levels.map { it.coerceIn(min, max) }, minLevel = min, maxLevel = max,
            bassStrength = p.getInt("bass", 0).coerceIn(0, 1000),
            virtualizerStrength = p.getInt("virtualizer", 0).coerceIn(0, 1000)
        )
    }

    internal fun presetProfile(name: String, frequencies: List<Int>): List<Float> = frequencies.map { hz ->
        when (name) {
            "Bass Boost" -> when { hz < 180 -> .75f; hz < 600 -> .35f; else -> 0f }
            "Treble Boost" -> when { hz > 6000 -> .7f; hz > 2000 -> .35f; else -> 0f }
            "Rock" -> when { hz < 180 -> .55f; hz in 500..2500 -> -.25f; hz > 6000 -> .5f; else -> .15f }
            "Pop" -> when { hz in 300..3000 -> .4f; hz > 7000 -> .25f; else -> .1f }
            "Electronic" -> when { hz < 220 -> .65f; hz > 5000 -> .55f; else -> -.1f }
            "Vocal" -> when { hz in 500..4000 -> .6f; hz < 180 -> -.25f; else -> .1f }
            else -> 0f
        }
    }
}
