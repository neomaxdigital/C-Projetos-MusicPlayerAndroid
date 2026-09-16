package com.musicplayer.offline.music

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class MusicRepository(private val context: Context) {
    fun loadSongs(includeUnknown: Boolean = false): List<Song> {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val genresBySongId = loadGenres(resolver)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE
        )
        return buildList {
            val mimePlaceholders = AudioFileSupport.recognizedMimeTypes.joinToString(",") { "?" }
            val selection = if (includeUnknown) {
                "${MediaStore.Audio.Media.DURATION} > 0"
            } else {
                "${MediaStore.Audio.Media.DURATION} > 0 AND (" +
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                    "${MediaStore.Audio.Media.MIME_TYPE} IN ($mimePlaceholders) OR " +
                    "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR " +
                    "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?)"
            }
            val selectionArgs = if (includeUnknown) null else
                (AudioFileSupport.recognizedMimeTypes + listOf("%.mp3", "%.wav")).toTypedArray()
            val cursor = resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.Audio.Media.TITLE} ASC")
                ?: error("MediaStore não retornou um cursor de áudio")
            cursor.use {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    val displayName = cursor.getString(displayNameIndex).orEmpty()
                    add(
                        Song(
                            id = id,
                            title = AudioFileSupport.title(cursor.getString(titleIndex), displayName),
                            artist = AudioFileSupport.artist(cursor.getString(artistIndex)),
                            uri = uri,
                            artwork = null,
                            album = AudioFileSupport.album(cursor.getString(albumIndex)),
                            albumId = cursor.getLong(albumIdIndex),
                            duration = cursor.getLong(durationIndex),
                            dateAdded = cursor.getLong(dateAddedIndex),
                            trackNumber = cursor.getInt(trackIndex),
                            relativePath = cursor.getString(relativePathIndex).orEmpty(),
                            genre = genresBySongId[id] ?: UNKNOWN_GENRE,
                            displayName = displayName,
                            mimeType = AudioFileSupport.mimeType(cursor.getString(mimeTypeIndex), displayName),
                            fileSize = cursor.getLong(sizeIndex)
                        )
                    )
                }
            }
        }
    }
    private fun loadGenres(resolver: ContentResolver): Map<Long, String> = runCatching {
        buildMap {
            val genresUri = MediaStore.Audio.Genres.getContentUri(MediaStore.VOLUME_EXTERNAL)
            resolver.query(
                genresUri,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null,
                null,
                MediaStore.Audio.Genres.NAME
            )?.use { genreCursor ->
                val idIndex = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameIndex = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(idIndex)
                    val name = genreCursor.getString(nameIndex).orEmpty().ifBlank { UNKNOWN_GENRE }
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, genreId)
                    resolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null
                    )?.use { memberCursor ->
                        val audioIdIndex = memberCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (memberCursor.moveToNext()) putIfAbsent(memberCursor.getLong(audioIdIndex), name)
                    }
                }
            }
        }
    }.getOrDefault(emptyMap())
}
