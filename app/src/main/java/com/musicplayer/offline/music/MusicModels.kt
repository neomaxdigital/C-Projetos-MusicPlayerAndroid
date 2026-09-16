package com.musicplayer.offline.music

import android.graphics.Bitmap
import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val artwork: Bitmap?,
    val album: String = UNKNOWN_ALBUM,
    val albumId: Long = -1L,
    val duration: Long = 0L,
    val dateAdded: Long = 0L,
    val trackNumber: Int = 0,
    val relativePath: String = "",
    val genre: String = UNKNOWN_GENRE,
    val displayName: String = "",
    val mimeType: String = ""
)

data class ArtistGroup(
    val name: String,
    val songs: List<Song>
) {
    val artwork: Bitmap? get() = songs.firstNotNullOfOrNull { it.artwork }
    val artworkUri: Uri? get() = songs.firstOrNull()?.uri
}

data class AlbumGroup(
    val key: String,
    val name: String,
    val artist: String,
    val songs: List<Song>
) {
    val artwork: Bitmap? get() = songs.firstNotNullOfOrNull { it.artwork }
    val artworkUri: Uri? get() = songs.firstOrNull()?.uri
}

data class FolderGroup(
    val path: String,
    val name: String,
    val songs: List<Song>
) {
    val artwork: Bitmap? get() = songs.firstNotNullOfOrNull { it.artwork }
    val artworkUri: Uri? get() = songs.firstOrNull()?.uri
}

data class GenreGroup(
    val name: String,
    val songs: List<Song>
) {
    val artwork: Bitmap? get() = songs.firstNotNullOfOrNull { it.artwork }
    val artworkUri: Uri? get() = songs.firstOrNull()?.uri
}

enum class SongSort(val label: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DURATION("Duração"),
    DATE_ADDED("Data adicionada")
}

const val UNKNOWN_ARTIST = "Artista desconhecido"
const val UNKNOWN_ALBUM = "Álbum desconhecido"
const val UNKNOWN_GENRE = "Sem gênero"

fun List<Song>.sortedByOption(option: SongSort): List<Song> = when (option) {
    SongSort.TITLE -> sortedWith(compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.ARTIST -> sortedWith(compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.ALBUM -> sortedWith(compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }.thenBy { normalizedTrack(it.trackNumber) }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.DURATION -> sortedWith(compareBy<Song> { it.duration }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.DATE_ADDED -> sortedWith(compareByDescending<Song> { it.dateAdded }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}

fun List<Song>.asArtists(): List<ArtistGroup> =
    groupBy { it.artist }
        .map { (artist, songs) -> ArtistGroup(artist, songs.sortedByOption(SongSort.TITLE)) }
        .sortedWith(compareBy<ArtistGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<Song>.asAlbums(): List<AlbumGroup> =
    groupBy { song -> if (song.albumId >= 0) "id:${song.albumId}" else "${song.album}\u0000${song.artist}" }
        .map { (key, songs) ->
            val ordered = songs.sortedWith(
                compareBy<Song> { normalizedTrack(it.trackNumber) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            AlbumGroup(key, ordered.first().album, representativeAlbumArtist(ordered), ordered)
        }
        .sortedWith(compareBy<AlbumGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<Song>.asFolders(): List<FolderGroup> =
    groupBy { it.relativePath.ifBlank { "Armazenamento" } }
        .map { (path, songs) ->
            FolderGroup(
                path = path,
                name = path.trimEnd('/').substringAfterLast('/').ifBlank { "Armazenamento" },
                songs = songs.sortedByOption(SongSort.TITLE)
            )
        }
        .sortedWith(compareBy<FolderGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<Song>.asGenres(): List<GenreGroup> =
    groupBy { it.genre.ifBlank { UNKNOWN_GENRE } }
        .map { (genre, songs) -> GenreGroup(genre, songs.sortedByOption(SongSort.TITLE)) }
        .sortedWith(compareBy<GenreGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.name })

private fun normalizedTrack(track: Int): Int = if (track > 0) track else Int.MAX_VALUE

private fun representativeAlbumArtist(songs: List<Song>): String {
    val artists = songs.map { it.artist }.distinct()
    return if (artists.size == 1) artists.first() else "Vários artistas"
}
