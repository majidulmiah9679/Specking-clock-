package com.example.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

object ChimeGenerator {

    /**
     * Plays a pleasant two-tone clock bell chime (C5 + E5 harmonic ding-dong)
     * using AudioTrack PCM sine synthesis.
     */
    suspend fun playChime() = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 44100
            val durationSec = 0.55 // Total duration
            val totalSamples = (sampleRate * durationSec).toInt()
            val pcmData = ShortArray(totalSamples)

            val freq1 = 587.33 // D5 note
            val freq2 = 880.00 // A5 harmonic bell note

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                // Exponential decay envelope
                val envelope = kotlin.math.exp(-4.2 * t)
                val sampleValue = (
                    0.65 * sin(2.0 * Math.PI * freq1 * t) +
                    0.35 * sin(2.0 * Math.PI * freq2 * t)
                ) * envelope

                val shortSample = (sampleValue * Short.MAX_VALUE * 0.85).toInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                )
                pcmData[i] = shortSample.toShort()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(maxOf(minBufferSize, totalSamples * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, totalSamples)
            audioTrack.play()

            // Wait until played
            kotlinx.coroutines.delay((durationSec * 1000).toLong())
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            // AudioTrack might fail if device is busy, fallback gracefully
        }
    }
}
