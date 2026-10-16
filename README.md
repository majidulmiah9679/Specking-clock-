# Speaking Clock - Native Android Application

A complete, production-quality native Android application built with Kotlin and Jetpack Compose that speaks the current time aloud at user-selected intervals, engineered to operate seamlessly even when the phone screen is turned off.

---

## Features

1. **Large Digital Clock Display**
   - Live real-time ticking clock with hours, minutes, blinking colon, and second counter.
   - Formatted full date display.
   - 12-hour (with AM/PM badge) and 24-hour military format.

2. **Multilingual Real-Time Time Speech Engine**
   - **English**: *"The time is now ten fifteen AM."*
   - **Bengali (বাংলা)**: *"এখন সময় সকাল দশটা পনেরো মিনিট।"*
   - **Hindi (हिन्दी)**: *"अभी सुबह दस बजकर पंद्रह मिनट हुए हैं।"*
   - Real linguistic time formatting with periods of day (সকাল, দুপুর, বিকেল, সন্ধ্যা, রাত; सुबह, दोपहर, शाम, रात).
   - Voice Data detection with 1-tap installation intent if voice data is missing.

3. **Screen-Off & Background Execution**
   - **Foreground Service** (`SpeakingClockService`) with persistent status notification.
   - **Hardware AlarmManager**: uses `setAlarmClock()` and `setExactAndAllowWhileIdle()` to reliably wake the CPU from deep Doze at the exact scheduled announcement minute.
   - **WakeLock Management**: temporary partial wake lock (15s timeout) during speech so the device never sleeps mid-sentence.
   - **Boot & Time Receiver** (`BootReceiver`): automatically restores schedules on device reboot, manual time changes, or timezone adjustments.
   - **Duplicate Suppression**: prevents duplicate triggers if alarms fire twice in the same minute.

4. **Flexible Speaking Intervals**
   - Preset: Every 15 minutes (:00, :15, :30, :45)
   - Preset: Every 30 minutes (:00, :30)
   - Preset: Every 1 hour (:00)
   - Custom: 1 minute up to 12 hours (validated to prevent zero or negative intervals).
   - Real-time countdown timer to the next announcement.

5. **Audio Customization**
   - Dual-tone harmonic bell chime (C5 + E5) synthesized via AudioTrack PCM before speech.
   - Speech rate adjustment (0.5x to 2.0x).
   - Speech pitch adjustment (0.5x to 2.0x).
   - Haptic vibration pulse before speaking.
   - AI Studio Voice & Chime Studio: supports `gemini-3.8-flash-tts` and Lyria music chimes (`lyria-3-clip-preview`).

---

## Architecture & Project Structure

```
├── app/
│   ├── build.gradle.kts                # App-level build script (Compose, Secrets, Kotlin 2.2.10)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml     # Permissions, foreground service, receivers, queries
│   │   │   ├── java/com/example/
│   │   │   │   ├── SpeakingClockApp.kt          # Application class, notification channels
│   │   │   │   ├── MainActivity.kt              # Jetpack Compose UI entry point
│   │   │   │   ├── model/
│   │   │   │   │   └── ClockModels.kt           # ServiceStatus, ClockLanguage, IntervalPreset, ClockSettings
│   │   │   │   ├── data/
│   │   │   │   │   └── ClockPreferences.kt      # SharedPreferences storage & StateFlow
│   │   │   │   ├── tts/
│   │   │   │   │   ├── NaturalTimeFormatter.kt  # English, Bengali, Hindi linguistic engines
│   │   │   │   │   ├── TtsManager.kt            # Native TextToSpeech, AudioFocus, Utterance listener
│   │   │   │   │   └── ChimeGenerator.kt        # Dual-tone bell chime PCM audio synthesizer
│   │   │   │   ├── scheduler/
│   │   │   │   │   └── AlarmScheduler.kt        # Exact Alarms, next interval math, cancel logic
│   │   │   │   ├── service/
│   │   │   │   │   └── SpeakingClockService.kt  # Foreground service with persistent notification
│   │   │   │   ├── receiver/
│   │   │   │   │   ├── ClockAlarmReceiver.kt    # WakeLock + alarm trigger handler
│   │   │   │   │   └── BootReceiver.kt          # BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED
│   │   │   │   ├── ai/
│   │   │   │   │   └── GeminiAiService.kt       # gemini-3.8-flash-tts and lyria-3-clip-preview
│   │   │   │   └── ui/
│   │   │   │       ├── SpeakingClockViewModel.kt # StateFlows, permissions, timers, actions
│   │   │   │       ├── components/
│   │   │   │       │   ├── DigitalClockCard.kt
│   │   │   │       │   ├── StatusCountdownCard.kt
│   │   │   │       │   ├── ActionButtonsBar.kt
│   │   │   │       │   ├── IntervalPickerSection.kt
│   │   │   │       │   ├── SettingsDialog.kt
│   │   │   │       │   ├── PermissionBanner.kt
│   │   │   │       │   └── MultiLanguagePreviewCard.kt
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt
│   │   │   │           ├── Theme.kt
│   │   │   │           └── Type.kt
│   │   │   └── res/
│   │   │       ├── drawable/
│   │   │       │   ├── ic_launcher_background.xml # Adaptive icon background (indigo gradient)
│   │   │       │   └── ic_launcher_foreground.xml # Adaptive icon clock & sound waves
│   │   │       └── values/
│   │   │           └── strings.xml
│   └── src/test/java/com/example/
│       └── ExampleRobolectricTest.kt   # Unit tests for scheduling, TTS phrasing, preferences
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Root project name & modules
└── gradle/
    └── libs.versions.toml              # Centralized version catalog
```

