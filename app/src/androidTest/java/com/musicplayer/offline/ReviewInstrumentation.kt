package com.musicplayer.offline

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.ComponentName
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musicplayer.offline.playback.PlaybackService
import com.musicplayer.offline.playback.EqualizerManager
import com.musicplayer.offline.ui.toMediaItem
import com.musicplayer.offline.alarm.*
import com.musicplayer.offline.conversion.AudioConversionManager
import com.musicplayer.offline.conversion.ConversionState
import com.musicplayer.offline.data.*
import com.musicplayer.offline.music.*
import java.time.ZonedDateTime

/** Device checks packaged only in the separate test APK, never in the player APK. */
class ReviewInstrumentation : Instrumentation() {
    private var mode = "review"
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        mode = arguments?.getString("mode") ?: "review"
        start()
    }

    override fun onStart() {
        val results = Bundle()
        try {
            val ctx = targetContext
            val alarms = MusicAlarmRepository(ctx)
            if (mode == "cleanup") {
                ctx.stopService(Intent(ctx, AlarmPlaybackService::class.java))
                AlarmScheduler.cancel(ctx, TEST_ALARM_ID)
                alarms.delete(TEST_ALARM_ID)
                results.putString("result", "Test alarm removed; user alarms preserved")
            } else {
                val songs = SafMusicRepository(ctx).loadSongs()
                check(songs.isNotEmpty()) { "Import at least one song before device review" }
                check(songs.map { it.id }.distinct().size == songs.size)
                results.putInt("library_tracks", songs.size)

                if (mode == "playback") {
                    lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
                    runOnMainSync {
                        future = MediaController.Builder(ctx, SessionToken(ctx,
                            ComponentName(ctx, PlaybackService::class.java))).buildAsync()
                    }
                    val controller = future.get(20, java.util.concurrent.TimeUnit.SECONDS)
                    val queue = List(5) { songs[it % songs.size].toMediaItem() }
                    runOnMainSync {
                        controller.setMediaItems(queue)
                        controller.prepare()
                        controller.play()
                    }
                    Thread.sleep(2_000)
                    runOnMainSync {
                        check(controller.isPlaying) { "MediaSession playback did not start" }
                        check(controller.currentPosition > 0)
                        controller.pause()
                        controller.seekTo(0, 500)
                        controller.shuffleModeEnabled = true
                        controller.repeatMode = Player.REPEAT_MODE_ALL
                        check(controller.shuffleModeEnabled)
                        check(controller.repeatMode == Player.REPEAT_MODE_ALL)
                        controller.moveMediaItem(4, 1)
                        check(controller.getMediaItemAt(1).mediaId == queue[4].mediaId)
                        controller.removeMediaItem(4)
                        check(controller.mediaItemCount == 4)
                        controller.seekToDefaultPosition(1)
                        controller.play()
                    }
                    Thread.sleep(1_000)
                    runOnMainSync {
                        check(controller.currentMediaItemIndex == 1)
                        val eq = EqualizerManager.state.value
                        if (eq.available) {
                            EqualizerManager.setEnabled(true)
                            EqualizerManager.applyPreset("Rock")
                            check(EqualizerManager.state.value.bandLevels.any { it != 0 })
                            EqualizerManager.setBand(0, 200)
                            check(EqualizerManager.state.value.bandLevels[0] == 200)
                            EqualizerManager.reset()
                            check(EqualizerManager.state.value.bandLevels.all { it == 0 })
                            EqualizerManager.setEnabled(eq.enabled)
                            eq.bandLevels.forEachIndexed(EqualizerManager::setBand)
                            results.putString("equalizer_bands_presets_manual_reset", "PASS")
                        } else results.putString("equalizer", "UNAVAILABLE on this device")
                        controller.pause()
                        com.musicplayer.offline.ui.clearQueueKeepingCurrent(controller)
                        check(controller.mediaItemCount == 1)
                        controller.release()
                    }
                    results.putString("media_session_play_pause_seek_shuffle_repeat_queue_5_move_remove_clear", "PASS")
                    finishReview(Activity.RESULT_OK, results)
                    return
                }

                if (mode == "review") {
                    val settingsRepo = AppSettingsRepository(ctx)
                    val originalSettings = settingsRepo.load()
                    try {
                        settingsRepo.save(originalSettings.copy(accentColor = AccentColor.ELECTRIC_BLUE))
                        check(AppSettingsRepository(ctx).load().accentColor == AccentColor.ELECTRIC_BLUE)
                    } finally { settingsRepo.save(originalSettings) }
                    results.putString("settings_persistence", "PASS")

                    val playlists = PlaylistRepository(ctx)
                    val before = playlists.playlists().map { it.id }.toSet()
                    val id = playlists.create("Revisão automática").first { it.id !in before }.id
                    try {
                        playlists.addSong(id, songs.first().id)
                        playlists.rename(id, "Revisão renomeada")
                        check(PlaylistRepository(ctx).playlists().first { it.id == id }.songIds.contains(songs.first().id))
                        val vm = MusicPlayerViewModel(UserLibraryRepository(ctx), playlists, settingsRepo)
                        runOnMainSync { vm.setSongs(emptyList()) }
                        check(playlists.playlists().first { it.id == id }.songIds.contains(songs.first().id))
                        playlists.removeSong(id, songs.first().id)
                        check(playlists.playlists().first { it.id == id }.songIds.isEmpty())
                    } finally { playlists.delete(id) }
                    results.putString("playlist_crud_and_filtered_persistence", "PASS")

                    songs.firstOrNull(AudioFileSupport::isWav)?.let { wav ->
                        val originalSize = ctx.contentResolver.openInputStream(wav.uri)!!.use { it.readBytes().size }
                        AudioConversionManager.start(ctx, wav, false, 128)
                        val deadline = System.currentTimeMillis() + 60_000
                        while (System.currentTimeMillis() < deadline) {
                            val state = AudioConversionManager.state.value
                            if (state is ConversionState.Success || state is ConversionState.Failure) break
                            Thread.sleep(200)
                        }
                        val state = AudioConversionManager.state.value
                        check(state is ConversionState.Success) { "WAV conversion result: $state" }
                        check(ctx.contentResolver.openInputStream(state.uri)!!.use { it.readBytes().size } > 0)
                        check(ctx.contentResolver.openInputStream(wav.uri)!!.use { it.readBytes().size } == originalSize)
                        results.putString("wav_128kbps_original_preserved", "PASS")
                        results.putString("converted_uri", state.uri.toString())
                        AudioConversionManager.dismiss()
                    }
                }

                val trigger = ZonedDateTime.now().plusMinutes(2).withSecond(0).withNano(0)
                val tracks = if (mode == "playlist") songs.take(3) else listOf(songs.first())
                val alarm = MusicAlarm(
                    id = TEST_ALARM_ID, label = "Teste JUKE revisão", hour = trigger.hour,
                    minute = trigger.minute, days = setOf(trigger.dayOfWeek.value),
                    sourceType = if (mode == "playlist") AlarmSourceType.PLAYLIST else AlarmSourceType.SONG,
                    sourceTitle = if (mode == "playlist") "Playlist de teste" else tracks.first().title,
                    tracks = tracks.map { AlarmTrack(it.id, it.uri.toString(), it.title, it.artist, it.mimeType) },
                    gradualVolume = true, volumePercent = 80, snoozeMinutes = 5, vibrate = true
                )
                alarms.upsert(alarm)
                check(MusicAlarmRepository(ctx).find(TEST_ALARM_ID) == alarm)
                // Instrumentation completion kills its process immediately; flush apply()
                // before testing a cold-process alarm delivery.
                val preferences = ctx.getSharedPreferences("music_alarm_preferences", 0)
                check(preferences.edit().putString("alarms_v1", preferences.getString("alarms_v1", null)).commit())
                check(AlarmScheduler.schedule(ctx, alarm)) { "Grant exact alarm permission before testing" }
                results.putString("scheduled_alarm", trigger.toString())
                results.putInt("alarm_tracks", tracks.size)
            }
            finishReview(Activity.RESULT_OK, results)
        } catch (error: Throwable) {
            results.putString("failure", error.stackTraceToString())
            finishReview(Activity.RESULT_CANCELED, results)
        }
    }

    private fun finishReview(code: Int, results: Bundle) {
        for (name in listOf("music_alarm_preferences", "app_settings", "user_library", "equalizer")) {
            val preferences = targetContext.getSharedPreferences(name, 0)
            val edit = preferences.edit()
            preferences.all.forEach { (key, value) ->
                when (value) {
                    is String -> edit.putString(key, value)
                    is Boolean -> edit.putBoolean(key, value)
                    is Int -> edit.putInt(key, value)
                    is Long -> edit.putLong(key, value)
                }
            }
            edit.commit()
        }
        finish(code, results)
    }

    companion object { const val TEST_ALARM_ID = "juke-device-review-test" }
}
