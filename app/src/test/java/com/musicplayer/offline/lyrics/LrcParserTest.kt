package com.musicplayer.offline.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test fun parsesAndSortsSynchronizedLines() {
        val result = LrcParser.parse("[ar:Teste]\n[00:10.50]Segunda\n[00:01.2]Primeira\n[00:20.000][00:30.00]Refrão")
        assertTrue(result.synchronized)
        assertEquals(listOf(1_200L, 10_500L, 20_000L, 30_000L), result.lines.map { it.timeMs })
        assertEquals("Refrão", result.lines.last().text)
    }

    @Test fun keepsPlainUnsynchronizedLyrics() {
        val result = LrcParser.parse("Primeira linha\n\nSegunda linha")
        assertFalse(result.synchronized)
        assertEquals(listOf("Primeira linha", "Segunda linha"), result.lines.map { it.text })
    }
}
