package com.example.stacktower

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Fully synthesised, low-latency sound effects (SoundPool + generated WAVs). */
class Sound(private val ctx: Context) {
    private val rate = 22050
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
    private val ids = ConcurrentHashMap<String, Int>()
    private val noise = Random(7)
    private val scale = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24)

    @Volatile var enabled = true

    init {
        Thread {
            try { build() } catch (_: Exception) { }
        }.start()
    }

    fun play(name: String, vol: Float = 1f) {
        if (!enabled) return
        val id = ids[name] ?: return
        pool.play(id, vol, vol, 1, 0, 1f)
    }

    /** Chime whose pitch climbs with the combo. */
    fun perfect(combo: Int) = play("c" + (combo - 1).coerceIn(0, scale.size - 1))

    fun release() { pool.release() }

    // ---------- synthesis ----------
    private fun buf(ms: Int) = DoubleArray(rate * ms / 1000)

    private fun note(b: DoubleArray, startMs: Int, freq: Double, ms: Int, vol: Double, decay: Double, rich: Boolean = true) {
        val s0 = rate * startMs / 1000
        val n = rate * ms / 1000
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= b.size) break
            val t = i.toDouble() / n
            ph += 2 * PI * freq / rate
            var v = sin(ph)
            if (rich) v += 0.35 * sin(2.0 * ph) + 0.15 * sin(3.01 * ph) + 0.08 * sin(5.4 * ph)
            b[idx] += v * exp(-decay * t) * min(1.0, i / 60.0) * vol
        }
    }

    private fun pcm(b: DoubleArray) = ShortArray(b.size) { (b[it].coerceIn(-1.0, 1.0) * 30000).toInt().toShort() }

    private fun add(name: String, data: ShortArray) {
        val f = File(ctx.cacheDir, "st_$name.wav")
        f.writeBytes(wav(data))
        ids[name] = pool.load(f.path, 1)
    }

    private fun wav(p: ShortArray): ByteArray {
        val bb = ByteBuffer.allocate(44 + p.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + p.size * 2)
        bb.put("WAVE".toByteArray()); bb.put("fmt ".toByteArray())
        bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(rate); bb.putInt(rate * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(p.size * 2)
        for (s in p) bb.putShort(s)
        return bb.array()
    }

    private fun build() {
        // combo chimes
        for (i in scale.indices) {
            val f = 523.25 * 2.0.pow(scale[i] / 12.0)
            val b = buf(650)
            note(b, 0, f, 600, 0.45, 5.0)
            note(b, 0, f * 2, 350, 0.18, 7.0, false)
            note(b, 35, f * 1.5, 500, 0.22, 6.0, false)
            note(b, 70, f * 3, 300, 0.10, 8.0, false)
            add("c$i", pcm(b))
        }
        // soft landing thud
        run {
            val b = buf(200)
            var ph = 0.0
            for (i in b.indices) {
                val t = i.toDouble() / b.size
                ph += 2 * PI * (150 - 95 * t) / rate
                b[i] = sin(ph) * exp(-8 * t) * 0.8 + (noise.nextDouble() - 0.5) * exp(-45 * t) * 0.5
            }
            add("thud", pcm(b))
        }
        // slice / cut-off swoosh
        run {
            val b = buf(280)
            var ph = 0.0
            for (i in b.indices) {
                val t = i.toDouble() / b.size
                ph += 2 * PI * (520 - 380 * t) / rate
                b[i] = (noise.nextDouble() - 0.5) * exp(-7 * t) * 0.45 + sin(ph) * exp(-6 * t) * 0.25
            }
            add("cut", pcm(b))
        }
        // game over: sad descending notes
        run {
            val b = buf(1200)
            note(b, 0, 392.0, 500, 0.4, 4.0)
            note(b, 170, 329.6, 500, 0.4, 4.0)
            note(b, 340, 261.6, 500, 0.4, 4.0)
            note(b, 520, 196.0, 650, 0.45, 3.5)
            add("over", pcm(b))
        }
        // landmark fanfare
        run {
            val b = buf(900)
            val fr = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
            for (k in fr.indices) note(b, k * 90, fr[k], if (k == 3) 600 else 300, 0.35, 4.0)
            add("fanfare", pcm(b))
        }
        // golden block sparkle
        run {
            val b = buf(900)
            for (k in 0 until 8) note(b, k * 55, 784.0 * 2.0.pow(scale[k] / 12.0) / 1.0, 350, 0.25, 6.0, false)
            note(b, 440, 1568.0, 450, 0.3, 4.0)
            add("golden", pcm(b))
        }
        // tiny UI tick
        run {
            val b = buf(50)
            note(b, 0, 900.0, 45, 0.4, 5.0, false)
            add("tick", pcm(b))
        }
    }
}
