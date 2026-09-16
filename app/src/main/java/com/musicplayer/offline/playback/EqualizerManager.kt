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
    val bassEnabled: Boolean = false,
    val bassStrength: Int = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Int = 0,
    val message: String? = null
)

object EqualizerManager {
    val presetNames = listOf("Flat", "Pop", "Rock", "Clássico", "Jazz", "Hip-hop", "Dance", "Vocal", "Heavy Metal")
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
            val minLevel = range[0].toInt().coerceAtLeast(-1_200)
            val maxLevel = range[1].toInt().coerceAtMost(1_200)
            val saved = loadSettings(context, frequencies.size, minLevel, maxLevel)
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

    fun reset() = applyPreset("Flat")

    fun setBassEnabled(enabled: Boolean) {
        val current = mutableState.value
        val strength = if (enabled && current.bassStrength == 0) DEFAULT_BASS_STRENGTH else current.bassStrength
        mutableState.value = current.copy(bassEnabled = enabled, bassStrength = strength)
        runCatching { bassBoost?.setStrength(strength.toShort()) }
        runCatching { bassBoost?.enabled = current.enabled && enabled && strength > 0 }
        persist()
    }

    fun setBassStrength(value: Int) {
        mutableState.value = mutableState.value.copy(bassStrength = value.coerceIn(0, 1000))
        runCatching { bassBoost?.setStrength(mutableState.value.bassStrength.toShort()) }
        runCatching { bassBoost?.enabled = mutableState.value.enabled && mutableState.value.bassEnabled && mutableState.value.bassStrength > 0 }
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
        runCatching { bassBoost?.enabled = value.enabled && value.bassEnabled && value.bassStrength > 0 }
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
            .putBoolean("bass_enabled", value.bassEnabled)
            .putInt("bass", value.bassStrength)
            .putInt("virtualizer", value.virtualizerStrength)
            .apply()
    }

    private fun loadSettings(context: Context, bands: Int, min: Int, max: Int): EqualizerUiState {
        val p = context.getSharedPreferences("equalizer", Context.MODE_PRIVATE)
        val levels = p.getString("levels", null)?.split(',')?.mapNotNull(String::toIntOrNull)
            ?.takeIf { it.size == bands } ?: List(bands) { 0 }
        return EqualizerUiState(
            enabled = p.getBoolean("enabled", false),
            preset = p.getString("preset", "Flat")?.takeIf(presetNames::contains) ?: "Flat",
            bandLevels = levels.map { it.coerceIn(min, max) }, minLevel = min, maxLevel = max,
            bassEnabled = p.getBoolean("bass_enabled", false),
            bassStrength = p.getInt("bass", 0).coerceIn(0, 1000),
            virtualizerStrength = p.getInt("virtualizer", 0).coerceIn(0, 1000)
        )
    }

    internal fun presetProfile(name: String, frequencies: List<Int>): List<Float> = frequencies.map { hz ->
        when (name) {
            "Rock" -> when { hz < 180 -> .55f; hz in 500..2500 -> -.25f; hz > 6000 -> .5f; else -> .15f }
            "Pop" -> when { hz in 300..3000 -> .4f; hz > 7000 -> .25f; else -> .1f }
            "Clássico" -> when { hz < 180 -> .2f; hz in 500..2500 -> -.1f; hz > 6000 -> .3f; else -> 0f }
            "Jazz" -> when { hz < 220 -> .3f; hz in 500..3000 -> .15f; hz > 6000 -> .25f; else -> 0f }
            "Hip-hop" -> when { hz < 220 -> .65f; hz < 800 -> .35f; hz > 7000 -> .1f; else -> -.15f }
            "Dance" -> when { hz < 220 -> .55f; hz in 500..2500 -> -.1f; hz > 5000 -> .4f; else -> .15f }
            "Vocal" -> when { hz in 500..4000 -> .6f; hz < 180 -> -.25f; else -> .1f }
            "Heavy Metal" -> when { hz < 220 -> .55f; hz in 500..2500 -> -.2f; hz > 5000 -> .55f; else -> .15f }
            else -> 0f
        }
    }

    private const val DEFAULT_BASS_STRENGTH = 500
}
