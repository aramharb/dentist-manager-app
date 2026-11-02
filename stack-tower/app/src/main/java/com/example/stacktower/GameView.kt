package com.example.stacktower

import android.content.Context
import android.graphics.*
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private class Block(var x: Float, var w: Float, val hue: Float)
private class Debris(var x: Float, var y: Float, val w: Float, val hue: Float, var vy: Float, var vx: Float, var rot: Float = 0f, val vr: Float)
private class Ring(val cx: Float, val y: Float, val w: Float, var age: Float = 0f)

class GameView(context: Context) : View(context), Choreographer.FrameCallback {
    private val sound = Sound()
    private val prefs = context.getSharedPreferences("stacktower", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE }

    private val tower = ArrayList<Block>()
    private val debris = ArrayList<Debris>()
    private val rings = ArrayList<Ring>()
    private var cur = Block(0f, 0f, 0f)
    private var dir = 1f
    private var combo = 0
    private var score = 0
    private var best = prefs.getInt("best", 0)
    private var camY = 0f
    private var over = false
    private var overTime = 0f
    private var started = false
    private var popAge = 1f
    private var popText = ""
    private var lastNs = 0L
    private var running = false
    private var bg = 0f

    private val bh get() = width / 10f
    private val baseY get() = height * 0.75f

    private fun reset() {
        tower.clear(); debris.clear(); rings.clear()
        score = 0; combo = 0; camY = 0f; over = false; started = false
        val w = width * 0.55f
        tower.add(Block((width - w) / 2f, w, 200f))
        spawn()
    }

    private fun spawn() {
        val top = tower.last()
        cur = Block(0f, top.w, (tower.size * 12f + 200f) % 360f)
        dir = if (Random.nextBoolean()) 1f else -1f
        cur.x = if (dir > 0) 0f else width - cur.w
    }

    private fun speed() = width * (0.7f + 0.03f * tower.size).coerceAtMost(1.9f)

    private fun topY(i: Int) = baseY - i * bh

    private fun drop() {
        val top = tower.last()
        val diff = cur.x - top.x
        val tol = width * 0.012f
        val n = tower.size
        if (abs(diff) <= tol) {
            combo++
            score++
            // perfect: snap; long combos even regrow the block a little
            val grow = if (combo >= 5) width * 0.015f else 0f
            val w = (top.w + grow).coerceAtMost(width * 0.8f)
            tower.add(Block(top.x - grow / 2f, w, cur.hue))
            rings.add(Ring(top.x + top.w / 2f, topY(n), w))
            sound.perfect(combo)
            popText = if (combo > 1) "PERFECT x$combo" else "PERFECT"
            popAge = 0f
        } else {
            val overlap = top.w - abs(diff)
            if (overlap <= 0f) { // missed entirely
                debris.add(Debris(cur.x, topY(n), cur.w, cur.hue, 0f, 0f, 0f, 1f))
                endGame(); return
            }
            combo = 0
            score++
            val nx = max(cur.x, top.x)
            tower.add(Block(nx, overlap, cur.hue))
            val cutW = cur.w - overlap
            val cutX = if (diff > 0) top.x + top.w else cur.x
            debris.add(Debris(cutX, topY(n), cutW, cur.hue, 0f, if (diff > 0) 120f else -120f, 0f, if (diff > 0) 90f else -90f))
            sound.place()
        }
        spawn()
    }

    private fun endGame() {
        over = true; overTime = 0f
        if (score > best) { best = score; prefs.edit().putInt("best", best).apply() }
        sound.gameOver()
    }

