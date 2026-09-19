package com.musicplayer.offline.alarm

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AlarmPlaybackService : Service() {
    private var player: ExoPlayer? = null
    private var currentAlarmId: String? = null
    private var currentSnoozeMinutes: Int = 10
    private val handler = Handler(Looper.getMainLooper())
    private var volumeRamp: Runnable? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> intent.getStringExtra(EXTRA_ALARM_ID)?.let { startAlarm(it) }
            ACTION_SNOOZE -> snoozeCurrentAlarm()
            ACTION_STOP -> stopAlarm()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        currentAlarmId = null
        releasePlayer()
        stopVibration()
        super.onDestroy()
    }

    private fun startAlarm(alarmId: String, focusAttempt: Int = 0) {
        val alarm = MusicAlarmRepository(this).find(alarmId)
        if (alarm == null || alarm.tracks.isEmpty()) {
            stopSelf()
            return
        }

        currentAlarmId = alarmId
        currentSnoozeMinutes = alarm.snoozeMinutes
        startForeground(NOTIFICATION_ID, buildNotification(alarm))
        releasePlayer()
        stopVibration()
        val audioManager = getSystemService(AudioManager::class.java)
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                PlatformAudioAttributes.Builder()
                    .setUsage(PlatformAudioAttributes.USAGE_ALARM)
                    .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener({ change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> stopAlarm()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
                    AudioManager.AUDIOFOCUS_GAIN -> player?.play()
                }
            }, handler)
            .build()
        audioFocusRequest = focusRequest
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Retry briefly after foreground promotion; never play without granted focus.
            if (focusAttempt < 10) {
                handler.postDelayed({
                    if (currentAlarmId == alarmId) startAlarm(alarmId, focusAttempt + 1)
                }, 200)
            } else stopAlarm()
            return
        }

        val activePlayer = player ?: ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                // Media3 automatic focus does not support USAGE_ALARM.
                false
            )
            .build()
            .also { exo ->
                exo.setWakeMode(C.WAKE_MODE_LOCAL)
                exo.setHandleAudioBecomingNoisy(false)
                player = exo
            }

        val mediaItems = alarm.tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(Uri.parse(track.uri))
                .setMimeType(track.mimeType.ifBlank { null })
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .build()
                )
                .build()
        }

        activePlayer.setMediaItems(mediaItems)
        activePlayer.repeatMode =
            if (mediaItems.size > 1) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
        activePlayer.prepare()

        val targetVolume = alarm.volumePercent.coerceIn(1, 100) / 100f
        if (alarm.gradualVolume) startVolumeRamp(activePlayer, targetVolume)
        else activePlayer.volume = targetVolume

        activePlayer.play()
        if (alarm.vibrate) startVibration()
    }

    private fun startVolumeRamp(activePlayer: ExoPlayer, targetVolume: Float) {
        volumeRamp?.let(handler::removeCallbacks)
        activePlayer.volume = alarmRampVolume(targetVolume, 0)
        var step = 0
        val totalSteps = 15
        val runnable = object : Runnable {
            override fun run() {
                step++
                activePlayer.volume = alarmRampVolume(targetVolume, step, totalSteps)
                if (step < totalSteps) handler.postDelayed(this, 2_000)
            }
        }
        volumeRamp = runnable
        handler.postDelayed(runnable, 1_000)
    }

    private fun snoozeCurrentAlarm() {
        currentAlarmId?.let {
            AlarmScheduler.scheduleSnooze(this, it, currentSnoozeMinutes)
        }
        stopAlarm()
    }

    private fun stopAlarm() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        releasePlayer()
        stopVibration()
        currentAlarmId = null
        stopSelf()
    }

    private fun releasePlayer() {
        volumeRamp?.let(handler::removeCallbacks)
        volumeRamp = null
        player?.release()
        player = null
        audioFocusRequest?.let {
            getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 600), 0)
        vibrator?.vibrate(
            effect,
            PlatformAudioAttributes.Builder()
                .setUsage(PlatformAudioAttributes.USAGE_ALARM)
                .build()
        )
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun buildNotification(alarm: MusicAlarm): android.app.Notification {
        val ringingActivityIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val backgroundLaunchOptions = if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }.toBundle()
        } else {
            null
        }
        val ringingIntent = PendingIntent.getActivity(
            this,
            alarm.id.hashCode(),
            ringingActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            backgroundLaunchOptions
        )

        val snoozeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmPlaybackService::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AlarmPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.label)
            .setContentText(alarm.sourceTitle)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(ringingIntent)
            .setFullScreenIntent(ringingIntent, true)
            .addAction(0, "Soneca " + alarm.snoozeMinutes + " min", snoozeIntent)
            .addAction(0, "Parar", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Despertador musical",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarmes musicais do Juke Mp3 Player"
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.musicplayer.offline.alarm.START"
        const val ACTION_SNOOZE = "com.musicplayer.offline.alarm.SNOOZE"
        const val ACTION_STOP = "com.musicplayer.offline.alarm.STOP"
        const val EXTRA_ALARM_ID = "alarm_id"

        private const val CHANNEL_ID = "juke_music_alarm"
        private const val NOTIFICATION_ID = 7401
    }
}
