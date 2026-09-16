package com.musicplayer.offline.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSessionCodecTest {
    @Test fun sessionRoundTripPreservesQueueAndPlaybackState() {
        val expected = PlaybackSnapshot(listOf(11, 22, 33), 1, 91_234, 2, true, false)
        assertEquals(expected, PlaybackSnapshotCodec.decode(PlaybackSnapshotCodec.encode(expected)))
    }

    @Test fun damagedSessionIsIgnored() {
        assertNull(PlaybackSnapshotCodec.decode("1,2;bad;data"))
    }

    @Test fun invalidRepeatModeFallsBackToOff() {
        assertEquals(0, PlaybackSnapshotCodec.decode("1;0;0;99;false;false")?.repeatMode)
    }
}