    private fun update(dt: Float) {
        bg += dt * 4f
        if (started && !over) {
            cur.x += dir * speed() * dt
            val max = width - cur.w
            if (cur.x > max) { cur.x = max; dir = -1f } else if (cur.x < 0f) { cur.x = 0f; dir = 1f }
        } else if (!started && !over) {
            cur.x = (width - cur.w) / 2f + kotlin.math.sin(bg * 0.25f) * width * 0.3f
        }
        if (over) overTime += dt
        val target = max(0f, (tower.size + 1) * bh - height * 0.5f)
        camY += (target - camY) * (1f - Math.exp(-6.0 * dt).toFloat())
        val iter = debris.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            d.vy += height * 2.2f * dt; d.y += d.vy * dt; d.x += d.vx * dt; d.rot += d.vr * dt
            if (d.y - camY > height + 200f) iter.remove()
        }
        rings.removeAll { it.age > 0.5f }
        rings.forEach { it.age += dt }
        popAge += dt
    }

    private fun color(h: Float, v: Float = 1f) = Color.HSVToColor(floatArrayOf(h, 0.55f, v))

    override fun onDraw(c: Canvas) {
        val hue = (tower.size * 12f + 200f) % 360f
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
            Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.25f)), Color.HSVToColor(floatArrayOf((hue + 40) % 360, 0.6f, 0.55f)), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        fun block(x: Float, y: Float, w: Float, h: Float) {
            // y is world top-of-block; apply camera
            val sy = y + camY
            paint.color = color(h)
            c.drawRect(x, sy - bh, x + w, sy, paint)
            paint.color = color(h, 0.75f)
            c.drawRect(x, sy - bh * 0.18f, x + w, sy, paint)
        }
        for ((i, b) in tower.withIndex()) block(b.x, topY(i), b.w, b.hue)
        if (!over) block(cur.x, topY(tower.size), cur.w, cur.hue)
        for (d in debris) {
            c.save(); c.rotate(d.rot, d.x + d.w / 2, d.y + camY - bh / 2)
            block(d.x, d.y, d.w, d.hue); c.restore()
        }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 6f
        for (r in rings) {
            val t = r.age / 0.5f
            paint.color = Color.argb(((1 - t) * 255).toInt(), 255, 255, 255)
            val g = bh * 0.6f * t + 4f
            c.drawRect(r.cx - r.w / 2 - g, r.y + camY - bh - g, r.cx + r.w / 2 + g, r.y + camY + g, paint)
        }
        paint.style = Paint.Style.FILL

        text.textSize = width * 0.22f; text.typeface = Typeface.DEFAULT_BOLD; text.alpha = 255
        c.drawText(score.toString(), width / 2f, height * 0.16f, text)
        text.textSize = width * 0.05f
        c.drawText("BEST $best", width / 2f, height * 0.16f + width * 0.08f, text)
        if (popAge < 0.8f) {
            text.textSize = width * 0.075f * (1f + 0.4f * (1f - popAge / 0.8f))
            text.alpha = ((1f - popAge / 0.8f) * 255).toInt()
            c.drawText(popText, width / 2f, height * 0.28f, text)
            text.alpha = 255
        }
        if (!started && !over) {
            text.textSize = width * 0.06f
            c.drawText("TAP TO DROP", width / 2f, height * 0.9f, text)
        }
        if (over) {
            paint.color = Color.argb(140, 0, 0, 0)
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            text.textSize = width * 0.12f
            c.drawText("GAME OVER", width / 2f, height * 0.45f, text)
            text.textSize = width * 0.06f
            c.drawText("Height: $score   Best: $best", width / 2f, height * 0.52f, text)
            if (overTime > 0.5f) c.drawText("Tap to play again", width / 2f, height * 0.6f, text)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        when {
            over -> if (overTime > 0.5f) reset()
            !started -> started = true
            else -> drop()
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { reset() }

    override fun doFrame(ns: Long) {
        if (!running) return
        val dt = if (lastNs == 0L) 0f else ((ns - lastNs) / 1e9f).coerceAtMost(0.05f)
        lastNs = ns
        update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun resumeGame() { running = true; lastNs = 0L; Choreographer.getInstance().postFrameCallback(this) }
    fun pauseGame() { running = false }
}
