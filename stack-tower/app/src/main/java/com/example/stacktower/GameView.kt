package com.example.stacktower

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private class Block(var x: Float, var w: Float, val hue: Float, val golden: Boolean)
private class Debris(var x: Float, var y: Float, val w: Float, val hue: Float, val golden: Boolean,
                     var vx: Float, var vy: Float, var rot: Float, val vr: Float)
private class Ring(val cx: Float, val y: Float, val w: Float, var age: Float = 0f)
private class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float,
                    val maxLife: Float, val color: Int, val size: Float, val confetti: Boolean, var rot: Float = 0f)
private class Cloud(var x: Float, var y0: Float, val s: Float, val speed: Float)
private class Star(val x: Float, val y: Float, val r: Float, val ph: Float)
private class Bird(var x: Float, val y0: Float, val speed: Float, val ph: Float)

class GameView(context: Context) : View(context), Choreographer.FrameCallback {

    private enum class State { MENU, PLAYING, FALLING, PAUSED, OVER }

    // ---------- tools ----------
    private val sound = Sound(context)
    private val prefs = context.getSharedPreferences("stacktower", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(18f, 14f), 0f)
    }
    private val path = Path()
    private val rect = RectF()
    private val argb = ArgbEvaluator()
    private val hsv = FloatArray(3)
    private val rnd = Random(System.nanoTime())

    // ---------- world data ----------
    private val tower = ArrayList<Block>()
    private val debris = ArrayList<Debris>()
    private val rings = ArrayList<Ring>()
    private val sparks = ArrayList<Spark>()
    private var cur = Block(0f, 0f, 0f, false)
    private val clouds = ArrayList<Cloud>()
    private val stars = ArrayList<Star>()
    private val birds = ArrayList<Bird>()
    private var vignette: RadialGradient? = null

    private val landmarks = listOf(
        6 to "🦒 Giraffe", 12 to "🏠 House", 20 to "🗽 Statue of Liberty",
        30 to "🗼 Eiffel Tower", 45 to "☁️ Cloud level", 60 to "✈️ Airplane",
        80 to "🛰️ Space station", 100 to "🌙 The Moon", 150 to "🪐 Saturn"
    )
    private val tips = listOf(
        "Perfect drops snap in place and chain into combos",
        "Chain perfects to make your block grow back",
        "Golden blocks are slow and give +3 and extra width",
        "How high can you go? Reach the Moon!",
        "Every block you miss makes the tower thinner"
    )
    private val popTexts = listOf("PERFECT!", "PERFECT x2", "GREAT x3", "AMAZING x4", "INCREDIBLE x5", "UNREAL x6")

    // ---------- state ----------
    private var state = State.MENU
    private var time = 0f
    private var phase = 0f
    private var score = 0
    private var best = prefs.getInt("best", 0)
    private var plays = prefs.getInt("plays", 0)
    private var combo = 0
    private var maxCombo = 0
    private var newBest = false
    private var nextLm = 0
    private var nextGolden = 10
    private var camY = 0f
    private var skyAlt = 0f
    private var zoom = 1f
    private var shift = 0f
    private var dropOff = 0f
    private var fallV = 0f
    private var squash = 0f
    private var shake = 0f
    private var flash = 0f
    private var scoreBump = 0f
    private var popAge = 9f
    private var popText = ""
    private var popSub = ""
    private var bannerAge = 9f
    private var bannerText = ""
    private var overTime = 0f
    private var tip = tips[0]
    private var lastNs = 0L
    private var running = false

    // sky extras
    private var ufoOn = false
    private var ufoX = 0f
    private var ufoY = 0f
    private var ufoDir = 1f
    private var ufoTimer = 8f
    private var ssLife = 0f
    private var ssX = 0f
    private var ssY = 0f
    private var ssVX = 0f
    private var ssVY = 0f
    private var ssTimer = 3f

    private val bh get() = width * 0.085f
    private val baseY get() = height * 0.78f
    private val u get() = width / 100f
    private val hover get() = bh * 0.55f
    private val btnR get() = width * 0.055f
    private val pauseX get() = width * 0.09f
    private val soundX get() = width * 0.91f
    private val btnY get() = width * 0.13f

    private companion object {
        val ALTS = floatArrayOf(0f, 20f, 45f, 75f, 110f)
        val SKY_TOP = intArrayOf(0xFF3D8FD6.toInt(), 0xFF5B3F8F.toInt(), 0xFF0B1038.toInt(), 0xFF02030F.toInt(), 0xFF000000.toInt())
        val SKY_BOT = intArrayOf(0xFFBDE6FF.toInt(), 0xFFFFA45C.toInt(), 0xFF283A7A.toInt(), 0xFF0B1235.toInt(), 0xFF04061A.toInt())
        val CLOUD = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFD2B0.toInt(), 0xFF55608F.toInt(), 0xFF2A3050.toInt(), 0xFF2A3050.toInt())
        const val TWO_PI = 6.2831855f
    }

    init {
        sound.enabled = prefs.getBoolean("sound", true)
        isHapticFeedbackEnabled = true
    }

    // ---------- helpers ----------
    private fun hsvc(h: Float, s: Float, v: Float, a: Int = 255): Int {
        hsv[0] = ((h % 360f) + 360f) % 360f
        hsv[1] = s.coerceIn(0f, 1f)
        hsv[2] = v.coerceIn(0f, 1f)
        return Color.HSVToColor(a.coerceIn(0, 255), hsv)
    }

    private fun lerpC(a: Int, b: Int, t: Float): Int = argb.evaluate(t.coerceIn(0f, 1f), a, b) as Int

    private fun stops(alt: Float, cols: IntArray): Int {
        if (alt <= ALTS[0]) return cols[0]
        for (i in 1 until ALTS.size) {
            if (alt <= ALTS[i]) return lerpC(cols[i - 1], cols[i], (alt - ALTS[i - 1]) / (ALTS[i] - ALTS[i - 1]))
        }
        return cols[cols.size - 1]
    }

    private fun withAlpha(c: Int, a: Float) = Color.argb((a.coerceIn(0f, 1f) * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))

    private fun bottomY(i: Int) = baseY - i * bh
    private fun sway(i: Int) = sin(time * 1.2f) * width * 0.004f * sqrt(i.toFloat())

    private fun label(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = Color.WHITE,
                      align: Paint.Align = Paint.Align.CENTER, alpha: Int = 255) {
        text.textSize = size
        text.color = color
        text.alpha = alpha.coerceIn(0, 255)
        text.textAlign = align
        text.setShadowLayer(size * 0.07f, 0f, size * 0.05f, Color.argb((alpha * 0.45f).toInt().coerceIn(0, 255), 0, 0, 0))
        c.drawText(s, x, y, text)
    }

    private fun haptic(kind: Int) { performHapticFeedback(kind) }

    // ---------- game flow ----------
    private fun reset(startPlaying: Boolean) {
        tower.clear(); debris.clear(); rings.clear(); sparks.clear()
        score = 0; combo = 0; maxCombo = 0; nextLm = 0; newBest = false
        nextGolden = 9 + rnd.nextInt(6)
        squash = 0f; shake = 0f; flash = 0f
        popAge = 9f; bannerAge = 9f
        val w = width * 0.5f
        tower.add(Block((width - w) / 2f, w, 190f, false))
        tip = tips[rnd.nextInt(tips.size)]
        state = if (startPlaying) State.PLAYING else State.MENU
        spawn()
    }

    private fun spawn() {
        val top = tower[tower.size - 1]
        val golden = score >= nextGolden
        cur = Block(0f, top.w, (tower.size * 9f + 190f) % 360f, golden)
        dropOff = hover
        fallV = 0f
        phase = if (rnd.nextBoolean()) -TWO_PI / 4f else TWO_PI / 4f
        positionCur()
    }

    private fun positionCur() {
        cur.x = (width - cur.w) * (0.5f + 0.5f * sin(phase))
    }

    private fun omega(): Float {
        val o = min(2.0f + 0.045f * score, 5.2f)
        return if (cur.golden) o * 0.7f else o
    }

    private fun startDrop() {
        state = State.FALLING
        fallV = height * 0.8f
        sound.play("tick", 0.5f)
    }

    private fun burst(x: Float, y: Float, n: Int, confetti: Boolean, gold: Boolean) {
        if (sparks.size > 400) return
        for (i in 0 until n) {
            val ang = rnd.nextFloat() * TWO_PI
            val sp = width * (0.2f + rnd.nextFloat() * 0.7f)
            val col = when {
                confetti -> hsvc(rnd.nextFloat() * 360f, 0.75f, 1f)
                gold -> hsvc(40f + rnd.nextFloat() * 15f, 0.8f, 1f)
                else -> Color.WHITE
            }
            val l = 0.5f + rnd.nextFloat() * 0.6f
            sparks.add(Spark(x, y, kotlin.math.cos(ang) * sp, sin(ang) * sp - width * 0.3f, l, l, col,
                u * (if (confetti) 1.6f else 0.9f) * (0.6f + rnd.nextFloat()), confetti, rnd.nextFloat() * 360f))
        }
    }

    private fun land() {
        val top = tower[tower.size - 1]
        val n = tower.size
        val diff = cur.x - top.x
        val tol = width * 0.022f
        val wy = bottomY(n)
        val topX = top.x + sway(n - 1)
        squash = 1f

        if (abs(diff) <= tol) {
            // ---------- perfect ----------
            combo++
            maxCombo = max(maxCombo, combo)
            var nw = top.w
            popSub = ""
            if (combo >= 4) { nw = min(nw + width * 0.012f, width * 0.75f); popSub = "BLOCK GROWS!" }
            var gain = 1
            if (cur.golden) {
                gain = 3
                nw = min(nw + width * 0.08f, width * 0.75f)
                popSub = "GOLDEN BONUS +3"
                burst(width / 2f, wy - bh, 50, true, false)
                sound.play("golden")
                nextGolden = score + gain + 10 + rnd.nextInt(8)
            }
            tower.add(Block(top.x - (nw - top.w) / 2f, nw, cur.hue, cur.golden))
            score += gain
            rings.add(Ring(topX + top.w / 2f, wy, nw))
            burst(topX, wy - bh * 0.5f, 8, false, true)
            burst(topX + top.w, wy - bh * 0.5f, 8, false, true)
            burst(topX + top.w / 2f, wy - bh, 8, false, false)
            sound.perfect(combo)
            haptic(HapticFeedbackConstants.KEYBOARD_TAP)
            popText = if (combo <= popTexts.size) popTexts[combo - 1] else "LEGENDARY x$combo"
            popAge = 0f
            flash = 0.3f
        } else {
            val overlap = top.w - abs(diff)
            if (overlap <= bh * 0.1f) {
                // ---------- complete miss ----------
                debris.add(Debris(cur.x, wy - dropOff, cur.w, cur.hue, cur.golden,
                    if (diff > 0) 60f else -60f, 0f, 0f, if (diff > 0) 70f else -70f))
                shake = 1f
                endGame()
                return
            }
            // ---------- trimmed ----------
            combo = 0
            val nx = max(cur.x, top.x)
            tower.add(Block(nx, overlap, cur.hue, cur.golden))
            score++
            if (cur.golden) nextGolden = score + 10 + rnd.nextInt(8)
            val cutX = if (diff > 0) top.x + top.w else cur.x
            val dir = if (diff > 0) 1f else -1f
            debris.add(Debris(cutX, wy, abs(diff), cur.hue, cur.golden, dir * width * 0.12f, 0f, 0f, dir * 110f))
            burst(cutX + (if (diff > 0) 0f else abs(diff)), wy - bh * 0.5f, 6, false, false)
            shake = 0.45f
            sound.play("thud")
            sound.play("cut", 0.6f)
            haptic(HapticFeedbackConstants.CLOCK_TICK)
            popText = ""
        }
        scoreBump = 1f
        while (nextLm < landmarks.size && score >= landmarks[nextLm].first) {
            bannerText = "Reached: " + landmarks[nextLm].second
            bannerAge = 0f
            burst(width / 2f, bottomY(tower.size) - bh, 40, true, false)
            sound.play("fanfare")
            nextLm++
        }
        state = State.PLAYING
        spawn()
    }

    private fun endGame() {
        state = State.OVER
        overTime = 0f
        plays++
        newBest = score > best
        if (newBest) best = score
        prefs.edit().putInt("best", best).putInt("plays", plays).apply()
        tip = tips[rnd.nextInt(tips.size)]
        sound.play("over")
        haptic(HapticFeedbackConstants.LONG_PRESS)
    }

    // ---------- update ----------
    private fun update(dt: Float) {
        if (state == State.PAUSED) return
        time += dt
        scoreBump = max(0f, scoreBump - dt * 4f)
        popAge += dt
        bannerAge += dt
        flash = max(0f, flash - dt * 2.5f)
        squash = max(0f, squash - dt * 4.5f)
        shake = max(0f, shake - dt * 2.5f)

        when (state) {
            State.MENU -> { phase += 1.6f * dt; positionCur() }
            State.PLAYING -> { phase += omega() * dt; positionCur() }
            State.FALLING -> {
                fallV += height * 7f * dt
                dropOff -= fallV * dt
                if (dropOff <= 0f) { dropOff = 0f; land() }
            }
            State.OVER -> overTime += dt
            else -> {}
        }

        // camera, zoom
        val n = tower.size
        val over = state == State.OVER
        val target = if (over) 0f else max(0f, n * bh - height * 0.30f)
        camY += (target - camY) * (1f - exp(-5f * dt))
        val k = 1f - exp(-2.8f * dt)
        val zT = if (over) min(1f, height * 0.38f / ((n + 1) * bh)) else 1f
        val sT = if (over) baseY - height * 0.45f else 0f
        zoom += (zT - zoom) * k
        shift += (sT - shift) * k
        if (!over) skyAlt += (camY / bh - skyAlt) * (1f - exp(-3f * dt))

        // debris
        val di = debris.iterator()
        while (di.hasNext()) {
            val d = di.next()
            d.vy += height * 2.4f * dt
            d.y += d.vy * dt
            d.x += d.vx * dt
            d.rot += d.vr * dt
            if (d.y > baseY + bh * 2f) di.remove()
        }
        // rings
        val ri = rings.iterator()
        while (ri.hasNext()) { val r = ri.next(); r.age += dt; if (r.age > 0.55f) ri.remove() }
        // sparks
        val si = sparks.iterator()
        while (si.hasNext()) {
            val s = si.next()
            s.life -= dt
            s.vy += width * 1.1f * dt
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.rot += 400f * dt
            if (s.life <= 0f) si.remove()
        }

        // background life
        for (cl in clouds) {
            cl.x += cl.speed * dt
            if (cl.x > width + cl.s * 2f) cl.x = -cl.s * 2f
            if (cl.y0 + camY * 0.45f > height + cl.s * 2f) cl.y0 -= height * 1.3f
        }
        for (b in birds) {
            b.x += b.speed * dt
            if (b.x > width + 40f) b.x = -40f
            if (b.x < -40f && b.speed < 0f) b.x = width + 40f
        }
        if (skyAlt > 55f) {
            if (!ufoOn) {
                ufoTimer -= dt
                if (ufoTimer <= 0f) {
                    ufoOn = true; ufoDir = if (rnd.nextBoolean()) 1f else -1f
                    ufoX = if (ufoDir > 0) -width * 0.1f else width * 1.1f
                    ufoY = height * (0.15f + rnd.nextFloat() * 0.5f)
                    ufoTimer = 14f + rnd.nextFloat() * 12f
                }
            } else {
                ufoX += ufoDir * width * 0.22f * dt
                ufoY += sin(time * 3f) * 20f * dt
                if (ufoX < -width * 0.2f || ufoX > width * 1.2f) ufoOn = false
            }
        }
        if (skyAlt > 30f) {
            if (ssLife <= 0f) {
                ssTimer -= dt
                if (ssTimer <= 0f) {
                    ssLife = 0.7f; ssX = width * (0.3f + rnd.nextFloat() * 0.7f); ssY = height * rnd.nextFloat() * 0.4f
                    ssVX = -width * 0.9f; ssVY = width * 0.45f; ssTimer = 3f + rnd.nextFloat() * 5f
                }
            } else {
                ssLife -= dt; ssX += ssVX * dt; ssY += ssVY * dt
            }
        }
    }

    // ---------- drawing: background ----------
    private fun drawHill(c: Canvas, baseline: Float, amp: Float, freq: Float, ph: Float, pointy: Boolean, color: Int) {
        if (baseline - amp > height) return
        path.reset()
        path.moveTo(0f, height.toFloat() + 10f)
        val steps = 24
        for (i in 0..steps) {
            val x = width * i / steps.toFloat()
            val s = sin(x / width * freq * TWO_PI + ph)
            val v = if (pointy) 1f - abs(s) else 0.5f + 0.5f * s
            path.lineTo(x, baseline - amp * v)
        }
        path.lineTo(width.toFloat(), height.toFloat() + 10f)
        path.close()
        paint.color = color
        c.drawPath(path, paint)
    }

    private fun drawBackground(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = skyAlt
        val top = stops(a, SKY_TOP)
        val bot = stops(a, SKY_BOT)
        paint.shader = LinearGradient(0f, 0f, 0f, h, top, bot, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // stars
        val sa = ((a - 26f) / 22f).coerceIn(0f, 1f)
        if (sa > 0f) {
            for (s in stars) {
                val tw = 0.6f + 0.4f * sin(time * 2f + s.ph)
                paint.color = Color.argb((255 * sa * tw).toInt(), 255, 255, 255)
                c.drawCircle(s.x * w, s.y * h, s.r, paint)
            }
        }
        // shooting star
        if (ssLife > 0f) {
            stroke.strokeWidth = 3f
            stroke.color = Color.argb((ssLife / 0.7f * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            c.drawLine(ssX, ssY, ssX - ssVX * 0.12f, ssY - ssVY * 0.12f, stroke)
        }
        // sun
        val sunA = (1f - (a - 6f) / 30f).coerceIn(0f, 1f)
        if (sunA > 0f) {
            val sx = w * 0.78f
            val sy = h * (0.17f + (a / 40f) * 0.5f)
            paint.shader = RadialGradient(sx, sy, w * 0.38f,
                intArrayOf(Color.argb((sunA * 150).toInt(), 255, 240, 190), Color.argb(0, 255, 200, 120)), null, Shader.TileMode.CLAMP)
            c.drawCircle(sx, sy, w * 0.38f, paint)
            paint.shader = null
            paint.color = Color.argb((sunA * 255).toInt(), 255, 246, 214)
            c.drawCircle(sx, sy, w * 0.065f, paint)
        }
        // moon
        val moonA = ((a - 32f) / 20f).coerceIn(0f, 1f)
        if (moonA > 0f) {
            val mx = w * 0.2f
            val my = h * 0.19f
            paint.shader = RadialGradient(mx, my, w * 0.25f,
                intArrayOf(Color.argb((moonA * 90).toInt(), 200, 215, 255), Color.argb(0, 200, 215, 255)), null, Shader.TileMode.CLAMP)
            c.drawCircle(mx, my, w * 0.25f, paint)
            paint.shader = null
            paint.color = Color.argb((moonA * 255).toInt(), 240, 243, 255)
            c.drawCircle(mx, my, w * 0.06f, paint)
            paint.color = Color.argb((moonA * 50).toInt(), 120, 130, 170)
            c.drawCircle(mx - w * 0.02f, my - w * 0.012f, w * 0.014f, paint)
            c.drawCircle(mx + w * 0.015f, my + w * 0.02f, w * 0.01f, paint)
        }
        // ringed planet
        val plA = ((a - 62f) / 20f).coerceIn(0f, 1f)
        if (plA > 0f) {
            val px = w * 0.72f
            val py = h * 0.3f + sin(time * 0.4f) * 6f
            val pr = w * 0.1f
            c.save()
            c.rotate(-18f, px, py)
            rect.set(px - pr * 1.9f, py - pr * 0.45f, px + pr * 1.9f, py + pr * 0.45f)
            stroke.strokeWidth = pr * 0.14f
            stroke.color = Color.argb((plA * 150).toInt(), 255, 220, 170)
            c.drawOval(rect, stroke)
            paint.shader = RadialGradient(px - pr * 0.3f, py - pr * 0.3f, pr * 1.3f,
                intArrayOf(Color.argb((plA * 255).toInt(), 255, 190, 120), Color.argb((plA * 255).toInt(), 150, 70, 60)), null, Shader.TileMode.CLAMP)
            c.drawCircle(px, py, pr, paint)
            paint.shader = null
            c.drawArc(rect, 0f, 180f, false, stroke)
            c.restore()
        }
        // UFO
        if (ufoOn) {
            val bob = sin(time * 4f) * 4f
            paint.color = Color.argb(40, 150, 255, 200)
            path.reset()
            path.moveTo(ufoX - w * 0.02f, ufoY + bob); path.lineTo(ufoX + w * 0.02f, ufoY + bob)
            path.lineTo(ufoX + w * 0.07f, ufoY + h * 0.12f); path.lineTo(ufoX - w * 0.07f, ufoY + h * 0.12f); path.close()
            c.drawPath(path, paint)
            paint.color = Color.argb(200, 130, 230, 255)
            c.drawCircle(ufoX, ufoY + bob - w * 0.012f, w * 0.022f, paint)
            paint.color = Color.rgb(170, 175, 190)
            rect.set(ufoX - w * 0.05f, ufoY + bob - w * 0.012f, ufoX + w * 0.05f, ufoY + bob + w * 0.018f)
            c.drawOval(rect, paint)
            for (i in -1..1) {
                paint.color = if (((time * 4f).toInt() + i) % 2 == 0) Color.YELLOW else Color.rgb(255, 120, 120)
                c.drawCircle(ufoX + i * w * 0.025f, ufoY + bob + w * 0.004f, w * 0.005f, paint)
            }
        }
        // clouds
        val cloudA = (1f - (a - 38f) / 30f).coerceIn(0f, 1f)
        if (cloudA > 0f) {
            paint.color = withAlpha(stops(a, CLOUD), cloudA * 0.92f)
            for (cl in clouds) {
                val cy = cl.y0 + camY * 0.45f
                val s = cl.s
                c.drawCircle(cl.x, cy, s, paint)
                c.drawCircle(cl.x + s * 0.9f, cy + s * 0.18f, s * 0.8f, paint)
                c.drawCircle(cl.x - s * 0.9f, cy + s * 0.22f, s * 0.7f, paint)
                c.drawCircle(cl.x + s * 0.15f, cy - s * 0.4f, s * 0.7f, paint)
                c.drawRect(cl.x - s * 1.4f, cy + s * 0.3f, cl.x + s * 1.5f, cy + s * 0.95f, paint)
            }
        }
        // birds (daytime, low)
        if (a < 22f) {
            stroke.strokeWidth = 3f
            stroke.color = Color.argb(((1f - a / 22f) * 150).toInt(), 40, 50, 70)
            for (b in birds) {
                val by = b.y0 + camY * 0.8f
                if (by > h || by < 0f) continue
                val fl = sin(time * 9f + b.ph) * 7f
                path.reset()
                path.moveTo(b.x - 13f, by - fl * 0.3f)
                path.quadTo(b.x - 6f, by - fl, b.x, by)
                path.quadTo(b.x + 6f, by - fl, b.x + 13f, by - fl * 0.3f)
                c.drawPath(path, stroke)
            }
        }
        // hills / mountains
        val nf = (a / 45f).coerceIn(0f, 1f)
        val far = lerpC(lerpC(bot, 0xFF4A6FA5.toInt(), 0.5f), 0xFF0B1030.toInt(), nf * 0.7f)
        val near = lerpC(lerpC(bot, 0xFF2F7D46.toInt(), 0.6f), 0xFF0A1A12.toInt(), nf * 0.8f)
        drawHill(c, baseY - bh * 2f + camY * 0.35f, bh * 5f, 1.6f, 0.6f, true, far)
        drawHill(c, baseY - bh * 0.4f + camY * 0.7f, bh * 2f, 1.2f, 2.0f, false, near)

        // ground
        val gy = baseY + camY * zoom - shift
        if (gy < h) {
            val grass = lerpC(0xFF58B85A.toInt(), 0xFF123820.toInt(), nf)
            val soil = lerpC(0xFF8A6642.toInt(), 0xFF22160F.toInt(), nf)
            paint.color = grass
            c.drawRect(0f, gy, w, h, paint)
            paint.color = soil
            c.drawRect(0f, gy + bh * 0.35f, w, h, paint)
            paint.color = lerpC(grass, Color.WHITE, 0.18f)
            c.drawRect(0f, gy, w, gy + bh * 0.07f, paint)
            // little trees
            val tc = lerpC(0xFF2E8B57.toInt(), 0xFF0C2A18.toInt(), nf)
            val tk = lerpC(0xFF6B4A2B.toInt(), 0xFF1A120B.toInt(), nf)
            for (tx in floatArrayOf(0.06f, 0.2f, 0.84f, 0.95f)) {
                val x = tx * w
                val s = bh * (if (tx > 0.5f) 1.0f else 1.3f)
                paint.color = tk
                c.drawRect(x - s * 0.08f, gy - s * 0.4f, x + s * 0.08f, gy, paint)
                paint.color = tc
                path.reset()
                path.moveTo(x, gy - s * 1.9f); path.lineTo(x - s * 0.5f, gy - s * 0.5f); path.lineTo(x + s * 0.5f, gy - s * 0.5f); path.close()
                c.drawPath(path, paint)
                path.reset()
                path.moveTo(x, gy - s * 1.4f); path.lineTo(x - s * 0.65f, gy - s * 0.2f); path.lineTo(x + s * 0.65f, gy - s * 0.2f); path.close()
                c.drawPath(path, paint)
            }
        }
    }

    // ---------- drawing: world ----------
    private fun drawBlock(c: Canvas, x: Float, wy: Float, w: Float, hue: Float, golden: Boolean, hf: Float = 1f) {
        val sy = wy + camY
        val bhh = bh * hf
        val dx = bh * 0.38f
        val dy = bh * 0.28f
        val sat = if (golden) 0.85f else 0.5f
        val hh = if (golden) 46f else hue
        // right face
        paint.color = hsvc(hh, sat, 0.58f)
        path.reset()
        path.moveTo(x + w, sy - bhh); path.lineTo(x + w + dx, sy - bhh - dy)
        path.lineTo(x + w + dx, sy - dy); path.lineTo(x + w, sy); path.close()
        c.drawPath(path, paint)
        // top face
        paint.color = hsvc(hh, sat * 0.65f, 1f)
        path.reset()
        path.moveTo(x, sy - bhh); path.lineTo(x + dx, sy - bhh - dy)
        path.lineTo(x + w + dx, sy - bhh - dy); path.lineTo(x + w, sy - bhh); path.close()
        c.drawPath(path, paint)
        // front face
        paint.color = hsvc(hh, sat, 0.86f)
        c.drawRect(x, sy - bhh, x + w, sy, paint)
        // shading strip + highlight
        paint.color = hsvc(hh, sat, 0.7f)
        c.drawRect(x, sy - bhh * 0.16f, x + w, sy, paint)
        paint.color = Color.argb(70, 255, 255, 255)
        c.drawRect(x, sy - bhh, x + w, sy - bhh + bhh * 0.07f, paint)
        if (golden) {
            label(c, "★", x + w / 2f, sy - bhh * 0.22f, bhh * 0.75f, Color.WHITE, Paint.Align.CENTER, 230)
        }
    }

    private fun drawWorld(c: Canvas) {
        val w = width.toFloat()
        // landmark lines
        for (lm in landmarks) {
            val sy = baseY - (lm.first + 1) * bh + camY
            if (sy < -40f || sy > height + 40f) continue
            val passed = score >= lm.first
            dash.color = Color.argb(if (passed) 45 else 150, 255, 255, 255)
            c.drawLine(0f, sy, w, sy, dash)
            label(c, lm.second + "  " + lm.first, w - u * 3f, sy - u * 1.5f, u * 3.8f, Color.WHITE, Paint.Align.RIGHT, if (passed) 90 else 220)
        }
        val n = tower.size
        val top = tower[n - 1]
        // aiming guides
        if (state == State.PLAYING || state == State.MENU) {
            val gy = bottomY(n) + camY
            val tx = top.x + sway(n - 1)
            dash.color = Color.argb(70, 255, 255, 255)
            c.drawLine(tx, gy, tx, 0f, dash)
            c.drawLine(tx + top.w, gy, tx + top.w, 0f, dash)
        }
        // tower
        for (i in 0 until n) {
            val b = tower[i]
            val hf = if (i == n - 1 && squash > 0f) 1f - 0.16f * squash else 1f
            drawBlock(c, b.x + sway(i), bottomY(i), b.w, b.hue, b.golden, hf)
        }
        // moving block
        if (state != State.OVER) {
            drawBlock(c, cur.x + sway(n), bottomY(n) - dropOff, cur.w, cur.hue, cur.golden)
        }
        // debris
        for (d in debris) {
            c.save()
            c.rotate(d.rot, d.x + d.w / 2f, d.y + camY - bh / 2f)
            drawBlock(c, d.x, d.y, d.w, d.hue, d.golden)
            c.restore()
        }
        // rings
        stroke.strokeWidth = u * 0.7f
        for (r in rings) {
            val t = r.age / 0.55f
            stroke.color = Color.argb(((1f - t) * 230).toInt().coerceIn(0, 255), 255, 255, 255)
            val g = bh * 0.8f * t + 3f
            rect.set(r.cx - r.w / 2f - g, r.y + camY - bh - g, r.cx + r.w / 2f + g, r.y + camY + g)
            c.drawRoundRect(rect, g, g, stroke)
        }
        // sparks / confetti
        for (s in sparks) {
            val a = (s.life / s.maxLife).coerceIn(0f, 1f)
            paint.color = withAlpha(s.color, a)
            val sy = s.y + camY
            if (s.confetti) {
                c.save(); c.rotate(s.rot, s.x, sy)
                c.drawRect(s.x - s.size, sy - s.size * 0.5f, s.x + s.size, sy + s.size * 0.5f, paint)
                c.restore()
            } else {
                c.drawCircle(s.x, sy, s.size * (0.4f + a), paint)
            }
        }
    }

    // ---------- drawing: UI ----------
    private fun drawButtons(c: Canvas) {
        val r = btnR
        paint.color = Color.argb(70, 0, 0, 0)
        if (state == State.PLAYING || state == State.PAUSED || state == State.FALLING) {
            c.drawCircle(pauseX, btnY, r, paint)
            paint.color = Color.WHITE
            if (state == State.PAUSED) {
                path.reset()
                path.moveTo(pauseX - r * 0.3f, btnY - r * 0.45f); path.lineTo(pauseX + r * 0.5f, btnY); path.lineTo(pauseX - r * 0.3f, btnY + r * 0.45f); path.close()
                c.drawPath(path, paint)
            } else {
                c.drawRect(pauseX - r * 0.35f, btnY - r * 0.4f, pauseX - r * 0.1f, btnY + r * 0.4f, paint)
                c.drawRect(pauseX + r * 0.1f, btnY - r * 0.4f, pauseX + r * 0.35f, btnY + r * 0.4f, paint)
            }
        }
        paint.color = Color.argb(70, 0, 0, 0)
        c.drawCircle(soundX, btnY, r, paint)
        label(c, if (sound.enabled) "🔊" else "🔇", soundX, btnY + r * 0.35f, r * 1.0f)
    }

    private fun drawHud(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val y1 = h * 0.15f
        label(c, score.toString(), w / 2f, y1, u * 20f * (1f + 0.22f * scoreBump))
        label(c, "BEST $best", w / 2f, y1 + u * 7.5f, u * 4.2f, Color.WHITE, Paint.Align.CENTER, 200)
        var nextTxt = "You've reached them all!"
        for (lm in landmarks) if (lm.first > score) { nextTxt = "Next: ${lm.second}  (${lm.first - score} to go)"; break }
        label(c, nextTxt, w / 2f, y1 + u * 13.5f, u * 3.8f, Color.WHITE, Paint.Align.CENTER, 190)

        // combo pips
        val py = y1 + u * 18.5f
        for (i in 0 until 5) {
            paint.color = if (i < combo) Color.rgb(255, 214, 64) else Color.argb(80, 255, 255, 255)
            c.drawCircle(w / 2f + (i - 2) * u * 4.2f, py, u * 1.3f, paint)
        }
        // perfect pop
        if (popAge < 0.9f && popText.isNotEmpty()) {
            val t = popAge / 0.9f
            val sc = 1f + 0.5f * (1f - t) * (1f - t)
            label(c, popText, w / 2f, py + u * 10f, u * 8f * sc, Color.rgb(255, 232, 120), Paint.Align.CENTER, ((1f - t * t) * 255).toInt())
            if (popSub.isNotEmpty()) {
                label(c, popSub, w / 2f, py + u * 16f, u * 4.2f, Color.WHITE, Paint.Align.CENTER, ((1f - t * t) * 255).toInt())
            }
        }
        // landmark banner
        if (bannerAge < 2.2f) {
            val slide = min(1f, bannerAge / 0.25f)
            val fade = if (bannerAge > 1.7f) (2.2f - bannerAge) / 0.5f else 1f
            val by = h * 0.075f + (1f - slide) * -u * 10f
            paint.color = Color.argb((140 * fade).toInt(), 0, 0, 0)
            rect.set(w * 0.12f, by - u * 4.5f, w * 0.88f, by + u * 4f)
            c.drawRoundRect(rect, u * 4f, u * 4f, paint)
            label(c, bannerText, w / 2f, by + u * 1.4f, u * 4.6f, Color.WHITE, Paint.Align.CENTER, (255 * fade).toInt())
        }
        // first-time hint
        if (plays == 0 && score < 3 && (state == State.PLAYING || state == State.FALLING)) {
            label(c, "Tap when the block lines up!", w / 2f, h * 0.92f, u * 4.6f, Color.WHITE, Paint.Align.CENTER,
                (150 + 100 * sin(time * 4f)).toInt())
        }
    }

    private fun drawMenu(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val bob = sin(time * 2f) * u * 1.2f
        val ty = h * 0.2f + bob
        text.shader = LinearGradient(0f, ty - u * 14f, 0f, ty + u * 18f, Color.rgb(255, 240, 150), Color.rgb(255, 140, 60), Shader.TileMode.CLAMP)
        label(c, "STACK", w / 2f, ty, u * 22f)
        label(c, "TOWER", w / 2f, ty + u * 20f, u * 22f)
        text.shader = null
        label(c, "Tap to drop. Line it up for PERFECT combos!", w / 2f, ty + u * 31f, u * 4.2f, Color.WHITE, Paint.Align.CENTER, 230)
        if (best > 0) label(c, "BEST  $best", w / 2f, ty + u * 40f, u * 6f, Color.rgb(255, 232, 120))
        val pulse = 0.5f + 0.5f * sin(time * 4f)
        paint.color = Color.argb((90 + 70 * pulse).toInt(), 255, 255, 255)
        rect.set(w * 0.22f, h * 0.9f - u * 7f, w * 0.78f, h * 0.9f + u * 3f)
        c.drawRoundRect(rect, u * 5f, u * 5f, paint)
        label(c, "TAP TO START", w / 2f, h * 0.9f, u * 6.2f, Color.rgb(40, 50, 90))
        label(c, tip, w / 2f, h * 0.9f + u * 9f, u * 3.4f, Color.WHITE, Paint.Align.CENTER, 200)
    }

    private fun drawPaused(c: Canvas) {
        paint.color = Color.argb(150, 0, 0, 0)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        label(c, "PAUSED", width / 2f, height * 0.45f, u * 14f)
        label(c, "Tap to resume", width / 2f, height * 0.45f + u * 10f, u * 5f, Color.WHITE, Paint.Align.CENTER, 220)
    }

    private fun drawOver(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = Color.argb((min(1f, overTime / 0.4f) * 110).toInt(), 0, 0, 0)
        c.drawRect(0f, 0f, w, h, paint)

        val t = (overTime / 0.45f).coerceIn(0f, 1f)
        val ease = 1f + 2.2f * (t - 1f) * (t - 1f) * (t - 1f) + 1.2f * (t - 1f) * (t - 1f) // easeOutBack-ish
        val cardH = min(h * 0.45f, u * 100f)
        val top = h * 0.5f
        val cx = w / 2f
        c.save()
        c.scale(ease, ease, cx, top + cardH / 2f)
        paint.color = Color.argb(235, 24, 30, 60)
        rect.set(u * 6f, top, u * 94f, top + cardH)
        c.drawRoundRect(rect, u * 6f, u * 6f, paint)
        stroke.strokeWidth = u * 0.6f
        stroke.color = Color.argb(90, 255, 255, 255)
        c.drawRoundRect(rect, u * 6f, u * 6f, stroke)

        label(c, "GAME OVER", cx, top + u * 12f, u * 9f)
        label(c, score.toString(), cx, top + u * 34f, u * 21f, Color.rgb(255, 232, 120))
        if (newBest) {
            label(c, "★ NEW BEST! ★", cx, top + u * 43f, u * 6f, Color.rgb(255, 214, 64), Paint.Align.CENTER,
                (200 + 55 * sin(time * 6f)).toInt())
        } else {
            label(c, "BEST  $best", cx, top + u * 43f, u * 5.5f, Color.WHITE, Paint.Align.CENTER, 220)
        }
        label(c, "Best combo: x$maxCombo", cx, top + u * 51f, u * 4.2f, Color.WHITE, Paint.Align.CENTER, 200)
        val reached = if (nextLm > 0) "Reached: " + landmarks[nextLm - 1].second else "Reach the Giraffe next!"
        label(c, reached, cx, top + u * 58f, u * 4.2f, Color.WHITE, Paint.Align.CENTER, 200)

        if (overTime > 0.6f) {
            val pulse = 0.5f + 0.5f * sin(time * 5f)
            paint.color = Color.rgb(255, (180 + 40 * pulse).toInt(), 60)
            rect.set(u * 18f, top + cardH - u * 17f, u * 82f, top + cardH - u * 5f)
            c.drawRoundRect(rect, u * 6f, u * 6f, paint)
            label(c, "PLAY AGAIN", cx, top + cardH - u * 9f, u * 6.4f, Color.rgb(50, 30, 10))
        }
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        if (width == 0) return
        drawBackground(c)
        c.save()
        if (shake > 0f) c.translate((rnd.nextFloat() - 0.5f) * shake * bh * 0.4f, (rnd.nextFloat() - 0.5f) * shake * bh * 0.4f)
        c.translate(0f, -shift)
        c.scale(zoom, zoom, width / 2f, baseY)
        drawWorld(c)
        c.restore()

        if (flash > 0f) {
            paint.color = Color.argb((flash * 140).toInt().coerceIn(0, 255), 255, 255, 255)
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        vignette?.let { paint.shader = it; c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint); paint.shader = null }

        when (state) {
            State.MENU -> drawMenu(c)
            State.OVER -> drawOver(c)
            else -> drawHud(c)
        }
        drawButtons(c)
        if (state == State.PAUSED) drawPaused(c)
    }

    // ---------- input ----------
    private fun hit(x: Float, y: Float, bx: Float, by: Float) = hypot(x - bx, y - by) < btnR * 1.5f

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return true
        val x = e.x
        val y = e.y
        if (hit(x, y, soundX, btnY)) {
            sound.enabled = !sound.enabled
            prefs.edit().putBoolean("sound", sound.enabled).apply()
            sound.play("tick")
            return true
        }
        if ((state == State.PLAYING) && hit(x, y, pauseX, btnY)) {
            state = State.PAUSED
            return true
        }
        when (state) {
            State.MENU -> state = State.PLAYING
            State.PLAYING -> startDrop()
            State.PAUSED -> { state = State.PLAYING; lastNs = 0L }
            State.OVER -> if (overTime > 0.6f) reset(true)
            State.FALLING -> {}
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (w == 0 || h == 0) return
        stars.clear(); clouds.clear(); birds.clear()
        repeat(110) { stars.add(Star(rnd.nextFloat(), rnd.nextFloat(), 1f + rnd.nextFloat() * 2.2f, rnd.nextFloat() * TWO_PI)) }
        repeat(9) {
            clouds.add(Cloud(rnd.nextFloat() * w, h * (0.04f + rnd.nextFloat() * 0.7f), w * (0.05f + rnd.nextFloat() * 0.06f), 6f + rnd.nextFloat() * 14f))
        }
        repeat(4) {
            birds.add(Bird(rnd.nextFloat() * w, h * (0.1f + rnd.nextFloat() * 0.4f), 40f + rnd.nextFloat() * 40f, rnd.nextFloat() * 6f))
        }
        vignette = RadialGradient(w / 2f, h / 2f, h * 0.75f,
            intArrayOf(Color.TRANSPARENT, Color.argb(90, 0, 0, 0)), floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP)
        reset(false)
    }

    // ---------- loop ----------
    override fun doFrame(ns: Long) {
        if (!running) return
        val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1e9f).coerceIn(0.001f, 0.033f)
        lastNs = ns
        if (width > 0) update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun resumeGame() {
        running = true; lastNs = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun pauseGame() {
        running = false
        if (state == State.PLAYING) state = State.PAUSED
    }

    fun release() { sound.release() }
}
