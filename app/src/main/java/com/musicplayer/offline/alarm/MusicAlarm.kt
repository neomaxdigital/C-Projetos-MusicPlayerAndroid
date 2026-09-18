package com.musicplayer.offline.alarm

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

enum class AlarmSourceType { SONG, PLAYLIST }

internal fun alarmRampVolume(targetVolume: Float, step: Int, totalSteps: Int = 15): Float {
    val target = targetVolume.coerceIn(0f, 1f)
    val initial = minOf(0.08f, target)
    val fraction = (step.toFloat() / totalSteps.coerceAtLeast(1)).coerceIn(0f, 1f)
    return initial + (target - initial) * fraction
}

data class AlarmTrack(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val mimeType: String
)

data class MusicAlarm(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Despertador",
    val hour: Int = 7,
    val minute: Int = 0,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5),
    val enabled: Boolean = true,
    val sourceType: AlarmSourceType = AlarmSourceType.SONG,
    val sourceId: String = "",
    val sourceTitle: String = "Escolha uma música",
    val tracks: List<AlarmTrack> = emptyList(),
    val gradualVolume: Boolean = true,
    val volumePercent: Int = 80,
    val snoozeMinutes: Int = 10,
    val vibrate: Boolean = true
)

object AlarmDayLabels {
    val ordered = listOf(
        1 to "Seg",
        2 to "Ter",
        3 to "Qua",
        4 to "Qui",
        5 to "Sex",
        6 to "Sáb",
        7 to "Dom"
    )

    fun summary(days: Set<Int>): String = when {
        days.size == 7 -> "Todos os dias"
        days == setOf(1, 2, 3, 4, 5) -> "Seg a sex"
        days == setOf(6, 7) -> "Fim de semana"
        days.isEmpty() -> "Nenhum dia"
        else -> ordered.filter { it.first in days }.joinToString(", ") { it.second }
    }
}

internal object MusicAlarmCodec {
    fun encode(alarms: List<MusicAlarm>): String = alarms.joinToString("\n") { alarm ->
        listOf(
            alarm.id,
            enc(alarm.label),
            alarm.hour.coerceIn(0, 23).toString(),
            alarm.minute.coerceIn(0, 59).toString(),
            alarm.days.sorted().joinToString(","),
            alarm.enabled.toString(),
            alarm.sourceType.name,
            enc(alarm.sourceId),
            enc(alarm.sourceTitle),
            alarm.gradualVolume.toString(),
            alarm.volumePercent.coerceIn(1, 100).toString(),
            alarm.snoozeMinutes.coerceIn(1, 60).toString(),
            alarm.vibrate.toString(),
            enc(encodeTracks(alarm.tracks))
        ).joinToString("|")
    }

    fun decode(raw: String?): List<MusicAlarm> = raw.orEmpty().lineSequence().mapNotNull { line ->
        val p = line.split('|', limit = 14)
        if (p.size != 14) return@mapNotNull null
        runCatching {
            MusicAlarm(
                id = p[0],
                label = dec(p[1]).ifBlank { "Despertador" },
                hour = p[2].toInt().coerceIn(0, 23),
                minute = p[3].toInt().coerceIn(0, 59),
                days = p[4].split(',').mapNotNull(String::toIntOrNull).filter { it in 1..7 }.toSet(),
                enabled = p[5].toBooleanStrict(),
                sourceType = AlarmSourceType.valueOf(p[6]),
                sourceId = dec(p[7]),
                sourceTitle = dec(p[8]),
                gradualVolume = p[9].toBooleanStrict(),
                volumePercent = p[10].toInt().coerceIn(1, 100),
                snoozeMinutes = p[11].toInt().coerceIn(1, 60),
                vibrate = p[12].toBooleanStrict(),
                tracks = decodeTracks(dec(p[13]))
            )
        }.getOrNull()
    }.toList()

    private fun encodeTracks(tracks: List<AlarmTrack>): String = tracks.joinToString(";") { track ->
        listOf(
            track.id.toString(),
            enc(track.uri),
            enc(track.title),
            enc(track.artist),
            enc(track.mimeType)
        ).joinToString("~")
    }

    private fun decodeTracks(raw: String): List<AlarmTrack> = raw.split(';').mapNotNull { item ->
        if (item.isBlank()) return@mapNotNull null
        val p = item.split('~', limit = 5)
        if (p.size != 5) return@mapNotNull null
        runCatching {
            AlarmTrack(
                id = p[0].toLong(),
                uri = dec(p[1]),
                title = dec(p[2]),
                artist = dec(p[3]),
                mimeType = dec(p[4])
            )
        }.getOrNull()
    }

    private fun enc(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun dec(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8
    )
}

class MusicAlarmRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun alarms(): List<MusicAlarm> = MusicAlarmCodec.decode(preferences.getString(KEY, null))

    fun find(id: String): MusicAlarm? = alarms().firstOrNull { it.id == id }

    fun upsert(alarm: MusicAlarm): List<MusicAlarm> {
        val current = alarms()
        val updated = if (current.any { it.id == alarm.id }) {
            current.map { if (it.id == alarm.id) alarm else it }
        } else {
            current + alarm
        }
        return save(updated)
    }

    fun delete(id: String): List<MusicAlarm> = save(alarms().filterNot { it.id == id })

    fun setEnabled(id: String, enabled: Boolean): List<MusicAlarm> =
        save(alarms().map { if (it.id == id) it.copy(enabled = enabled) else it })

    private fun save(value: List<MusicAlarm>): List<MusicAlarm> {
        preferences.edit().putString(KEY, MusicAlarmCodec.encode(value)).apply()
        return value
    }

    private companion object {
        const val PREFERENCES_NAME = "music_alarm_preferences"
        const val KEY = "alarms_v1"
    }
}
