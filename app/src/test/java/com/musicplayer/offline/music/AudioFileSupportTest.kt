package com.musicplayer.offline.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFileSupportTest {
    @Test fun wavWithoutTagsUsesFileNameAndNeutralMetadata() {
        assertEquals("JUKE_WAV_Sem_Tags", AudioFileSupport.title("<unknown>", "JUKE_WAV_Sem_Tags.wav"))
        assertEquals(UNKNOWN_ARTIST, AudioFileSupport.artist("<unknown>"))
        assertEquals(UNKNOWN_ALBUM, AudioFileSupport.album(null))
        assertEquals("audio/wav", AudioFileSupport.mimeType(null, "JUKE_WAV_Sem_Tags.WAV"))
    }

    @Test fun mp3AndWavMimeTypesAreRecognized() {
        assertTrue("audio/mpeg" in AudioFileSupport.recognizedMimeTypes)
        assertTrue("audio/wav" in AudioFileSupport.recognizedMimeTypes)
    }

    @Test fun unsupportedDecoderAndContainerErrorsAreClassified() {
        assertTrue(AudioFileSupport.isUnsupportedCodecError(3_003))
        assertTrue(AudioFileSupport.isUnsupportedCodecError(4_005))
    }
}
