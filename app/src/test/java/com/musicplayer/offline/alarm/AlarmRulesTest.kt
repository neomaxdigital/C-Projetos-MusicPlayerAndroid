package com.musicplayer.offline.alarm

import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRulesTest {
    @Test fun gradualVolumeNeverDropsAndStopsAtSelectedMaximum() {
        for (percent in 10..100) {
            val target = percent / 100f
            val ramp = (0..15).map { alarmRampVolume(target, it) }
            assertTrue(ramp.zipWithNext().all { (previous, next) -> next >= previous })
            assertEquals(target, ramp.last(), 0.00001f)
            assertTrue(ramp.all { it in 0f..target })
        }
    }

    @Test fun alarmAndPlaylistSurviveSerialization() {
        val alarm = MusicAlarm(
            label = "Acordar | café ☀", sourceType = AlarmSourceType.PLAYLIST,
            sourceTitle = "Minha playlist", days = setOf(1, 5, 7), snoozeMinutes = 5,
            tracks = listOf(AlarmTrack(-42, "content://docs/track", "A;B~C", "Artista", "audio/mpeg"))
        )
        assertEquals(listOf(alarm), MusicAlarmCodec.decode(MusicAlarmCodec.encode(listOf(alarm))))
    }

    @Test fun malformedSavedAlarmDoesNotCrash() {
        assertTrue(MusicAlarmCodec.decode("invalid|data").isEmpty())
    }

    @Test fun scheduleUsesSelectedDayAndFutureTime() {
        val now = ZonedDateTime.parse("2026-09-18T10:00:00-03:00[America/Sao_Paulo]")
        val today = MusicAlarm(hour = 10, minute = 2, days = setOf(5))
        assertEquals(now.plusMinutes(2).toInstant().toEpochMilli(), AlarmScheduler.nextTriggerMillis(today, now))
        assertEquals(now.plusWeeks(1).toInstant().toEpochMilli(),
            AlarmScheduler.nextTriggerMillis(today.copy(minute = 0), now))
    }
}
