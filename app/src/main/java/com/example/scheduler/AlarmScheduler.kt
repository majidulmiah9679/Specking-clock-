package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.model.ClockSettings
import com.example.model.IntervalPreset
import com.example.receiver.ClockAlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_CLOCK_ALARM = "com.example.action.CLOCK_ALARM"
    private const val REQUEST_CODE_ALARM = 1001
    private const val REQUEST_CODE_MAIN_ACTIVITY = 1002

    fun calculateNextAnnouncementTime(settings: ClockSettings, fromTimeMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromTimeMillis }
        val next = Calendar.getInstance().apply {
            timeInMillis = fromTimeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentMinute = now.get(Calendar.MINUTE)

        when (settings.intervalPreset) {
            IntervalPreset.EVERY_15_MIN -> {
                val nextMinute = when {
                    currentMinute < 15 -> 15
                    currentMinute < 30 -> 30
                    currentMinute < 45 -> 45
                    else -> {
                        next.add(Calendar.HOUR_OF_DAY, 1)
                        0
                    }
                }
                next.set(Calendar.MINUTE, nextMinute)
            }
            IntervalPreset.EVERY_30_MIN -> {
                val nextMinute = when {
                    currentMinute < 30 -> 30
                    else -> {
                        next.add(Calendar.HOUR_OF_DAY, 1)
                        0
                    }
                }
                next.set(Calendar.MINUTE, nextMinute)
            }
            IntervalPreset.EVERY_1_HOUR -> {
                next.add(Calendar.HOUR_OF_DAY, 1)
                next.set(Calendar.MINUTE, 0)
            }
            IntervalPreset.CUSTOM -> {
                val minutes = settings.customIntervalMinutes.coerceIn(1, 720)
                if (minutes < 60 && 60 % minutes == 0) {
                    // Clean divisor of an hour: align to marks
                    var targetMinute = -1
                    for (m in minutes until 60 step minutes) {
                        if (m > currentMinute) {
                            targetMinute = m
                            break
                        }
                    }
                    if (targetMinute != -1) {
                        next.set(Calendar.MINUTE, targetMinute)
                    } else {
                        next.add(Calendar.HOUR_OF_DAY, 1)
                        next.set(Calendar.MINUTE, 0)
                    }
                } else {
                    // Non-standard interval: add minutes from now
                    next.timeInMillis = fromTimeMillis + (minutes * 60 * 1000L)
                    next.set(Calendar.SECOND, 0)
                    next.set(Calendar.MILLISECOND, 0)
                }
            }
        }

        // Safety guarantee: next scheduled time must be at least 5 seconds in the future
        if (next.timeInMillis <= fromTimeMillis + 5000L) {
            next.add(Calendar.MINUTE, 1)
        }

        return next.timeInMillis
    }

    fun scheduleNextAlarm(context: Context, nextMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ClockAlarmReceiver::class.java).apply {
            action = ACTION_CLOCK_ALARM
            putExtra("scheduled_time", nextMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val showIntent = Intent(context, MainActivity::class.java)
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        REQUEST_CODE_MAIN_ACTIVITY,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val clockInfo = AlarmManager.AlarmClockInfo(nextMillis, showPendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val showIntent = Intent(context, MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_MAIN_ACTIVITY,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val clockInfo = AlarmManager.AlarmClockInfo(nextMillis, showPendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // In case exact alarm permission was revoked in runtime
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextMillis, pendingIntent)
            } catch (ignored: Exception) {
            }
        } catch (e: Exception) {
            // Other failures
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ClockAlarmReceiver::class.java).apply {
            action = ACTION_CLOCK_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
