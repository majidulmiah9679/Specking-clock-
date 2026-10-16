package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class SpeakingClockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Service Channel - Low importance so persistent notification doesn't chirp
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_channel_desc)
                setShowBadge(false)
            }

            // Time Announcement Channel - Default importance for announcement popups/heads-up if needed
            val announcementChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENT_ID,
                getString(R.string.announcement_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.announcement_notification_channel_desc)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(announcementChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "speaking_clock_service_channel"
        const val CHANNEL_ANNOUNCEMENT_ID = "speaking_clock_announcement_channel"
    }
}
