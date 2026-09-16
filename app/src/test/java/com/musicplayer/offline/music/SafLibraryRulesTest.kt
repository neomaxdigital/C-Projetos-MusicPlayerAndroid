package com.musicplayer.offline.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafLibraryRulesTest {
    @Test fun uriBasedIdsAreStableAndNegative() {
        val uri = "content://example.provider/document/music%3Atrack.mp3"
        val id = SafSongIds.forUriString(uri)

        assertEquals(id, SafSongIds.forUriString(uri))
        assertNotEquals(id, SafSongIds.forUriString("content://example.provider/document/music%3Aother.mp3"))
        assertTrue(id < 0L)
    }

    @Test fun fallbackDeduplicationNeedsNameDurationAndSize() {
        assertEquals("track.mp3|245000|4096", LibrarySongMerge.fallbackKey("Track.MP3", "ignored", 245_000L, 4_096L))
        assertNull(LibrarySongMerge.fallbackKey("track.mp3", "track", 0L, 4_096L))
        assertNull(LibrarySongMerge.fallbackKey("track.mp3", "track", 245_000L, 0L))
    }
}
