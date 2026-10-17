package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.ClockLanguage
import com.example.model.SpeechAvailabilityStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _availabilityState = MutableStateFlow<Map<ClockLanguage, SpeechAvailabilityStatus>>(emptyMap())
    val availabilityState: StateFlow<Map<ClockLanguage, SpeechAvailabilityStatus>> = _availabilityState.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            initDeferred.complete(true)
            checkAllLanguagesAvailability()
        } else {
            isInitialized = false
            initDeferred.complete(false)
        }
    }

    suspend fun awaitInitialization(): Boolean {
        return if (isInitialized) true else initDeferred.await()
    }

    fun getLocaleForLanguage(language: ClockLanguage): Locale {
        return when (language) {
            ClockLanguage.ENGLISH -> Locale.US
            ClockLanguage.BENGALI -> {
                // Try Bangladesh, then India, then generic Bengali
                val tts = textToSpeech
                val localeBD = Locale.forLanguageTag("bn-BD")
                val localeIN = Locale.forLanguageTag("bn-IN")
                if (tts?.isLanguageAvailable(localeBD) in listOf(
                        TextToSpeech.LANG_AVAILABLE,
                        TextToSpeech.LANG_COUNTRY_AVAILABLE,
                        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                    )
                ) {
                    localeBD
                } else if (tts?.isLanguageAvailable(localeIN) in listOf(
                        TextToSpeech.LANG_AVAILABLE,
                        TextToSpeech.LANG_COUNTRY_AVAILABLE,
                        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                    )
                ) {
                    localeIN
                } else {
                    Locale.forLanguageTag("bn")
                }
            }
            ClockLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
        }
    }

    fun checkLanguageAvailability(language: ClockLanguage): SpeechAvailabilityStatus {
        val tts = textToSpeech ?: return SpeechAvailabilityStatus.CHECKING
        if (!isInitialized) return SpeechAvailabilityStatus.CHECKING

        val locale = getLocaleForLanguage(language)
        val result = tts.isLanguageAvailable(locale)

        return when (result) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> SpeechAvailabilityStatus.AVAILABLE
            TextToSpeech.LANG_MISSING_DATA -> SpeechAvailabilityStatus.MISSING_DATA
            TextToSpeech.LANG_NOT_SUPPORTED -> SpeechAvailabilityStatus.NOT_SUPPORTED
            else -> SpeechAvailabilityStatus.NOT_SUPPORTED
        }
    }

    fun checkAllLanguagesAvailability() {
        val updatedMap = mutableMapOf<ClockLanguage, SpeechAvailabilityStatus>()
        for (lang in ClockLanguage.values()) {
            updatedMap[lang] = checkLanguageAvailability(lang)
        }
        _availabilityState.value = updatedMap
    }

    suspend fun speakText(
        text: String,
        language: ClockLanguage,
        voiceGender: com.example.model.VoiceGender = com.example.model.VoiceGender.FEMALE,
        rate: Float = 1.0f,
        pitch: Float = 1.0f,
        playChime: Boolean = true,
        vibrate: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        if (!awaitInitialization()) {
            onComplete?.invoke()
            return
        }

        if (vibrate) {
            vibrateBriefly()
        }

        if (playChime) {
            ChimeGenerator.playChime()
        }

        val tts = textToSpeech ?: run {
            onComplete?.invoke()
            return
        }

        val locale = getLocaleForLanguage(language)
        tts.language = locale
        tts.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        applyVoice(tts, locale, voiceGender, pitch)

        requestAudioFocus()

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    abandonAudioFocus()
                    onComplete?.invoke()
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    abandonAudioFocus()
                    onComplete?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    abandonAudioFocus()
                    onComplete?.invoke()
                }
            }
        })

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
        abandonAudioFocus()
    }

    fun shutdown() {
        try {
            stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
        } catch (e: Exception) {
            // Ignore on destroy
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* Focus change listener */ }
                    .build()
                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            // Focus request best-effort
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            // Best effort
        }
    }

    private fun vibrateBriefly() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (e: Exception) {
            // Ignore permission or hardware issues
        }
    }

    private fun applyVoice(
        tts: TextToSpeech,
        locale: Locale,
        gender: com.example.model.VoiceGender,
        basePitch: Float
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val availableVoices = tts.voices
                if (!availableVoices.isNullOrEmpty()) {
                    val localeVoices = availableVoices.filter {
                        it.locale.language.equals(locale.language, ignoreCase = true)
                    }
                    if (localeVoices.isNotEmpty()) {
                        val matchedVoice = when (gender) {
                            com.example.model.VoiceGender.FEMALE -> {
                                localeVoices.firstOrNull { v ->
                                    val n = v.name.lowercase()
                                    n.contains("female") || n.contains("-f-") || n.contains("fem") ||
                                    n.contains("bnc") || n.contains("cfc") || n.contains("sfg") ||
                                    (v.features != null && v.features.any { f -> f.lowercase().contains("female") })
                                } ?: localeVoices.firstOrNull { !it.name.lowercase().contains("male") }
                            }
                            com.example.model.VoiceGender.MALE -> {
                                localeVoices.firstOrNull { v ->
                                    val n = v.name.lowercase()
                                    n.contains("male") || n.contains("-m-") ||
                                    (v.features != null && v.features.any { f -> f.lowercase().contains("male") })
                                }
                            }
                            com.example.model.VoiceGender.DEFAULT -> null
                        }
                        if (matchedVoice != null) {
                            tts.voice = matchedVoice
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Best effort voice pack selection
        }

        // Natural formant pitch adjustment for feminine sweetness and clarity
        val adjustedPitch = when (gender) {
            com.example.model.VoiceGender.FEMALE -> (basePitch * 1.25f).coerceIn(0.5f, 2.0f)
            com.example.model.VoiceGender.MALE -> (basePitch * 0.88f).coerceIn(0.5f, 2.0f)
            com.example.model.VoiceGender.DEFAULT -> basePitch.coerceIn(0.5f, 2.0f)
        }
        tts.setPitch(adjustedPitch)
    }
}
