package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.example.data.ClockPreferences
import com.example.model.ServiceStatus
import com.example.scheduler.AlarmScheduler
import com.example.service.SpeakingClockService

class ClockAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_CLOCK_ALARM) return

        val prefs = ClockPreferences.getInstance(context)
        val settings = prefs.getSettings()

        // Only announce if currently running (not stopped or paused)
        if (settings.status != ServiceStatus.RUNNING) {
            AlarmScheduler.cancelAlarm(context)
            return
        }

        // Avoid duplicate triggers within the same 45 seconds
        val now = System.currentTimeMillis()
        if (Math.abs(now - settings.lastAnnouncedMillis) < 45_000L) {
            return
        }

        // Acquire a safe, temporary partial wake lock so CPU doesn't sleep before TTS starts
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SpeakingClock:AlarmWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(15_000L) // 15 second max failsafe
        }

        val serviceIntent = Intent(context, SpeakingClockService::class.java).apply {
            action = SpeakingClockService.ACTION_ANNOUNCE_TIME
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Service start exception
        }
    }
}
