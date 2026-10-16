package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.ClockPreferences
import com.example.model.ServiceStatus
import com.example.scheduler.AlarmScheduler
import com.example.service.SpeakingClockService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prefs = ClockPreferences.getInstance(context)
        val settings = prefs.getSettings()

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (settings.autoRestartOnBoot && settings.status == ServiceStatus.RUNNING) {
                    val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings)
                    prefs.updateNextAnnouncement(nextTime)
                    AlarmScheduler.scheduleNextAlarm(context, nextTime)

                    val serviceIntent = Intent(context, SpeakingClockService::class.java).apply {
                        this.action = SpeakingClockService.ACTION_START
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        // Background execution restriction handling
                    }
                } else if (settings.status != ServiceStatus.RUNNING) {
                    AlarmScheduler.cancelAlarm(context)
                }
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // If clock is running, recalculate alarm for newly adjusted system time
                if (settings.status == ServiceStatus.RUNNING) {
                    val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings)
                    prefs.updateNextAnnouncement(nextTime)
                    AlarmScheduler.scheduleNextAlarm(context, nextTime)

                    // Also update foreground service notification text
                    val serviceIntent = Intent(context, SpeakingClockService::class.java).apply {
                        this.action = SpeakingClockService.ACTION_UPDATE_NOTIFICATION
                    }
                    try {
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                        // Best effort
                    }
                }
            }
        }
    }
}
