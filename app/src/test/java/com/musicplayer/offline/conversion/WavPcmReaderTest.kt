package com.musicplayer.offline.conversion

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WavPcmReaderTest {
    @Test
    fun readsMono16BitPcmAndDecodesSamples() {
        val wav = pcmWav(channels = 1, bits = 16, samples = shortArrayOf(-32768, 0, 32767))
        val reader = WavPcmReader(ByteArrayInputStream(wav))

        assertEquals(1, reader.format.channels)
        assertEquals(44_100, reader.format.sampleRate)
        assertEquals(6, reader.format.dataSize)
        val data = ByteArray(6).also { reader.input.readChunk(it, it.size) }
        assertArrayEquals(shortArrayOf(-32768, 0, 32767), reader.decodeToShorts(data, data.size))
    }

    @Test
    fun readsStereo16BitInterleavedPcm() {
        val samples = shortArrayOf(-1000, 1000, -2000, 2000)
        val reader = WavPcmReader(ByteArrayInputStream(pcmWav(2, 16, samples)))

        assertEquals(2, reader.format.channels)
        assertEquals(4, reader.format.blockAlign)
        val data = ByteArray(8).also { reader.input.readChunk(it, it.size) }
        assertArrayEquals(samples, reader.decodeToShorts(data, data.size))
    }

    @Test(expected = UnsupportedWavException::class)
    fun rejectsCompressedWav() {
        WavPcmReader(ByteArrayInputStream(pcmWav(1, 16, shortArrayOf(0), formatCode = 3)))
    }

    private fun pcmWav(channels: Int, bits: Int, samples: ShortArray, formatCode: Int = 1): ByteArray {
        val pcm = ByteArrayOutputStream().apply {
            samples.forEach { sample ->
                write(sample.toInt() and 0xff)
                write((sample.toInt() ushr 8) and 0xff)
            }
        }.toByteArray()
        return ByteArrayOutputStream().apply {
            write("RIFF".toByteArray())
            writeLe32(36 + pcm.size)
            write("WAVEfmt ".toByteArray())
            writeLe32(16)
            writeLe16(formatCode)
            writeLe16(channels)
            writeLe32(44_100)
            writeLe32(44_100 * channels * bits / 8)
            writeLe16(channels * bits / 8)
            writeLe16(bits)
            write("data".toByteArray())
            writeLe32(pcm.size)
            write(pcm)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeLe16(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
    }

    private fun ByteArrayOutputStream.writeLe32(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
        write((value ushr 16) and 0xff)
        write((value ushr 24) and 0xff)
    }
}
