package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundAndHaptics(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val soundScope = CoroutineScope(Dispatchers.Default)

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    /**
     * Sonido gratificante con tono ascendente al recoger estrellas consecutivas (Combo).
     */
    fun playStarSound(consecutiveCombo: Int = 1) {
        if (!soundEnabled) return
        soundScope.launch {
            try {
                val sampleRate = 22050
                val durationMs = 120
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                // Frecuencia base que escala con el combo (880Hz + escalón armónico de 65Hz)
                val baseFreq = 880.0 + (consecutiveCombo.coerceIn(1, 8) - 1) * 68.0

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val freq = baseFreq + progress * 420.0
                    val envelope = (1.0 - progress) * sin(progress * PI)
                    val sample = (sin(2.0 * PI * i * freq / sampleRate) * envelope * Short.MAX_VALUE * 0.45).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playPcm(buffer, sampleRate)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Sonido de impacto energético y salto cuántico (Teletransporte / Modo Fantasma).
     */
    fun playQuantumJumpSound() {
        if (!soundEnabled) return
        soundScope.launch {
            try {
                val sampleRate = 22050
                val durationMs = 240
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    // Pitch warp ascendente y resonancia cian
                    val freq = 450.0 + sin(progress * PI) * 600.0
                    val envelope = sin(progress * PI) * (1.0 - progress * 0.4)
                    val sample = (sin(2.0 * PI * i * freq / sampleRate) * envelope * Short.MAX_VALUE * 0.50).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playPcm(buffer, sampleRate)
            } catch (_: Exception) {
            }
        }
    }

    fun playThunderShock() {
        if (!soundEnabled) return
        soundScope.launch {
            try {
                // Electric crackle and rumble
                val sampleRate = 22050
                val durationMs = 280
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                val random = Random(42)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val decay = 1.0 - progress
                    // Mix of low frequency boom and white noise crackle
                    val rumble = sin(2.0 * PI * i * 65.0 / sampleRate)
                    val noise = (random.nextFloat() * 2f - 1f)
                    val combined = (rumble * 0.6 + noise * 0.4) * decay
                    val sample = (combined * Short.MAX_VALUE * 0.6).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playPcm(buffer, sampleRate)
            } catch (_: Exception) {
            }
        }
    }

    fun playStartSound() {
        if (!soundEnabled) return
        soundScope.launch {
            try {
                // Rising whoosh sound
                val sampleRate = 22050
                val durationMs = 150
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val freq = 300.0 + progress * 400.0
                    val envelope = sin(progress * PI)
                    val sample = (sin(2.0 * PI * i * freq / sampleRate) * envelope * Short.MAX_VALUE * 0.35).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playPcm(buffer, sampleRate)
            } catch (_: Exception) {
            }
        }
    }

    private fun playPcm(buffer: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(buffer.size * 2, minBufferSize)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size.toFloat() / sampleRate * 1000).toLong() + 30)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Micro-vibración háptica ultra-corta (18ms) al recolectar una estrella.
     */
    fun vibrateStarCollect() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, 140))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Vibración firme y energética al activar la Onda Expansiva / Teletransporte de Seguridad.
     */
    fun vibrateFirmBurst() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 45, 30, 75)
                val amplitudes = intArrayOf(0, 220, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (_: Exception) {
        }
    }

    fun vibrateCollision() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 50, 120)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (_: Exception) {
        }
    }
}
