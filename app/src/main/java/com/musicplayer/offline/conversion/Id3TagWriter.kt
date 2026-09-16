package com.musicplayer.offline.conversion

import java.io.ByteArrayOutputStream
import java.io.OutputStream

data class Mp3Metadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: String?,
    val track: Int?,
    val artwork: ByteArray?
)

internal object Id3TagWriter {
    fun write(output: OutputStream, metadata: Mp3Metadata) {
        val frames = ByteArrayOutputStream()
        textFrame(frames, "TIT2", metadata.title)
        textFrame(frames, "TPE1", metadata.artist)
        textFrame(frames, "TALB", metadata.album)
        textFrame(frames, "TYER", metadata.year)
        textFrame(frames, "TRCK", metadata.track?.takeIf { it > 0 }?.toString())
        metadata.artwork?.takeIf { it.isNotEmpty() }?.let { artworkFrame(frames, it) }

        val body = frames.toByteArray()
        output.write(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0))
        output.write(syncSafe(body.size))
        output.write(body)
    }

    private fun textFrame(output: ByteArrayOutputStream, id: String, value: String?) {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return
        val payload = byteArrayOf(1) + text.toByteArray(Charsets.UTF_16)
        frame(output, id, payload)
    }

    private fun artworkFrame(output: ByteArrayOutputStream, artwork: ByteArray) {
        val mime = when {
            artwork.size >= 8 && artwork.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)) -> "image/png"
            else -> "image/jpeg"
        }
        val payload = ByteArrayOutputStream().apply {
            write(0) // ISO-8859-1 for MIME and description
            write(mime.toByteArray(Charsets.ISO_8859_1))
            write(0)
            write(3) // front cover
            write(0) // empty description
            write(artwork)
        }.toByteArray()
        frame(output, "APIC", payload)
    }

    private fun frame(output: ByteArrayOutputStream, id: String, payload: ByteArray) {
        output.write(id.toByteArray(Charsets.US_ASCII))
        output.write(byteArrayOf(
            (payload.size ushr 24).toByte(), (payload.size ushr 16).toByte(),
            (payload.size ushr 8).toByte(), payload.size.toByte(), 0, 0
        ))
        output.write(payload)
    }

    private fun syncSafe(size: Int) = byteArrayOf(
        ((size ushr 21) and 0x7f).toByte(), ((size ushr 14) and 0x7f).toByte(),
        ((size ushr 7) and 0x7f).toByte(), (size and 0x7f).toByte()
    )
}
