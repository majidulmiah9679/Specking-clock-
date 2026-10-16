package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ClockLanguage
import com.example.model.ClockSettings
import com.example.model.IntervalPreset
import com.example.model.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ClockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<ClockSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): ClockSettings = loadSettings()

    private fun loadSettings(): ClockSettings {
        val statusName = prefs.getString(KEY_STATUS, ServiceStatus.STOPPED.name) ?: ServiceStatus.STOPPED.name
        val status = try {
            ServiceStatus.valueOf(statusName)
        } catch (e: Exception) {
            ServiceStatus.STOPPED
        }

        val presetName = prefs.getString(KEY_INTERVAL_PRESET, IntervalPreset.EVERY_15_MIN.name)
            ?: IntervalPreset.EVERY_15_MIN.name
        val preset = try {
            IntervalPreset.valueOf(presetName)
        } catch (e: Exception) {
            IntervalPreset.EVERY_15_MIN
        }

        val customMinutes = prefs.getInt(KEY_CUSTOM_MINUTES, 15).coerceAtLeast(1)
        val langCode = prefs.getString(KEY_LANGUAGE, ClockLanguage.ENGLISH.code) ?: ClockLanguage.ENGLISH.code
        val language = ClockLanguage.fromCode(langCode)
        val speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        val speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        val is24h = prefs.getBoolean(KEY_IS_24H, false)
        val announceAmPm = prefs.getBoolean(KEY_ANNOUNCE_AMPM, true)
        val playChime = prefs.getBoolean(KEY_PLAY_CHIME, true)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val autoRestart = prefs.getBoolean(KEY_AUTO_RESTART_BOOT, true)
        val nextMillis = prefs.getLong(KEY_NEXT_ANNOUNCEMENT, 0L)
        val lastMillis = prefs.getLong(KEY_LAST_ANNOUNCED, 0L)

        return ClockSettings(
            status = status,
            intervalPreset = preset,
            customIntervalMinutes = customMinutes,
            language = language,
            speechRate = speechRate,
            speechPitch = speechPitch,
            is24HourFormat = is24h,
            announceAmPm = announceAmPm,
            playChime = playChime,
            vibrateOnAnnouncement = vibrate,
            autoRestartOnBoot = autoRestart,
            nextAnnouncementMillis = nextMillis,
            lastAnnouncedMillis = lastMillis
        )
    }

    fun updateStatus(status: ServiceStatus) {
        prefs.edit().putString(KEY_STATUS, status.name).apply()
        refresh()
    }

    fun updateInterval(preset: IntervalPreset, customMinutes: Int = 15) {
        val validCustom = customMinutes.coerceIn(1, 720)
        prefs.edit()
            .putString(KEY_INTERVAL_PRESET, preset.name)
            .putInt(KEY_CUSTOM_MINUTES, validCustom)
            .apply()
        refresh()
    }

    fun updateLanguage(language: ClockLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        refresh()
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate.coerceIn(0.5f, 2.0f)).apply()
        refresh()
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch.coerceIn(0.5f, 2.0f)).apply()
        refresh()
    }

    fun update24HourFormat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_24H, enabled).apply()
        refresh()
    }

    fun updateAnnounceAmPm(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANNOUNCE_AMPM, enabled).apply()
        refresh()
    }

    fun updatePlayChime(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PLAY_CHIME, enabled).apply()
        refresh()
    }

    fun updateVibrate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        refresh()
    }

    fun updateAutoRestartBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESTART_BOOT, enabled).apply()
        refresh()
    }

    fun updateNextAnnouncement(timeMillis: Long) {
        prefs.edit().putLong(KEY_NEXT_ANNOUNCEMENT, timeMillis).apply()
        refresh()
    }

    fun updateLastAnnounced(timeMillis: Long) {
        prefs.edit().putLong(KEY_LAST_ANNOUNCED, timeMillis).apply()
        refresh()
    }

    private fun refresh() {
        _settingsFlow.value = loadSettings()
    }

    companion object {
        private const val PREFS_NAME = "speaking_clock_prefs"
        private const val KEY_STATUS = "key_service_status"
        private const val KEY_INTERVAL_PRESET = "key_interval_preset"
        private const val KEY_CUSTOM_MINUTES = "key_custom_minutes"
        private const val KEY_LANGUAGE = "key_clock_language"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_IS_24H = "key_is_24h"
        private const val KEY_ANNOUNCE_AMPM = "key_announce_ampm"
        private const val KEY_PLAY_CHIME = "key_play_chime"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_AUTO_RESTART_BOOT = "key_auto_restart_boot"
        private const val KEY_NEXT_ANNOUNCEMENT = "key_next_announcement"
        private const val KEY_LAST_ANNOUNCED = "key_last_announced"

        @Volatile
        private var instance: ClockPreferences? = null

        fun getInstance(context: Context): ClockPreferences {
            return instance ?: synchronized(this) {
                instance ?: ClockPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
