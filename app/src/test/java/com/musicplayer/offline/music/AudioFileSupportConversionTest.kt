package com.musicplayer.offline.music

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFileSupportConversionTest {
    @Test fun detectsWavByMime() = assertTrue(AudioFileSupport.isWav("audio/x-wav", "sem_extensao"))
    @Test fun detectsWavByExtension() = assertTrue(AudioFileSupport.isWav(null, "Faixa.WAV"))
    @Test fun detectsWavByUri() = assertTrue(AudioFileSupport.isWav(null, "Faixa", "file:///Music/Faixa.wav"))
    @Test fun doesNotOfferConversionForMp3() = assertFalse(AudioFileSupport.isWav("audio/mpeg", "Faixa.mp3"))
}
