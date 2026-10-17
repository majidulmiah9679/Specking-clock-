# Speaking Clock - Native Android Application (কথা বলা ঘড়ি)

A complete, production-quality native Android application built with Kotlin and Jetpack Compose that speaks the current time aloud at user-selected intervals, engineered to operate seamlessly even when the phone screen is turned off.

---

## 🌟 Key Highlights & Features (প্রধান সুবিধাসমূহ)

1. **👩 Female Voice Support (মহিলা কণ্ঠস্বর)**
   - Pre-configured natural sweet feminine voice for Bengali, Hindi, and English.
   - 1-tap quick toggle between **Female Voice (মহিলা কণ্ঠ)** and **Male Voice (পুরুষ কণ্ঠ)** directly on the home screen.
   - Configurable speech rate (0.5x – 2.0x) and pitch adjustments.

2. **🔋 100% Screen-Off Always Active (স্ক্রিন বন্ধ থাকলেও সময় বলবে)**
   - **Foreground Service** with persistent low-priority ongoing notification.
   - **Dual-Layer Background Protection**:
     - Android Hardware `AlarmManager` with `setAlarmClock()` to wake device CPU from Doze mode.
     - Foreground Service Watchdog Coroutine ticker to guarantee announcements even if OEM aggressive battery savers throttle AlarmManager.
   - **Wake Lock Protection**: CPU is kept awake safely during speech to prevent cuts or interruptions.
   - **Device Reboot Support**: Automatically restores and recalculates schedules on phone restart.

3. **🗣️ Multilingual Real-Time Time Speech Engine**
   - **Bengali (বাংলা)**: *"এখন সময় সকাল দশটা পনেরো মিনিট।"*
   - **Hindi (हिन्दी)**: *"अभी सुबह दस बजकर पंद्रহ मिनट हुए हैं।"*
   - **English**: *"The time is now ten fifteen AM."*
   - Dynamic periods of day (সকাল, দুপুর, বিকেল, সন্ধ্যা, রাত; सुबह, दोपहर, शाम, रात).
   - Voice Data detection with 1-tap installation intent if voice data is missing.

4. **⏰ Flexible Speaking Intervals**
   - Every 15 minutes (:00, :15, :30, :45)
   - Every 30 minutes (:00, :30)
   - Every 1 hour (:00)
   - Custom: 1 minute up to 12 hours (with zero/negative validation).
   - Live countdown display to the next upcoming announcement.

5. **🔔 Audio & Haptics**
   - Dual-tone harmonic bell chime (C5 + E5) synthesized natively via AudioTrack PCM before speech.
   - Haptic vibration pulse before announcement.
   - Cloud AI Voice & Chime Studio: supports `gemini-3.8-flash-tts` and Lyria music chimes (`lyria-3-clip-preview`).

---

## 📥 GitHub থেকে সরাসরি APK ডাউনলোড করার নিয়ম (How to Download APK)

### পদ্ধতি ১: Releases থেকে ১-ক্লিকে সরাসরি APK ডাউনলোড (সবচেয়ে সহজ)
1. আপনার GitHub রিপোজিটরির মূল পাতায় যান।
2. পাতার ডানপাশে বা নিচে **"Releases"** অপশনে ট্যাপ করুন।
3. সর্বশেষ রিলিজের নিচে অ্যাটাচ করা **`SpeakingClock.apk`** ফাইলে ট্যাপ করুন।
4. কোনো জিপ (.zip) ফাইল আনজিপ করতে হবে না — সরাসরি খাঁটি APK আপনার ফোনে ডাউনলোড হয়ে যাবে এবং ট্যাপ করলেই ইনস্টল হবে!

### পদ্ধতি ২: Actions ট্যাব থেকে ডাউনলোড
1. GitHub-এর **"Actions"** ট্যাবে যান।
2. সর্বশেষ সম্পন্ন হওয়া বিল্ড (`Build Native Android APK`) এ ট্যাপ করুন।
3. পৃষ্ঠার একদম নিচে **"Artifacts"** সেকশনে যান।
4. **`SpeakingClock-Android-APK`** এ ক্লিক করুন।
   *(মোবাইল ব্রাউজারে এটি জিপ হিসেবে ডাউনলোড হলে যেকোনো ফাইল ম্যানেজার দিয়ে এক্সট্রাক্ট করলেই ভেতরের `SpeakingClock.apk` পেয়ে যাবেন)*।

---

## 📱 ফোন স্ক্রিন অফ থাকলেও সারাক্ষণ চালু রাখার সেটিংস

Xiaomi (MIUI/HyperOS), Samsung, Vivo, Oppo, Realme ইত্যাদি ফোনে ব্যাকগ্রাউন্ড অ্যাপ বন্ধ করার অতিরিক্ত ফিচার থাকে। অ্যাপটি সারাক্ষণ চালু রাখতে:
1. ফোনের **Settings > Apps > Speaking Clock** এ যান।
2. **Battery / Battery Saver** এ গিয়ে **"No Restrictions" (সীমাহীন)** সিলেক্ট করুন।
3. **Notification** পারমিশন চালু রাখুন।
4. অ্যাপের মূল স্ক্রিনে **"Screen-Off Active (স্ক্রিন অফ মোড)"** অন রাখুন।

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
│   │   │   │   │   └── ClockModels.kt           # ServiceStatus, ClockLanguage, IntervalPreset, ClockSettings, VoiceGender
│   │   │   │   ├── data/
│   │   │   │   │   └── ClockPreferences.kt      # SharedPreferences storage & StateFlow
│   │   │   │   ├── tts/
│   │   │   │   │   ├── NaturalTimeFormatter.kt  # English, Bengali, Hindi linguistic engines
│   │   │   │   │   ├── TtsManager.kt            # Native TextToSpeech, Female voice filters, AudioFocus
│   │   │   │   │   └── ChimeGenerator.kt        # Dual-tone bell chime PCM audio synthesizer
│   │   │   │   ├── scheduler/
│   │   │   │   │   └── AlarmScheduler.kt        # Exact Alarms, next interval math, cancel logic
│   │   │   │   ├── service/
│   │   │   │   │   └── SpeakingClockService.kt  # Foreground service with persistent notification & screen-off watchdog
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
│   │   │   │       │   ├── VoiceAndScreenOffCard.kt  # Direct Female voice selector & Screen-Off toggle
│   │   │   │       │   ├── DownloadGuideDialog.kt   # Step-by-step GitHub APK download guide
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
