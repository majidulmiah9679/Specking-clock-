package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SpeakingClockApp
import com.example.data.ClockPreferences
import com.example.model.ClockSettings
import com.example.model.IntervalPreset
import com.example.model.ServiceStatus
import com.example.scheduler.AlarmScheduler
import com.example.tts.NaturalTimeFormatter
import com.example.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SpeakingClockService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var prefs: ClockPreferences
    private lateinit var ttsManager: TtsManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var persistentWakeLock: PowerManager.WakeLock? = null
    private var screenOffWatchdogJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = ClockPreferences.getInstance(this)
        ttsManager = TtsManager(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SpeakingClock:SpeechWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }

        persistentWakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SpeakingClock:PersistentScreenOffWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val currentSettings = prefs.getSettings()

        when (action) {
            ACTION_START -> {
                handleStart(currentSettings)
            }
            ACTION_PAUSE -> {
                handlePause()
            }
            ACTION_RESUME -> {
                handleResume()
            }
            ACTION_STOP -> {
                handleStop()
                return START_NOT_STICKY
            }
            ACTION_ANNOUNCE_TIME -> {
                handleAnnounceTime(currentSettings)
            }
            ACTION_TEST_VOICE -> {
                handleTestVoice(currentSettings)
            }
            ACTION_UPDATE_NOTIFICATION -> {
                updateForegroundNotification(prefs.getSettings())
            }
        }

        return START_STICKY
    }

    private fun handleStart(settings: ClockSettings) {
        prefs.updateStatus(ServiceStatus.RUNNING)
        if (settings.screenOffAlwaysActive) {
            acquirePersistentWakeLock()
        }
        val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings)
        prefs.updateNextAnnouncement(nextTime)
        AlarmScheduler.scheduleNextAlarm(this, nextTime)

        startScreenOffWatchdog()

        val updatedSettings = prefs.getSettings()
        startInForeground(buildNotification(updatedSettings))
    }

    private fun handlePause() {
        prefs.updateStatus(ServiceStatus.PAUSED)
        stopScreenOffWatchdog()
        releasePersistentWakeLock()
        AlarmScheduler.cancelAlarm(this)
        val updatedSettings = prefs.getSettings()
        updateForegroundNotification(updatedSettings)
    }

    private fun handleResume() {
        prefs.updateStatus(ServiceStatus.RUNNING)
        val settings = prefs.getSettings()
        if (settings.screenOffAlwaysActive) {
            acquirePersistentWakeLock()
        }
        val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings)
        prefs.updateNextAnnouncement(nextTime)
        AlarmScheduler.scheduleNextAlarm(this, nextTime)

        startScreenOffWatchdog()

        val updatedSettings = prefs.getSettings()
        updateForegroundNotification(updatedSettings)
    }

    private fun handleStop() {
        prefs.updateStatus(ServiceStatus.STOPPED)
        stopScreenOffWatchdog()
        releasePersistentWakeLock()
        AlarmScheduler.cancelAlarm(this)
        ttsManager.stop()
        stopForegroundCompat()
        stopSelf()
    }

    private fun startScreenOffWatchdog() {
        screenOffWatchdogJob?.cancel()
        screenOffWatchdogJob = serviceScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(5000L)
                val s = prefs.getSettings()
                if (s.status != ServiceStatus.RUNNING) break
                val now = System.currentTimeMillis()
                if (s.nextAnnouncementMillis in 1..now && Math.abs(now - s.lastAnnouncedMillis) > 45_000L) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        handleAnnounceTime(prefs.getSettings())
                    }
                }
            }
        }
    }

    private fun stopScreenOffWatchdog() {
        screenOffWatchdogJob?.cancel()
        screenOffWatchdogJob = null
    }

    private fun handleAnnounceTime(settings: ClockSettings) {
        // Keep CPU awake while speaking
        wakeLock?.acquire(15_000L)

        // Ensure notification is active
        startInForeground(buildNotification(settings))

        val nowCal = Calendar.getInstance()
        val textToSpeak = NaturalTimeFormatter.formatSpokenTime(
            calendar = nowCal,
            language = settings.language,
            is24Hour = settings.is24HourFormat,
            announceAmPm = settings.announceAmPm
        )

        prefs.updateLastAnnounced(nowCal.timeInMillis)

        // Schedule next alarm immediately
        val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings)
        prefs.updateNextAnnouncement(nextTime)
        AlarmScheduler.scheduleNextAlarm(this, nextTime)
        updateForegroundNotification(prefs.getSettings())

        serviceScope.launch {
            ttsManager.speakText(
                text = textToSpeak,
                language = settings.language,
                voiceGender = settings.voiceGender,
                rate = settings.speechRate,
                pitch = settings.speechPitch,
                playChime = settings.playChime,
                vibrate = settings.vibrateOnAnnouncement,
                onComplete = {
                    releaseWakeLockSafely()
                }
            )
        }
    }

    private fun handleTestVoice(settings: ClockSettings) {
        val nowCal = Calendar.getInstance()
        val textToSpeak = NaturalTimeFormatter.formatSpokenTime(
            calendar = nowCal,
            language = settings.language,
            is24Hour = settings.is24HourFormat,
            announceAmPm = settings.announceAmPm
        )

        serviceScope.launch {
            ttsManager.speakText(
                text = textToSpeak,
                language = settings.language,
                voiceGender = settings.voiceGender,
                rate = settings.speechRate,
                pitch = settings.speechPitch,
                playChime = settings.playChime,
                vibrate = settings.vibrateOnAnnouncement
            )
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateForegroundNotification(settings: ClockSettings) {
        val notification = buildNotification(settings)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(settings: ClockSettings): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isRunning = settings.status == ServiceStatus.RUNNING
        val title = if (isRunning) "Speaking Clock is Active" else "Speaking Clock is Paused"

        val intervalText = when (settings.intervalPreset) {
            IntervalPreset.EVERY_15_MIN -> "15 min"
            IntervalPreset.EVERY_30_MIN -> "30 min"
            IntervalPreset.EVERY_1_HOUR -> "1 hour"
            IntervalPreset.CUSTOM -> "${settings.customIntervalMinutes} min"
        }

        val nextTimeStr = if (isRunning && settings.nextAnnouncementMillis > 0) {
            val format = if (settings.is24HourFormat) "HH:mm" else "hh:mm a"
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.format(settings.nextAnnouncementMillis)
        } else {
            "Paused"
        }

        val contentText = if (isRunning) {
            "Interval: $intervalText • Next at $nextTimeStr"
        } else {
            "Clock paused • Tap Resume to continue"
        }

        val builder = NotificationCompat.Builder(this, SpeakingClockApp.CHANNEL_SERVICE_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Action: Pause / Resume
        if (isRunning) {
            val pauseIntent = Intent(this, SpeakingClockService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            val resumeIntent = Intent(this, SpeakingClockService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        }

        // Action: Stop
        val stopIntent = Intent(this, SpeakingClockService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun acquirePersistentWakeLock() {
        try {
            if (persistentWakeLock?.isHeld != true) {
                persistentWakeLock?.acquire()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun releasePersistentWakeLock() {
        try {
            if (persistentWakeLock?.isHeld == true) {
                persistentWakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun releaseWakeLockSafely() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenOffWatchdog()
        releaseWakeLockSafely()
        releasePersistentWakeLock()
        ttsManager.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.example.action.START"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_ANNOUNCE_TIME = "com.example.action.ANNOUNCE_TIME"
        const val ACTION_TEST_VOICE = "com.example.action.TEST_VOICE"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.action.UPDATE_NOTIFICATION"
    }
}
