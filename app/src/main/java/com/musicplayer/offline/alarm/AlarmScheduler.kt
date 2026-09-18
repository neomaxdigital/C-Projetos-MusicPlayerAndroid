package com.musicplayer.offline.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

object AlarmScheduler {
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java)
        return manager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:" + context.packageName)
        )
    }

    fun schedule(context: Context, alarm: MusicAlarm): Boolean {
        if (!alarm.enabled || alarm.tracks.isEmpty()) {
            cancel(context, alarm.id)
            return false
        }
        if (!canScheduleExact(context)) return false

        val manager = context.getSystemService(AlarmManager::class.java)
        manager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerMillis(alarm),
            pendingIntent(context, alarm.id, isSnooze = false)
        )
        return true
    }

    fun scheduleSnooze(context: Context, alarmId: String, minutes: Int) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + minutes.coerceIn(1, 60) * 60_000L
        val intent = pendingIntent(context, alarmId, isSnooze = true)
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun cancel(context: Context, alarmId: String) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(pendingIntent(context, alarmId, isSnooze = false))
        manager.cancel(pendingIntent(context, alarmId, isSnooze = true))
    }

    fun rescheduleAll(context: Context) {
        val repository = MusicAlarmRepository(context)
        repository.alarms().filter { it.enabled }.forEach { schedule(context, it) }
    }

    internal fun nextTriggerMillis(
        alarm: MusicAlarm,
        now: ZonedDateTime = ZonedDateTime.now()
    ): Long {
        val activeDays = alarm.days.ifEmpty { (1..7).toSet() }
        val time = LocalTime.of(alarm.hour.coerceIn(0, 23), alarm.minute.coerceIn(0, 59))
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val day = DayOfWeek.from(date).value
            if (day !in activeDays) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now.plusSeconds(2))) return candidate.toInstant().toEpochMilli()
        }
        return now.plusDays(1).withHour(alarm.hour).withMinute(alarm.minute)
            .withSecond(0).withNano(0).toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, alarmId: String, isSnooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isSnooze) ACTION_SNOOZE_TRIGGER else ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val requestCode = alarmId.hashCode() * 31 + if (isSnooze) 1 else 0
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_IS_SNOOZE = "alarm_is_snooze"
    private const val ACTION_ALARM_TRIGGER = "com.musicplayer.offline.ALARM_TRIGGER"
    private const val ACTION_SNOOZE_TRIGGER = "com.musicplayer.offline.ALARM_SNOOZE_TRIGGER"
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
        val alarm = MusicAlarmRepository(context).find(alarmId) ?: return
        if (!alarm.enabled && !isSnooze) return

        if (!isSnooze) AlarmScheduler.schedule(context, alarm)

        val serviceIntent = Intent(context, AlarmPlaybackService::class.java).apply {
            action = AlarmPlaybackService.ACTION_START
            putExtra(AlarmPlaybackService.EXTRA_ALARM_ID, alarmId)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
    }
}

class AlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            AlarmScheduler.rescheduleAll(context)
        }
    }
}
