package com.musicplayer.offline.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerRulesTest {
    @Test fun remainingNeverBecomesNegative() {
        assertEquals(5_000, SleepTimerRules.remaining(15_000, 10_000))
        assertEquals(0, SleepTimerRules.remaining(10_000, 15_000))
    }

    @Test fun fadeOnlyAppliesInsideFinalWindow() {
        assertEquals(1f, SleepTimerRules.fadeFactor(15_000), 0.001f)
        assertEquals(.5f, SleepTimerRules.fadeFactor(5_000), 0.001f)
        assertEquals(0f, SleepTimerRules.fadeFactor(0), 0.001f)
    }
}
