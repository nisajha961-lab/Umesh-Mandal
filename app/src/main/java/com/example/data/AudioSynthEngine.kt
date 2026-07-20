package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioSynthEngine(private val sharedPrefManager: SharedPrefManager) {
    private val sampleRate = 22050 // Plentiful and fast for 8-bit sound synthesis

    fun playShoot() {
        if (!sharedPrefManager.isSoundOn) return
        generateAndPlay {
            val duration = 0.12f
            val numSamples = (sampleRate * duration).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = t / duration
                // Laser frequency sweep from 800Hz down to 150Hz exponentially
                val freq = 800f * Math.pow(0.1875, progress.toDouble()).toFloat()
                val angle = 2.0 * Math.PI * freq * t
                val amplitude = 1.0f - progress // Linear decay
                samples[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.12f * amplitude).toInt().toShort()
            }
            samples
        }
    }

    fun playExplosion() {
        if (!sharedPrefManager.isSoundOn) return
        generateAndPlay {
            val duration = 0.25f
            val numSamples = (sampleRate * duration).toInt()
            val samples = ShortArray(numSamples)
            val random = java.util.Random()
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                // White noise + bandpass simulation
                val noise = random.nextFloat() * 2f - 1f
                val amplitude = Math.pow(1.0 - progress.toDouble(), 2.0).toFloat() // Exponential decay
                samples[i] = (noise * Short.MAX_VALUE * 0.18f * amplitude).toInt().toShort()
            }
            // Math lowpass filter simulation over samples
            var lastValue = 0
            for (i in 0 until numSamples) {
                val value = samples[i].toInt()
                val smoothed = (value + lastValue) / 2
                samples[i] = smoothed.toShort()
                lastValue = smoothed
            }
            samples
        }
    }

    fun playCoin() {
        if (!sharedPrefManager.isSoundOn) return
        generateAndPlay {
            val duration = 0.22f
            val numSamples = (sampleRate * duration).toInt()
            val samples = ShortArray(numSamples)
            val halfPoint = (numSamples * 0.36f).toInt()
            
            // Retro 8-bit double chime
            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val freq = if (i < halfPoint) 523.25f else 783.99f // C5 -> G5
                phase += 2.0 * Math.PI * freq / sampleRate
                val amplitude = 1.0f - progress // decay
                samples[i] = (Math.sin(phase) * Short.MAX_VALUE * 0.12f * amplitude).toInt().toShort()
            }
            samples
        }
    }

    fun playGameOver() {
        if (!sharedPrefManager.isSoundOn) return
        generateAndPlay {
            val duration = 0.6f
            val numSamples = (sampleRate * duration).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = t / duration
                // Sweeping low-frequency sawtooth wave drop (180Hz down to 45Hz)
                val freq = 180f - progress * 135f
                val period = sampleRate / freq
                val cyclePos = i % period
                val sawValue = 2.0f * (cyclePos / period) - 1.0f
                val amplitude = 1.0f - progress
                samples[i] = (sawValue * Short.MAX_VALUE * 0.12f * amplitude).toInt().toShort()
            }
            samples
        }
    }

    fun playUiTap() {
        if (!sharedPrefManager.isSoundOn) return
        generateAndPlay {
            val duration = 0.04f
            val numSamples = (sampleRate * duration).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = t / duration
                val freq = 1100f // Crisp high-pitch sine beep
                val angle = 2.0 * Math.PI * freq * t
                val amplitude = 1.0f - progress
                samples[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.10f * amplitude).toInt().toShort()
            }
            samples
        }
    }

    private fun generateAndPlay(generator: () -> ShortArray) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val samples = generator()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = Math.max(samples.size, minBufferSize)
                
                val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
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
                        .setBufferSizeInBytes(bufferSize * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 2,
                        AudioTrack.MODE_STATIC
                    )
                }

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                
                val sleepTime = (samples.size.toFloat() / sampleRate * 1000).toLong() + 50
                delay(sleepTime)
                
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("AudioSynthEngine", "Error generating/playing PCM audio: ${e.message}")
            }
        }
    }
}
