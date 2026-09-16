package com.musicplayer.offline.conversion

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

internal class LameMp3Encoder(
    private val channels: Int,
    sampleRate: Int,
    bitrateKbps: Int
) : AutoCloseable {
    private var handle: Pointer? = api.lame_init()?.takeUnless { Pointer.nativeValue(it) == 0L }
        ?: error("Não foi possível iniciar o encoder LAME.")

    init {
        val encoder = requireNotNull(handle)
        checkResult(api.lame_set_num_channels(encoder, channels), "canais")
        checkResult(api.lame_set_in_samplerate(encoder, sampleRate), "sample rate")
        checkResult(api.lame_set_out_samplerate(encoder, sampleRate), "sample rate de saída")
        checkResult(api.lame_set_mode(encoder, if (channels == 1) MODE_MONO else MODE_JOINT_STEREO), "modo")
        checkResult(api.lame_set_VBR(encoder, VBR_OFF), "CBR")
        checkResult(api.lame_set_brate(encoder, bitrateKbps), "bitrate")
        checkResult(api.lame_set_quality(encoder, 5), "qualidade")
        checkResult(api.lame_set_bWriteVbrTag(encoder, 0), "cabeçalho VBR")
        checkResult(api.lame_init_params(encoder), "parâmetros")
    }

    fun encode(interleavedPcm: ShortArray): ByteArray {
        val encoder = requireNotNull(handle) { "Encoder já fechado." }
        val frames = interleavedPcm.size / channels
        val output = ByteArray((frames * 5 / 4 + 7_200).coerceAtLeast(8_192))
        val written = if (channels == 1) {
            api.lame_encode_buffer(encoder, interleavedPcm, null, frames, output, output.size)
        } else {
            api.lame_encode_buffer_interleaved(encoder, interleavedPcm, frames, output, output.size)
        }
        if (written < 0) error("O encoder LAME retornou o erro $written.")
        return output.copyOf(written)
    }

    fun flush(): ByteArray {
        val encoder = requireNotNull(handle) { "Encoder já fechado." }
        val output = ByteArray(7_200)
        val written = api.lame_encode_flush(encoder, output, output.size)
        if (written < 0) error("Não foi possível finalizar o MP3 (erro $written).")
        return output.copyOf(written)
    }

    override fun close() {
        handle?.let(api::lame_close)
        handle = null
    }

    private fun checkResult(result: Int, field: String) {
        if (result < 0) error("Configuração LAME inválida: $field ($result).")
    }

    private interface LameNative : Library {
        fun lame_init(): Pointer?
        fun lame_set_num_channels(handle: Pointer, channels: Int): Int
        fun lame_set_in_samplerate(handle: Pointer, sampleRate: Int): Int
        fun lame_set_out_samplerate(handle: Pointer, sampleRate: Int): Int
        fun lame_set_mode(handle: Pointer, mode: Int): Int
        fun lame_set_VBR(handle: Pointer, mode: Int): Int
        fun lame_set_brate(handle: Pointer, bitrate: Int): Int
        fun lame_set_quality(handle: Pointer, quality: Int): Int
        fun lame_set_bWriteVbrTag(handle: Pointer, enabled: Int): Int
        fun lame_init_params(handle: Pointer): Int
        fun lame_encode_buffer(
            handle: Pointer,
            pcmLeft: ShortArray,
            pcmRight: ShortArray?,
            frames: Int,
            output: ByteArray,
            outputSize: Int
        ): Int
        fun lame_encode_buffer_interleaved(
            handle: Pointer,
            pcm: ShortArray,
            frames: Int,
            output: ByteArray,
            outputSize: Int
        ): Int
        fun lame_encode_flush(handle: Pointer, output: ByteArray, outputSize: Int): Int
        fun lame_close(handle: Pointer): Int
    }

    companion object {
        private const val MODE_JOINT_STEREO = 1
        private const val MODE_MONO = 3
        private const val VBR_OFF = 0

        private val api: LameNative by lazy {
            System.loadLibrary("lame")
            Native.load("lame", LameNative::class.java)
        }
    }
}
