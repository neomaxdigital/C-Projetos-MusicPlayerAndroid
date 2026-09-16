package com.musicplayer.offline.conversion

import java.io.EOFException
import java.io.InputStream

data class WavPcmFormat(
    val channels: Int,
    val sampleRate: Int,
    val bitsPerSample: Int,
    val blockAlign: Int,
    val dataSize: Long
)

class UnsupportedWavException(message: String) : Exception(message)

/** Reads uncompressed little-endian PCM WAV without loading the complete file in memory. */
class WavPcmReader(val input: InputStream) {
    val format: WavPcmFormat = parse(input)

    private fun parse(input: InputStream): WavPcmFormat {
        if (input.readFourCc() != "RIFF") throw UnsupportedWavException("O arquivo não é um WAV RIFF válido.")
        input.readUInt32Le()
        if (input.readFourCc() != "WAVE") throw UnsupportedWavException("O cabeçalho WAVE é inválido.")

        var parsedFormat: WavPcmFormat? = null
        while (true) {
            val chunkId = input.readFourCc()
            val chunkSize = input.readUInt32Le()
            when (chunkId) {
                "fmt " -> {
                    if (chunkSize < 16L || chunkSize > 1_048_576L) throw UnsupportedWavException("Bloco de formato WAV inválido.")
                    val audioFormat = input.readUInt16Le()
                    val channels = input.readUInt16Le()
                    val sampleRate = input.readUInt32Le().toInt()
                    input.readUInt32Le() // byte rate
                    val blockAlign = input.readUInt16Le()
                    val bits = input.readUInt16Le()
                    input.skipExactly(chunkSize - 16L)
                    if (audioFormat != 1) throw UnsupportedWavException("Somente WAV PCM sem compressão pode ser convertido.")
                    if (channels !in 1..2) throw UnsupportedWavException("A conversão aceita WAV mono ou estéreo.")
                    if (sampleRate !in 8_000..192_000) throw UnsupportedWavException("A taxa de amostragem do WAV não é suportada.")
                    if (bits !in setOf(8, 16, 24, 32)) throw UnsupportedWavException("WAV PCM de $bits bits não é suportado.")
                    val expectedAlign = channels * (bits / 8)
                    if (blockAlign != expectedAlign) throw UnsupportedWavException("O alinhamento PCM do WAV é inválido.")
                    parsedFormat = WavPcmFormat(channels, sampleRate, bits, blockAlign, 0)
                }
                "data" -> {
                    val fmt = parsedFormat ?: throw UnsupportedWavException("O bloco de áudio aparece antes do formato WAV.")
                    if (chunkSize <= 0L) throw UnsupportedWavException("O WAV não contém áudio.")
                    return fmt.copy(dataSize = chunkSize)
                }
                else -> input.skipExactly(chunkSize)
            }
            if (chunkSize and 1L == 1L) input.skipExactly(1)
        }
    }

    fun decodeToShorts(bytes: ByteArray, length: Int): ShortArray {
        if (length % format.blockAlign != 0) throw UnsupportedWavException("Dados PCM truncados.")
        val bytesPerSample = format.bitsPerSample / 8
        val output = ShortArray(length / bytesPerSample)
        var source = 0
        for (index in output.indices) {
            val value = when (format.bitsPerSample) {
                8 -> ((bytes[source].toInt() and 0xff) - 128) shl 8
                16 -> (bytes[source].toInt() and 0xff) or (bytes[source + 1].toInt() shl 8)
                24 -> {
                    val raw = (bytes[source].toInt() and 0xff) or
                        ((bytes[source + 1].toInt() and 0xff) shl 8) or
                        (bytes[source + 2].toInt() shl 16)
                    raw shr 8
                }
                32 -> {
                    val raw = (bytes[source].toInt() and 0xff) or
                        ((bytes[source + 1].toInt() and 0xff) shl 8) or
                        ((bytes[source + 2].toInt() and 0xff) shl 16) or
                        (bytes[source + 3].toInt() shl 24)
                    raw shr 16
                }
                else -> error("Profundidade já validada")
            }
            output[index] = value.toShort()
            source += bytesPerSample
        }
        return output
    }
}

internal fun InputStream.readChunk(buffer: ByteArray, requested: Int): Int {
    var total = 0
    while (total < requested) {
        val count = read(buffer, total, requested - total)
        if (count < 0) break
        if (count == 0) continue
        total += count
    }
    return total
}

private fun InputStream.readFourCc(): String {
    val bytes = ByteArray(4)
    if (readChunk(bytes, bytes.size) != bytes.size) throw EOFException("Cabeçalho WAV truncado.")
    return bytes.toString(Charsets.US_ASCII)
}

private fun InputStream.readUInt16Le(): Int {
    val low = read()
    val high = read()
    if (low < 0 || high < 0) throw EOFException("Cabeçalho WAV truncado.")
    return low or (high shl 8)
}

private fun InputStream.readUInt32Le(): Long {
    var result = 0L
    repeat(4) { shift ->
        val value = read()
        if (value < 0) throw EOFException("Cabeçalho WAV truncado.")
        result = result or (value.toLong() shl (shift * 8))
    }
    return result
}

private fun InputStream.skipExactly(count: Long) {
    var remaining = count
    val scratch = ByteArray(8_192)
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
        } else {
            val read = read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("Bloco WAV truncado.")
            remaining -= read
        }
    }
}
