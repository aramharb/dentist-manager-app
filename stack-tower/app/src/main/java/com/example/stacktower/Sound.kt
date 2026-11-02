package com.example.stacktower

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Synthesised sound effects (no asset files needed). */
class Sound {
    private val rate = 22050
    private val exec = Executors.newSingleThreadExecutor()
    private val scale = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24) // major pentatonic

    private fun tone(freq: Double, ms: Int, vol: Double, harmonics: Boolean, drop: Double = 0.0): ShortArray {
        val n = rate * ms / 1000
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val f = freq * (1 - drop * t)
            phase += 2 * PI * f / rate
            var s = sin(phase)
            if (harmonics) s += 0.4 * sin(2 * phase) + 0.2 * sin(3 * phase)
            val env = exp(-4.0 * t) * minOf(1.0, i / 80.0)
            out[i] = (s * env * vol * 9000).toInt().toShort()
        }
        return out
    }

    private fun play(pcm: ShortArray) {
        exec.execute {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / rate + 60)
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    /** Satisfying chime; pitch climbs with the combo. */
    fun perfect(combo: Int) {
        val semis = scale[(combo - 1).coerceIn(0, scale.size - 1)]
        val f = 523.25 * 2.0.pow(semis / 12.0)
        val a = tone(f, 260, 1.0, true)
        val b = tone(f * 1.5, 260, 0.5, false)
        play(ShortArray(a.size) { (a[it] + b[it]).toShort() })
    }

    fun place() = play(tone(180.0, 120, 0.9, false, 0.3))
    fun gameOver() = play(tone(300.0, 600, 1.0, true, 0.7))
}
