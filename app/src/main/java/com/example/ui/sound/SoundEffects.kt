package com.example.ui.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundEffects {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playPiecePlace(isAt: Boolean) {
        scope.launch {
            val freq = if (isAt) 523.25 else 440.0 // C5 or A4
            playTone(freq, 80, 0.4f, shape = ToneShape.POP)
        }
    }

    fun playValidMove() {
        scope.launch {
            playTone(659.25, 60, 0.3f, shape = ToneShape.BELL) // E5
        }
    }

    fun playPencilScribble() {
        scope.launch {
            playNoise(durationMs = 90, volume = 0.25f)
        }
    }

    fun playWinFanfare() {
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            for (freq in notes) {
                playTone(freq, 120, 0.5f, shape = ToneShape.BELL)
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun playDrawSound() {
        scope.launch {
            playTone(392.00, 100, 0.4f, shape = ToneShape.BUZZ) // G4
            kotlinx.coroutines.delay(80)
            playTone(349.23, 150, 0.4f, shape = ToneShape.BUZZ) // F4
        }
    }

    fun playEraserSwipe() {
        scope.launch {
            playNoise(durationMs = 150, volume = 0.3f)
        }
    }

    private enum class ToneShape { BELL, POP, BUZZ }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float, shape: ToneShape) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val rawSine = sin(2.0 * Math.PI * freqHz * t)

                val envelope = when (shape) {
                    ToneShape.POP -> (1.0 - (i.toDouble() / numSamples))
                    ToneShape.BELL -> Math.exp(-4.0 * (i.toDouble() / numSamples))
                    ToneShape.BUZZ -> {
                        val decay = 1.0 - (i.toDouble() / numSamples)
                        decay * (if (rawSine > 0) 1.0 else -1.0)
                    }
                }

                val sample = (rawSine * envelope * volume * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // release after playing
            scope.launch {
                kotlinx.coroutines.delay(durationMs + 100L)
                audioTrack.release()
            }
        } catch (_: Exception) {
            // AudioTrack unavailable or muted
        }
    }

    private fun playNoise(durationMs: Int, volume: Float) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)
            var last = 0.0

            for (i in 0 until numSamples) {
                val white = (Math.random() * 2.0 - 1.0)
                // Brown/pink filter for soft paper scratch
                last = (last + (0.05 * white)) / 1.05
                val env = 1.0 - (i.toDouble() / numSamples)
                val sample = (last * env * volume * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                kotlinx.coroutines.delay(durationMs + 100L)
                audioTrack.release()
            }
        } catch (_: Exception) {
        }
    }
}
