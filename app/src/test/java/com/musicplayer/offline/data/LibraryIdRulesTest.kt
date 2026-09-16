package com.musicplayer.offline.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIdRulesTest {
    @Test fun favoriteToggleDoesNotDuplicateIds() {
        val added = LibraryIdRules.toggleFavorite(setOf(10L), 20L)
        assertEquals(2, added.size)
        assertTrue(20L in added)

        val removed = LibraryIdRules.toggleFavorite(added, 20L)
        assertFalse(20L in removed)
        assertEquals(setOf(10L), removed)
    }

    @Test fun recentMovesExistingSongToTopWithoutDuplicates() {
        val updated = LibraryIdRules.addRecent(listOf(3L, 2L, 1L), 2L)
        assertEquals(listOf(2L, 3L, 1L), updated)
        assertEquals(updated.distinct(), updated)
    }

    @Test fun consecutiveRecentIsIgnored() {
        val current = listOf(5L, 4L, 3L)
        assertEquals(current, LibraryIdRules.addRecent(current, 5L))
    }

    @Test fun removedSongsArePrunedWithoutChangingOrder() {
        val valid = setOf(2L, 4L)
        assertEquals(setOf(2L, 4L), LibraryIdRules.retainFavorites(setOf(1L, 2L, 4L), valid))
        assertEquals(listOf(4L, 2L), LibraryIdRules.retainRecents(listOf(4L, 3L, 2L, 1L), valid))
    }

    @Test fun removedSongsArePrunedFromPlayCounts() {
        assertEquals(
            mapOf(2L to 3, 4L to 1),
            LibraryIdRules.retainPlayCounts(mapOf(1L to 8, 2L to 3, 3L to 0, 4L to 1), setOf(2L, 3L, 4L))
        )
    }
}
