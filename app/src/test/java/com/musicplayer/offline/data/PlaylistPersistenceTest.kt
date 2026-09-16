package com.musicplayer.offline.data

import com.musicplayer.offline.music.LocalPlaylist
import com.musicplayer.offline.music.PlaylistRules
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistPersistenceTest {
    @Test fun codecPreservesMultiplePlaylistsNamesAndOrder() {
        val original = listOf(
            LocalPlaylist("one", "Estrada | Noite", listOf(3L, 1L, 2L)),
            LocalPlaylist("two", "Favoritas brasileiras", listOf(9L))
        )
        assertEquals(original, PlaylistCodec.decode(PlaylistCodec.encode(original)))
    }

    @Test fun addingSongAvoidsDuplicatesAndRemovalWorks() {
        val playlist = LocalPlaylist("id", "Teste", listOf(1L, 2L))
        assertEquals(listOf(1L, 2L), PlaylistRules.addSong(playlist, 2L).songIds)
        assertEquals(listOf(2L), PlaylistRules.removeSong(playlist, 1L).songIds)
    }

    @Test fun playlistOrderCanBeChanged() {
        val playlist = LocalPlaylist("id", "Teste", listOf(1L, 2L, 3L))
        assertEquals(listOf(2L, 3L, 1L), PlaylistRules.moveSong(playlist, 0, 2).songIds)
    }

    @Test fun fiveSongDragScenariosPersistInCodec() {
        val original = LocalPlaylist("drag", "Cinco músicas", listOf(1L, 2L, 3L, 4L, 5L))
        val firstToEnd = PlaylistRules.moveSong(original, 0, 4)
        assertEquals(listOf(2L, 3L, 4L, 5L, 1L), firstToEnd.songIds)
        val lastToStart = PlaylistRules.moveSong(firstToEnd, 4, 0)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), lastToStart.songIds)
        val onePosition = PlaylistRules.moveSong(lastToStart, 2, 1)
        assertEquals(listOf(1L, 3L, 2L, 4L, 5L), onePosition.songIds)
        assertEquals(listOf(onePosition), PlaylistCodec.decode(PlaylistCodec.encode(listOf(onePosition))))
    }

    @Test fun removedDeviceSongsArePrunedFromPlaylist() {
        val playlist = LocalPlaylist("id", "Teste", listOf(4L, 2L, 9L))
        assertEquals(listOf(4L, 9L), PlaylistRules.retainSongs(playlist, setOf(4L, 9L)).songIds)
    }
}
