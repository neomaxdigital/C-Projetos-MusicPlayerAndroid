package com.musicplayer.offline.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetRulesTest {
    @Test fun requestedPresetsAreAvailableIncludingFlat() {
        assertEquals(
            listOf("Flat", "Pop", "Rock", "Clássico", "Jazz", "Hip-hop", "Dance", "Vocal", "Heavy Metal"),
            EqualizerManager.presetNames
        )
    }

    @Test fun flatAndOtherProfilesStayWithinTheSupportedNormalizedRange() {
        val frequencies = listOf(60, 230, 910, 3_600, 14_000)
        assertEquals(List(frequencies.size) { 0f }, EqualizerManager.presetProfile("Flat", frequencies))
        EqualizerManager.presetNames.forEach { preset ->
            assertTrue(EqualizerManager.presetProfile(preset, frequencies).all { it in -1f..1f })
        }
    }
}
