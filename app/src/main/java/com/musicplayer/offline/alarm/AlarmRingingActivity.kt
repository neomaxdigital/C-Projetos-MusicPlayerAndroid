package com.musicplayer.offline.alarm

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class AlarmRingingActivity : Activity() {
    private var alarmId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAlarmWindow()
        showAlarm(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showAlarm(intent)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // The ringing screen should only be dismissed by an explicit alarm action.
    }

    private fun configureAlarmWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun showAlarm(intent: Intent) {
        val id = intent.getStringExtra(AlarmPlaybackService.EXTRA_ALARM_ID)
        val alarm = id?.let { MusicAlarmRepository(this).find(it) }
        if (alarm == null) {
            finish()
            return
        }

        alarmId = alarm.id
        title = "Juke Mp3 Player"

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(60), dp(28), dp(36))
            setBackgroundColor(Color.parseColor("#001C27"))
        }

        val appName = TextView(this).apply {
            text = "JUKE"
            textSize = 24f
            setTextColor(Color.parseColor("#00C2FB"))
            gravity = Gravity.CENTER
        }
        root.addView(
            appName,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val heading = TextView(this).apply {
            text = alarm.label.ifBlank { "Despertador" }
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(44), 0, dp(8))
        }
        root.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val time = TextView(this).apply {
            text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
            textSize = 68f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(
            time,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val source = TextView(this).apply {
            text = alarm.sourceTitle
            textSize = 18f
            setTextColor(Color.parseColor("#AFC1CA"))
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(42))
        }
        root.addView(
            source,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val spacer = android.view.Space(this)
        root.addView(
            spacer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val snoozeButton = Button(this).apply {
            text = "Soneca ${alarm.snoozeMinutes} min"
            textSize = 18f
            isAllCaps = false
            setTextColor(Color.parseColor("#001C27"))
            setBackgroundColor(Color.parseColor("#00C2FB"))
            setOnClickListener { performAlarmAction(AlarmPlaybackService.ACTION_SNOOZE) }
        }
        root.addView(
            snoozeButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                bottomMargin = dp(14)
            }
        )

        val stopButton = Button(this).apply {
            text = "Parar"
            textSize = 18f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#163544"))
            setOnClickListener { performAlarmAction(AlarmPlaybackService.ACTION_STOP) }
        }
        root.addView(
            stopButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        setContentView(root)
    }

    private fun performAlarmAction(action: String) {
        startService(
            Intent(this, AlarmPlaybackService::class.java).apply {
                this.action = action
                alarmId?.let { putExtra(AlarmPlaybackService.EXTRA_ALARM_ID, it) }
            }
        )
        finishAndRemoveTask()
    }
}
