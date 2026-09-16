package com.musicplayer.offline.conversion

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.musicplayer.offline.music.AudioFileSupport
import com.musicplayer.offline.music.Song
import com.musicplayer.offline.music.UNKNOWN_ALBUM
import com.musicplayer.offline.music.UNKNOWN_ARTIST
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConversionState {
    data object Idle : ConversionState
    data class Running(val song: Song, val progress: Int, val shareAfter: Boolean) : ConversionState
    data class Success(val song: Song, val uri: Uri, val displayName: String, val shareAfter: Boolean) : ConversionState
    data class Failure(val song: Song, val message: String) : ConversionState
    data class Cancelled(val song: Song) : ConversionState
}

object AudioConversionManager {
    const val DEFAULT_BITRATE_KBPS = 128
    const val OUTPUT_FOLDER = "Music/JUKE/Convertidos/"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val state: StateFlow<ConversionState> = mutableState.asStateFlow()
    private var activeJob: Job? = null

    @Synchronized
    fun start(context: Context, song: Song, shareAfter: Boolean, bitrateKbps: Int = DEFAULT_BITRATE_KBPS) {
        if (activeJob?.isActive == true) return
        val appContext = context.applicationContext
        activeJob = scope.launch {
            mutableState.value = ConversionState.Running(song, 0, shareAfter)
            var outputUri: Uri? = null
            try {
                require(AudioFileSupport.isWav(song)) { "Esta faixa não é um arquivo WAV." }
                val result = convert(appContext, song, bitrateKbps) { progress ->
                    mutableState.value = ConversionState.Running(song, progress, shareAfter)
                }
                outputUri = result.first
                mutableState.value = ConversionState.Success(song, result.first, result.second, shareAfter)
            } catch (_: CancellationException) {
                outputUri?.let { appContext.contentResolver.delete(it, null, null) }
                mutableState.value = ConversionState.Cancelled(song)
            } catch (error: Throwable) {
                outputUri?.let { appContext.contentResolver.delete(it, null, null) }
                mutableState.value = ConversionState.Failure(song, error.userMessage())
            }
        }
    }

    fun cancel() {
        val job = activeJob ?: return
        scope.launch { job.cancelAndJoin() }
    }

    fun dismiss() {
        if (mutableState.value !is ConversionState.Running) mutableState.value = ConversionState.Idle
    }

    private suspend fun convert(
        context: Context,
        song: Song,
        bitrateKbps: Int,
        onProgress: (Int) -> Unit
    ): Pair<Uri, String> {
        val resolver = context.contentResolver
        val baseName = song.displayName.ifBlank { song.title }.substringBeforeLast('.').sanitizeFileName()
        val displayName = uniqueDisplayName(context, baseName)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, OUTPUT_FOLDER)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
            put(MediaStore.Audio.Media.TITLE, song.title)
            if (song.artist != UNKNOWN_ARTIST) put(MediaStore.Audio.Media.ARTIST, song.artist)
            if (song.album != UNKNOWN_ALBUM) put(MediaStore.Audio.Media.ALBUM, song.album)
            if (song.trackNumber > 0) put(MediaStore.Audio.Media.TRACK, song.trackNumber)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val outputUri = resolver.insert(collection, values) ?: throw IOException("Não foi possível criar o MP3 no armazenamento.")
        var completed = false
        try {
            val metadata = readMetadata(context, song)
            resolver.openInputStream(song.uri)?.buffered()?.use { source ->
                val wav = WavPcmReader(source)
                resolver.openOutputStream(outputUri, "w")?.buffered()?.use { output ->
                    Id3TagWriter.write(output, metadata)
                    LameMp3Encoder(wav.format.channels, wav.format.sampleRate, bitrateKbps).use { lame ->
                        val chunkSize = 64 * 1024 - (64 * 1024 % wav.format.blockAlign)
                        val buffer = ByteArray(chunkSize)
                        var remaining = wav.format.dataSize
                        var consumed = 0L
                        while (remaining > 0) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val requested = minOf(buffer.size.toLong(), remaining).toInt()
                            val count = source.readChunk(buffer, requested)
                            if (count != requested) throw IOException("O áudio WAV está truncado.")
                            val pcm = wav.decodeToShorts(buffer, count)
                            val encoded = lame.encode(pcm)
                            if (encoded.isNotEmpty()) output.write(encoded)
                            consumed += count
                            remaining -= count
                            onProgress(((consumed * 100L) / wav.format.dataSize).toInt().coerceIn(0, 99))
                        }
                        val flushed = lame.flush()
                        if (flushed.isNotEmpty()) output.write(flushed)
                        output.flush()
                    }
                } ?: throw IOException("Não foi possível escrever o MP3.")
            } ?: throw IOException("Não foi possível ler o arquivo WAV.")
            resolver.update(outputUri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            completed = true
            onProgress(100)
            return outputUri to displayName
        } finally {
            if (!completed) resolver.delete(outputUri, null, null)
        }
    }

    private fun uniqueDisplayName(context: Context, rawBaseName: String): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        var suffix = 0
        while (true) {
            val name = if (suffix == 0) "$rawBaseName.mp3" else "$rawBaseName ($suffix).mp3"
            val exists = resolver.query(
                collection,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.RELATIVE_PATH} = ? AND ${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                arrayOf(OUTPUT_FOLDER, name),
                null
            )?.use { it.moveToFirst() } == true
            if (!exists) return name
            suffix++
        }
    }

    private fun readMetadata(context: Context, song: Song): Mp3Metadata {
        var year: String? = null
        var artwork: ByteArray? = null
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, song.uri)
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                artwork = retriever.embeddedPicture?.takeIf { it.size <= 8 * 1024 * 1024 }
            }
        }
        return Mp3Metadata(
            title = song.title,
            artist = song.artist.takeUnless { it == UNKNOWN_ARTIST },
            album = song.album.takeUnless { it == UNKNOWN_ALBUM },
            year = year,
            track = song.trackNumber.takeIf { it > 0 },
            artwork = artwork
        )
    }

    private fun String.sanitizeFileName(): String = replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim().trimEnd('.').ifBlank { "Audio convertido" }.take(120)

    private fun Throwable.userMessage(): String = when (this) {
        is UnsupportedWavException -> message ?: "Este formato WAV não é suportado para conversão."
        is IllegalArgumentException -> message ?: "O arquivo selecionado não pode ser convertido."
        is OutOfMemoryError -> "Não há memória suficiente para concluir a conversão."
        is IOException -> message ?: "Falha de leitura ou gravação durante a conversão."
        is UnsatisfiedLinkError -> "O encoder MP3 não é compatível com este dispositivo."
        else -> "Não foi possível converter este WAV para MP3."
    }
}
