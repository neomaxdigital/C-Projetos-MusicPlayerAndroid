package com.musicplayer.offline.music

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Keeps user-selected SAF documents separate from MediaStore and rebuilds their metadata on
 * every library refresh. A tree URI grants recursive read access without broad storage access.
 */
class SafMusicRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun addFolder(uri: Uri) = rememberUri(FOLDER_URIS_KEY, uri)

    /**
     * Reads the selected document before persisting it so the caller can put a valid Song on
     * screen immediately. The saved URI is rebuilt on every subsequent library refresh.
     */
    fun addSong(uri: Uri): SafSongImportResult = runCatching {
        val song = songFromUri(uri, INDIVIDUAL_SONGS_PATH) ?: return SafSongImportResult.Unsupported
        SafSongImportResult.Imported(song, rememberUri(SONG_URIS_KEY, uri))
    }.getOrDefault(SafSongImportResult.Failed)

    fun loadSongs(): List<Song> = buildList {
        folderUris().forEach { treeUri ->
            runCatching { loadTree(treeUri) }.getOrDefault(emptyList()).forEach(::add)
        }
        songUris().forEach { uri ->
            runCatching { songFromUri(uri, INDIVIDUAL_SONGS_PATH) }.getOrNull()?.let(::add)
        }
    }

    private fun rememberUri(key: String, uri: Uri): Boolean {
        takeReadPermissionIfAvailable(uri)
        val saved = preferences.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        if (!saved.add(uri.toString())) return false
        preferences.edit().putStringSet(key, saved).apply()
        return true
    }

    private fun takeReadPermissionIfAvailable(uri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun folderUris(): List<Uri> = preferences.getStringSet(FOLDER_URIS_KEY, emptySet())
        .orEmpty().mapNotNull(Uri::parse)

    private fun songUris(): List<Uri> = preferences.getStringSet(SONG_URIS_KEY, emptySet())
        .orEmpty().mapNotNull(Uri::parse)

    private fun loadTree(treeUri: Uri): List<Song> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootName = documentName(treeUri).ifBlank { "Pasta adicionada" }
        return loadChildren(treeUri, rootId, "Pastas adicionadas/$rootName")
    }

    private fun loadChildren(treeUri: Uri, parentDocumentId: String, relativePath: String): List<Song> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        return resolver.query(childrenUri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
            buildList {
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex).orEmpty()
                    val mimeType = cursor.getString(mimeIndex).orEmpty()
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        addAll(loadChildren(treeUri, documentId, relativePath))
                    } else {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        songFromUri(
                            uri = documentUri,
                            relativePath = relativePath,
                            knownName = displayName,
                            knownMimeType = mimeType,
                            knownSize = cursor.getLong(sizeIndex),
                            knownModifiedMs = cursor.getLong(modifiedIndex)
                        )?.let(::add)
                    }
                }
            }
        }.orEmpty()
    }

    private fun songFromUri(
        uri: Uri,
        relativePath: String,
        knownName: String? = null,
        knownMimeType: String? = null,
        knownSize: Long? = null,
        knownModifiedMs: Long? = null
    ): Song? {
        val document = documentInfo(uri)
        val displayName = knownName ?: document.name
        val mimeType = AudioFileSupport.mimeType(knownMimeType ?: resolver.getType(uri), displayName)
        if (!AudioFileSupport.isSupported(mimeType, displayName, uri.toString())) return null
        val metadata = metadata(uri)
        val duration = metadata.durationMs
        return Song(
            id = SafSongIds.forUri(uri),
            title = AudioFileSupport.title(metadata.title, displayName),
            artist = AudioFileSupport.artist(metadata.artist),
            uri = uri,
            artwork = null,
            album = AudioFileSupport.album(metadata.album),
            duration = duration,
            dateAdded = ((knownModifiedMs ?: document.modifiedMs) / 1_000L).coerceAtLeast(0L),
            relativePath = relativePath,
            displayName = displayName,
            mimeType = mimeType,
            fileSize = (knownSize ?: document.size).coerceAtLeast(0L)
        )
    }

    private fun documentName(uri: Uri): String = documentInfo(uri).name

    private fun documentInfo(uri: Uri): DocumentInfo = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
        null,
        null,
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use DocumentInfo()
        DocumentInfo(
            name = cursor.stringAt(OpenableColumns.DISPLAY_NAME),
            size = cursor.longAt(OpenableColumns.SIZE),
            modifiedMs = cursor.longAt(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        )
    } ?: DocumentInfo()

    private fun metadata(uri: Uri): Metadata = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(appContext, uri)
            Metadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            )
        }
    }.getOrDefault(Metadata())

    private fun Cursor.stringAt(column: String): String = getColumnIndex(column)
        .takeIf { it >= 0 }?.let(::getString).orEmpty()

    private fun Cursor.longAt(column: String): Long = getColumnIndex(column)
        .takeIf { it >= 0 }?.let(::getLong) ?: 0L

    private data class DocumentInfo(val name: String = "", val size: Long = 0L, val modifiedMs: Long = 0L)
    private data class Metadata(val title: String? = null, val artist: String? = null, val album: String? = null, val durationMs: Long = 0L)

    private companion object {
        const val PREFERENCES_NAME = "saf_music_library"
        const val FOLDER_URIS_KEY = "folder_uris"
        const val SONG_URIS_KEY = "song_uris"
        const val INDIVIDUAL_SONGS_PATH = "Músicas adicionadas"
        val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }
}

sealed interface SafSongImportResult {
    data class Imported(val song: Song, val newlyPersisted: Boolean) : SafSongImportResult
    data object Unsupported : SafSongImportResult
    data object Failed : SafSongImportResult
}

object SafSongIds {
    fun forUri(uri: Uri): Long = forUriString(uri.toString())

    fun forUriString(uri: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray(Charsets.UTF_8))
        val positive = ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long and Long.MAX_VALUE
        return -positive.coerceAtLeast(1L)
    }
}

object LibrarySongMerge {
    fun merge(mediaStoreSongs: List<Song>, safSongs: List<Song>): List<Song> {
        val seenUris = hashSetOf<String>()
        val seenFallbacks = hashSetOf<String>()
        return buildList {
            (mediaStoreSongs + safSongs).forEach { song ->
                val uriAdded = seenUris.add(song.uri.toString())
                val fallback = fallbackKey(song.displayName, song.title, song.duration, song.fileSize)
                if (uriAdded && (fallback == null || seenFallbacks.add(fallback))) add(song)
            }
        }
    }

    internal fun fallbackKey(displayName: String, title: String, duration: Long, fileSize: Long): String? {
        if (duration <= 0L || fileSize <= 0L) return null
        val name = displayName.ifBlank { title }.trim().lowercase()
        return name.takeIf(String::isNotBlank)?.let { "$it|$duration|$fileSize" }
    }
}
