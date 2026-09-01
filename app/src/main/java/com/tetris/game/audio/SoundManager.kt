package com.tetris.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 无资源合成音效：用 AudioTrack 实时生成正弦波
 * 轻量、无需音频文件，适配所有机型
 */
object SoundManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private const val SAMPLE_RATE = 44100
    @Volatile var enabled: Boolean = true
    private var lastSoftDropMs = 0L

    fun playMove() = playTone(900.0, 28, 0.18f)
    fun playRotate() = playTone(1200.0, 45, 0.22f)
    fun playSoftDrop() {
        val now = System.currentTimeMillis()
        if (now - lastSoftDropMs < 90) return
        lastSoftDropMs = now
        playTone(380.0, 18, 0.12f)
    }
    fun playLock() = playTone(160.0, 70, 0.25f)

    fun playClear(lines: Int, combo: Int, isTetris: Boolean) {
        if (!enabled) return
        scope.launch {
            when {
                isTetris -> {
                    // Tetris 号角：四连音阶上行
                    playToneSync(523.0, 90, 0.28f); kotlinx.coroutines.delay(30)
                    playToneSync(659.0, 90, 0.28f); kotlinx.coroutines.delay(30)
                    playToneSync(784.0, 90, 0.28f); kotlinx.coroutines.delay(30)
                    playToneSync(1046.0, 180, 0.32f)
                    if (combo > 1) { kotlinx.coroutines.delay(80); playToneSync(1318.0, 120, 0.28f) }
                }
                lines == 3 -> { playToneSync(659.0, 100, 0.26f); kotlinx.coroutines.delay(20); playToneSync(986.0, 160, 0.30f) }
                lines == 2 -> { playToneSync(523.0, 90, 0.24f); kotlinx.coroutines.delay(20); playToneSync(784.0, 130, 0.28f) }
                lines == 1 -> {
                    val base = 520.0 + (combo.coerceAtLeast(1) - 1) * 60.0
                    playToneSync(base, 110, 0.24f)
                }
                else -> playToneSync(440.0, 100, 0.22f)
            }
        }
    }

    fun playGameOver() {
        scope.launch {
            playToneSync(380.0, 180, 0.22f); kotlinx.coroutines.delay(60)
            playToneSync(300.0, 180, 0.22f); kotlinx.coroutines.delay(60)
            playToneSync(180.0, 380, 0.28f)
        }
    }

    fun playLevelClear() {
        scope.launch {
            playToneSync(523.0, 100, 0.26f); kotlinx.coroutines.delay(40)
            playToneSync(659.0, 100, 0.26f); kotlinx.coroutines.delay(40)
            playToneSync(784.0, 120, 0.28f); kotlinx.coroutines.delay(40)
            playToneSync(1046.0, 220, 0.32f)
        }
    }

    fun playPause() = playTone(600.0, 60, 0.15f)

    private fun playTone(freq: Double, durationMs: Int, volume: Float) {
        if (!enabled) return
        scope.launch { playToneSync(freq, durationMs, volume) }
    }

    private fun playToneSync(freq: Double, durationMs: Int, volume: Float) {
        try {
            val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt().coerceAtLeast(1)
            val buffer = ShortArray(numSamples)
            val attack = (SAMPLE_RATE * 0.006).toInt()
            val release = (SAMPLE_RATE * 0.012).toInt()
            for (i in 0 until numSamples) {
                val t = i / SAMPLE_RATE.toDouble()
                var env = 1f
                if (i < attack) env = i / attack.toFloat()
                else if (i > numSamples - release) env = (numSamples - i) / release.toFloat()
                // 轻微方波混合让音色更脆
                val sine = sin(2 * PI * freq * t)
                val square = if (sine > 0) 0.18 else -0.18
                val sample = (sine * 0.82 + square) * volume * env
                buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            // 同步等待播放完成再释放，避免截断
            Thread.sleep(durationMs + 12L)
            track.stop()
            track.release()
        } catch (_: Exception) { }
    }
}
