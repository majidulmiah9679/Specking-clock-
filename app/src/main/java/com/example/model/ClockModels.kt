package com.example.model

enum class ServiceStatus {
    STOPPED,
    RUNNING,
    PAUSED
}

enum class IntervalPreset(val label: String, val minutes: Int) {
    EVERY_15_MIN("Every 15 min", 15),
    EVERY_30_MIN("Every 30 min", 30),
    EVERY_1_HOUR("Every 1 hour", 60),
    CUSTOM("Custom", 0);

    companion object {
        fun fromMinutes(minutes: Int): IntervalPreset {
            return when (minutes) {
                15 -> EVERY_15_MIN
                30 -> EVERY_30_MIN
                60 -> EVERY_1_HOUR
                else -> CUSTOM
            }
        }
    }
}

enum class ClockLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val sampleAnnouncement: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English (US)",
        sampleAnnouncement = "The time is now ten fifteen AM."
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা (Bengali)",
        sampleAnnouncement = "এখন সময় সকাল দশটা পনেরো মিনিট।"
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी (Hindi)",
        sampleAnnouncement = "अभी सुबह दस बजकर पंद्रह मिनट हुए हैं।"
    );

    companion object {
        fun fromCode(code: String): ClockLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

enum class SpeechAvailabilityStatus {
    CHECKING,
    AVAILABLE,
    MISSING_DATA,
    NOT_SUPPORTED
}

data class ClockSettings(
    val status: ServiceStatus = ServiceStatus.STOPPED,
    val intervalPreset: IntervalPreset = IntervalPreset.EVERY_15_MIN,
    val customIntervalMinutes: Int = 15,
    val language: ClockLanguage = ClockLanguage.ENGLISH,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val is24HourFormat: Boolean = false,
    val announceAmPm: Boolean = true,
    val playChime: Boolean = true,
    val vibrateOnAnnouncement: Boolean = true,
    val autoRestartOnBoot: Boolean = true,
    val nextAnnouncementMillis: Long = 0L,
    val lastAnnouncedMillis: Long = 0L
) {
    val effectiveIntervalMinutes: Int
        get() = if (intervalPreset == IntervalPreset.CUSTOM) {
            customIntervalMinutes.coerceAtLeast(1)
        } else {
            intervalPreset.minutes
        }
}
