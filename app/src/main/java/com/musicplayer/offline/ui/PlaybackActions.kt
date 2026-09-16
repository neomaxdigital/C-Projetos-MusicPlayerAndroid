package com.musicplayer.offline.ui

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.musicplayer.offline.music.Song

fun playSong(player: Player?, queue: List<Song>, selected: Song) {
    if (player == null || queue.isEmpty()) return
    val items = queue.map(Song::toMediaItem)
    player.setMediaItems(items, queue.indexOf(selected).coerceAtLeast(0), 0L)
    player.prepare()
    player.play()
}

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setMediaId(id.toString())
    .setMimeType(mimeType.ifBlank { null })
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(uri)
            .build()
    )
    .build()

fun playNext(player: Player?, song: Song) {
    if (player == null) return
    if (player.mediaItemCount == 0) {
        player.setMediaItem(song.toMediaItem())
        player.prepare()
        player.play()
    } else {
        player.addMediaItem((player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount), song.toMediaItem())
    }
}

fun addToQueue(player: Player?, song: Song) {
    player?.addMediaItem(song.toMediaItem())
}

fun clearQueueKeepingCurrent(player: Player?) {
    if (player == null || player.mediaItemCount == 0) return
    val current = player.currentMediaItemIndex.coerceAtLeast(0)
    if (current + 1 < player.mediaItemCount) player.removeMediaItems(current + 1, player.mediaItemCount)
    if (current > 0) player.removeMediaItems(0, current)
}

fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