---

## Building the APK without a PC (From an Android Phone)

This project is a 100% genuine native Android Gradle project. You can compile it into a production APK directly from an Android phone using any of the following methods:

### Method 1: Google AI Studio / Cloud Build (Recommended)
1. In the AI Studio project dashboard, navigate to **Settings / Export**.
2. Select **Generate APK / AAB**.
3. The cloud build server will execute `./gradlew assembleRelease` and provide a direct download link for the signed APK.

### Method 2: GitHub Actions (Free Automated Cloud Build)
1. Push this repository to GitHub from your phone or export the ZIP.
2. In the GitHub repository, add `.github/workflows/android.yml`:
   ```yaml
   name: Build Android APK
   on: [push, workflow_dispatch]
   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Set up JDK 17
           uses: actions/setup-java@v4
           with:
             distribution: 'temurin'
             java-version: '17'
         - name: Build with Gradle
           run: gradle assembleDebug
         - name: Upload APK
           uses: actions/upload-artifact@v4
           with:
             name: SpeakingClock-Debug-APK
             path: app/build/outputs/apk/debug/*.apk
   ```
3. Whenever triggered, GitHub Actions builds the APK in the cloud in under 2 minutes. You can download and install it directly on your phone.

---

## Manual Testing Checklist

| Test Case | Description | Expected Result |
| :--- | :--- | :--- |
| **TC-01: Start Clock** | Tap "Start Speaking Clock" | Status changes to Active, persistent notification appears with next announcement time. |
| **TC-02: Pause / Resume** | Tap "Pause" then "Resume" | Pauses upcoming alarms; resumes without double-triggering. |
| **TC-03: Stop Clock** | Tap "Stop" | Service terminates, notification disappears, scheduled alarms are cleared. |
| **TC-04: 15-Min Interval** | Select 15 min preset | Next announcement aligns to next :00, :15, :30, or :45. |
| **TC-05: Custom Interval** | Enter 5 min or 2 hrs | Validates > 0 min and <= 720 min, updates countdown accurately. |
| **TC-06: Language Switch** | Change to Bengali or Hindi | Real-time preview updates immediately; speaks in Bengali/Hindi when triggered. |
| **TC-07: Screen-Off Operation** | Start clock, turn screen off, wait for trigger | Device awakens via AlarmClock, plays chime, and announces time aloud. |
| **TC-08: Device Reboot** | Reboot device while running | `BootReceiver` captures `BOOT_COMPLETED`, recalculates alarm, and resumes service. |
| **TC-09: Exact Alarm Denial** | Revoke exact alarm permission | Banner shows "Enable"; service falls back gracefully to inexact alarms. |
| **TC-10: Test Voice** | Tap "Test Voice" button | Immediately speaks current time in selected voice without resetting alarm schedule. |
