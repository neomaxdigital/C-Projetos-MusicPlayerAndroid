package com.musicplayer.offline.music

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueOrderRulesTest {
    @Test fun queueItemsKeepRequestedOrder() {
        assertEquals(listOf(10L, 30L, 20L), QueueOrderRules.move(listOf(10L, 20L, 30L), 2, 1))
    }

    @Test fun invalidMoveKeepsQueueUnchanged() {
        val queue = listOf(1L, 2L)
        assertEquals(queue, QueueOrderRules.move(queue, -1, 1))
    }
}
