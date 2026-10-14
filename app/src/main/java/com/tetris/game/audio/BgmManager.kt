package com.tetris.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 循环 BGM — 无资源文件纯合成，曲库驱动
 * 接 BgmLibrary 6首 118-165 BPM 动感循环；支持选曲/暂停/后台静音
 */
object BgmManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile var enabled: Boolean = true
    @Volatile private var job: Job? = null
    @Volatile private var currentTrackId: String = BgmLibrary.tracks.first().id
    @Volatile private var paused = false
    private const val SR = 44100

    fun start(trackId: String = currentTrackId) {
        currentTrackId = BgmLibrary.byId(trackId).id
        if (!enabled) return
        stop()
        paused = false
        job = scope.launch {
            while (isActive) {
                if (paused || !enabled) { delay(120); continue }
                playLoop(currentTrackId)
            }
        }
    }

    /** 兼容旧调用 start("endless"/"adventure") -> 映射到默认动感 */
    fun startLegacy(mode: String) {
        val id = when (mode) { "adventure" -> "neon"; else -> currentTrackId }
        start(id)
    }

    fun stop() { paused = false; job?.cancel(); job = null }
    fun pause() { paused = true }
    fun resume() { paused = false; if (enabled && job == null) start(currentTrackId) }
    fun setEnabled(on: Boolean, trackId: String = currentTrackId) {
        enabled = on
        if (on) start(trackId) else stop()
    }
    fun currentId(): String = currentTrackId

    private suspend fun playLoop(trackId: String) {
        val track = BgmLibrary.byId(trackId)
        for ((freq, dur) in track.seq) {
            if (!enabled || paused) break
            if (freq == 0.0) { delay(dur.toLong()); continue }
            playToneSync(freq, dur, 0.15f)
            delay(12)
        }
        delay(140)
    }

    private fun playToneSync(freq: Double, durationMs: Int, volume: Float) {
        try {
            val n = (SR * durationMs / 1000.0).toInt().coerceAtLeast(1)
            val buf = ShortArray(n)
            val isKick = freq < 130.0
            if (isKick) {
                // 底鼓：正弦 + 音高下滑 + 指数衰减，拳拳到肉
                val startHz = freq * 1.9
                val endHz = freq
                for (i in 0 until n) {
                    val t = i / SR.toDouble()
                    val prog = i / n.toDouble()
                    val curHz = startHz + (endHz - startHz) * (1 - exp(-prog * 6))
                    val env = exp(-prog * 9.0) * (1 - prog * 0.35)
                    val s = sin(2 * PI * curHz * t) * env
                    // 轻微过载
                    val shaped = s * (1 + s * s * 0.18)
                    buf[i] = (shaped * volume * 1.35 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            } else {
                val atk = (SR * 0.008).toInt()
                val dec = (SR * 0.012).toInt()
                val rel = (SR * 0.038).toInt()
                val sus = 0.88f
                for (i in 0 until n) {
                    val t = i / SR.toDouble()
                    val env = when {
                        i < atk -> i / atk.toFloat() * 1f
                        i < atk + dec -> 1f - (i - atk) / dec.toFloat() * (1f - sus)
                        i > n - rel -> sus * (n - i) / rel.toFloat()
                        else -> sus
                    }.coerceIn(0f, 1f)
                    // 主音 + 五度 + 八度 + 三角泛音，更饱满动感
                    val detune = 1.003
                    val s1 = sin(2 * PI * freq * t) * 0.58
                    val s1d = sin(2 * PI * freq * detune * t) * 0.08
                    val s2 = sin(2 * PI * freq * 1.5 * t) * 0.17
                    val s3 = sin(2 * PI * freq * 2.0 * t) * 0.11
                    // 三角感：软削波
                    val tri = (2 * ((freq * t) % 1) - 1).let { if (it > 0) 0.06f else -0.06f }
                    var sample = (s1 + s1d + s2 + s3 + tri) * volume * env
                    // 轻度软限幅防刺
                    sample = sample / (1 + kotlin.math.abs(sample) * 0.55).toFloat()
                    buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buf, 0, buf.size)
            track.play()
            var waited = 0
            while (waited < durationMs + 8) {
                Thread.sleep(10)
                waited += 10
                if (!enabled || paused) { track.stop(); track.release(); return }
            }
            track.stop(); track.release()
        } catch (_: Exception) {}
    }
}
