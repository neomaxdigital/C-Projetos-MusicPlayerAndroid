package com.musicplayer.offline.lyrics

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.musicplayer.offline.music.Song
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class LyricsRepository(private val context: Context) {
    fun load(song: Song): LyricsDocument? {
        val sidecar = loadSidecar(song)
        if (!sidecar.isNullOrBlank()) return LrcParser.parse(sidecar)
        val embedded = loadEmbeddedUslt(song.uri)
        return embedded?.takeIf(String::isNotBlank)?.let(LrcParser::parse)
    }

    private fun loadSidecar(song: Song): String? = runCatching {
        val base = song.displayName.substringBeforeLast('.', song.displayName).ifBlank { song.title }
        val names = arrayOf("$base.lrc", "$base.LRC")
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME),
            "${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} IN (?,?)",
            arrayOf(song.relativePath, names[0], names[1]), null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            context.contentResolver.openInputStream(Uri.withAppendedPath(collection, id.toString()))?.bufferedReader()?.use { it.readText() }
        }
    }.getOrNull()

    private fun loadEmbeddedUslt(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            val header = ByteArray(10)
            if (input.read(header) != 10 || String(header, 0, 3, Charsets.ISO_8859_1) != "ID3") return@use null
            val version = header[3].toInt()
            val tagSize = synchsafe(header, 6)
            if (tagSize <= 0 || tagSize > 4_000_000) return@use null
            val tag = ByteArray(tagSize)
            var read = 0
            while (read < tag.size) { val count = input.read(tag, read, tag.size - read); if (count < 0) break; read += count }
            findUslt(tag.copyOf(read), version)
        }
    }.getOrNull()

    private fun findUslt(data: ByteArray, version: Int): String? {
        var offset = 0
        while (offset + 10 <= data.size) {
            val id = String(data, offset, 4, Charsets.ISO_8859_1)
            if (id.all { it == '\u0000' }) break
            val size = if (version == 4) synchsafe(data, offset + 4) else ByteBuffer.wrap(data, offset + 4, 4).order(ByteOrder.BIG_ENDIAN).int
            if (size <= 0 || offset + 10 + size > data.size) break
            if (id == "USLT") return decodeUslt(data.copyOfRange(offset + 10, offset + 10 + size))
            offset += 10 + size
        }
        return null
    }

    private fun decodeUslt(frame: ByteArray): String? {
        if (frame.size < 5) return null
        val charset = when (frame[0].toInt()) { 1 -> Charsets.UTF_16; 2 -> Charset.forName("UTF-16BE"); 3 -> Charsets.UTF_8; else -> Charsets.ISO_8859_1 }
        val delimiter = if (frame[0].toInt() in 1..2) byteArrayOf(0, 0) else byteArrayOf(0)
        var cursor = 4
        while (cursor + delimiter.size <= frame.size && !delimiter.indices.all { frame[cursor + it] == delimiter[it] }) cursor++
        cursor = (cursor + delimiter.size).coerceAtMost(frame.size)
        return String(frame, cursor, frame.size - cursor, charset).trim('\u0000', '\ufeff', ' ', '\r', '\n')
    }

    private fun synchsafe(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0x7f) shl 21) or ((bytes[offset + 1].toInt() and 0x7f) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7f) shl 7) or (bytes[offset + 3].toInt() and 0x7f)
}
