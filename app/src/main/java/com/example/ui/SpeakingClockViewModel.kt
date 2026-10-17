package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiService
import com.example.data.ClockPreferences
import com.example.model.ClockLanguage
import com.example.model.ClockSettings
import com.example.model.IntervalPreset
import com.example.model.ServiceStatus
import com.example.model.SpeechAvailabilityStatus
import com.example.service.SpeakingClockService
import com.example.tts.NaturalTimeFormatter
import com.example.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class ClockUiState(
    val currentCalendar: Calendar = Calendar.getInstance(),
    val settings: ClockSettings = ClockSettings(),
    val languageAvailability: Map<ClockLanguage, SpeechAvailabilityStatus> = emptyMap(),
    val countdownSeconds: Long = 0L,
    val canScheduleExactAlarms: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val isSpeakingTest: Boolean = false,
    val aiGenerating: Boolean = false,
    val aiStatusMessage: String? = null
)

class SpeakingClockViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ClockPreferences.getInstance(application)
    private val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(ClockUiState(settings = prefs.getSettings()))
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

    init {
        // Collect preferences
        viewModelScope.launch {
            prefs.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                updateCountdown()
            }
        }

        // Collect TTS language status
        viewModelScope.launch {
            ttsManager.availabilityState.collect { availability ->
                _uiState.update { it.copy(languageAvailability = availability) }
            }
        }

        // Ticking clock loop: update time every 500ms
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = Calendar.getInstance()
                val nextMillis = _uiState.value.settings.nextAnnouncementMillis
                val countdown = if (_uiState.value.settings.status == ServiceStatus.RUNNING && nextMillis > now.timeInMillis) {
                    (nextMillis - now.timeInMillis) / 1000L
                } else {
                    0L
                }

                _uiState.update {
                    it.copy(
                        currentCalendar = now,
                        countdownSeconds = countdown
                    )
                }
                delay(500)
            }
        }

        refreshSystemPermissions()
    }

    fun refreshSystemPermissions() {
        val context = getApplication<Application>()

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true

        _uiState.update {
            it.copy(
                canScheduleExactAlarms = canExact,
                isIgnoringBatteryOptimizations = isIgnoringBattery
            )
        }
        ttsManager.checkAllLanguagesAvailability()
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasNotificationPermission = granted) }
    }

    fun startClock() {
        val context = getApplication<Application>()
        val intent = Intent(context, SpeakingClockService::class.java).apply {
            action = SpeakingClockService.ACTION_START
        }
        startServiceCompat(context, intent)
    }

    fun pauseClock() {
        val context = getApplication<Application>()
        val intent = Intent(context, SpeakingClockService::class.java).apply {
            action = SpeakingClockService.ACTION_PAUSE
        }
        startServiceCompat(context, intent)
    }

    fun resumeClock() {
        val context = getApplication<Application>()
        val intent = Intent(context, SpeakingClockService::class.java).apply {
            action = SpeakingClockService.ACTION_RESUME
        }
        startServiceCompat(context, intent)
    }

    fun stopClock() {
        val context = getApplication<Application>()
        val intent = Intent(context, SpeakingClockService::class.java).apply {
            action = SpeakingClockService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun testVoice() {
        val context = getApplication<Application>()
        val currentSettings = _uiState.value.settings
        _uiState.update { it.copy(isSpeakingTest = true) }

        val cal = Calendar.getInstance()
        val text = NaturalTimeFormatter.formatSpokenTime(
            calendar = cal,
            language = currentSettings.language,
            is24Hour = currentSettings.is24HourFormat,
            announceAmPm = currentSettings.announceAmPm
        )

        viewModelScope.launch {
            ttsManager.speakText(
                text = text,
                language = currentSettings.language,
                voiceGender = currentSettings.voiceGender,
                rate = currentSettings.speechRate,
                pitch = currentSettings.speechPitch,
                playChime = currentSettings.playChime,
                vibrate = currentSettings.vibrateOnAnnouncement,
                onComplete = {
                    _uiState.update { it.copy(isSpeakingTest = false) }
                }
            )
        }
    }

    fun updateVoiceGender(gender: com.example.model.VoiceGender) {
        prefs.updateVoiceGender(gender)
        notifyServiceSettingsChanged()
    }

    fun updateScreenOffAlwaysActive(enabled: Boolean) {
        prefs.updateScreenOffAlwaysActive(enabled)
        notifyServiceSettingsChanged()
    }

    fun updateInterval(preset: IntervalPreset, customMinutes: Int = 15) {
        prefs.updateInterval(preset, customMinutes)
        notifyServiceSettingsChanged()
    }

    fun updateLanguage(language: ClockLanguage) {
        prefs.updateLanguage(language)
        ttsManager.checkAllLanguagesAvailability()
        notifyServiceSettingsChanged()
    }

    fun updateSpeechRate(rate: Float) {
        prefs.updateSpeechRate(rate)
        notifyServiceSettingsChanged()
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.updateSpeechPitch(pitch)
        notifyServiceSettingsChanged()
    }

    fun update24HourFormat(enabled: Boolean) {
        prefs.update24HourFormat(enabled)
        notifyServiceSettingsChanged()
    }

    fun updateAnnounceAmPm(enabled: Boolean) {
        prefs.updateAnnounceAmPm(enabled)
        notifyServiceSettingsChanged()
    }

    fun updatePlayChime(enabled: Boolean) {
        prefs.updatePlayChime(enabled)
        notifyServiceSettingsChanged()
    }

    fun updateVibrate(enabled: Boolean) {
        prefs.updateVibrate(enabled)
        notifyServiceSettingsChanged()
    }

    fun updateAutoRestartBoot(enabled: Boolean) {
        prefs.updateAutoRestartBoot(enabled)
    }

    fun generateGeminiAiSpeechTest() {
        val context = getApplication<Application>()
        val currentSettings = _uiState.value.settings
        val text = NaturalTimeFormatter.formatSpokenTime(
            calendar = Calendar.getInstance(),
            language = currentSettings.language,
            is24Hour = currentSettings.is24HourFormat,
            announceAmPm = currentSettings.announceAmPm
        )

        _uiState.update { it.copy(aiGenerating = true, aiStatusMessage = "Synthesizing AI Voice with gemini-3.8-flash-tts...") }
        viewModelScope.launch {
            val result = GeminiAiService.generateAiSpeech(text)
            result.onSuccess { audioBytes ->
                _uiState.update { it.copy(aiGenerating = false, aiStatusMessage = "AI Audio generated successfully!") }
                GeminiAiService.playAudio(context, audioBytes)
            }.onFailure { error ->
                _uiState.update { it.copy(aiGenerating = false, aiStatusMessage = "AI Speech: ${error.localizedMessage ?: "Failed"}") }
            }
        }
    }

    fun generateLyriaChimeTest() {
        val context = getApplication<Application>()
        _uiState.update { it.copy(aiGenerating = true, aiStatusMessage = "Composing chime with lyria-3-clip-preview...") }
        viewModelScope.launch {
            val result = GeminiAiService.generateLyriaChime("Soft musical crystal clock bell chime")
            result.onSuccess { audioBytes ->
                _uiState.update { it.copy(aiGenerating = false, aiStatusMessage = "Lyria music chime generated!") }
                GeminiAiService.playAudio(context, audioBytes)
            }.onFailure { error ->
                _uiState.update { it.copy(aiGenerating = false, aiStatusMessage = "Lyria Chime: ${error.localizedMessage ?: "Failed"}") }
            }
        }
    }

    fun clearAiMessage() {
        _uiState.update { it.copy(aiStatusMessage = null) }
    }

    private fun notifyServiceSettingsChanged() {
        val context = getApplication<Application>()
        if (_uiState.value.settings.status == ServiceStatus.RUNNING) {
            val intent = Intent(context, SpeakingClockService::class.java).apply {
                action = SpeakingClockService.ACTION_START
            }
            startServiceCompat(context, intent)
        }
    }

    private fun updateCountdown() {
        val now = System.currentTimeMillis()
        val next = _uiState.value.settings.nextAnnouncementMillis
        val diffSec = if (_uiState.value.settings.status == ServiceStatus.RUNNING && next > now) {
            (next - now) / 1000L
        } else {
            0L
        }
        _uiState.update { it.copy(countdownSeconds = diffSec) }
    }

    private fun startServiceCompat(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // Background start error handling
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
