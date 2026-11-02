package com.nexus.proxy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import java.util.Random

/** Fundo: partículas vermelhas vivas ligadas por linhas quando se aproximam */
class ParticleView(c: Context) : View(c) {
    private class P(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float)
    private val ps = ArrayList<P>()
    private val rnd = Random()
    private val dens = c.resources.displayMetrics.density
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF2A2A") }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#44FF1A1A") }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.5f * dens }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        ps.clear()
        repeat(60) {
            ps.add(P(rnd.nextFloat() * w, rnd.nextFloat() * h,
                (rnd.nextFloat() - .5f) * 2f * dens, (rnd.nextFloat() - .5f) * 2f * dens,
                (1.5f + rnd.nextFloat() * 2.5f) * dens))
        }
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.BLACK)
        val maxD = 120f * dens
        for (p in ps) {
            p.x += p.vx; p.y += p.vy
            if (p.x < 0 || p.x > width) p.vx = -p.vx
            if (p.y < 0 || p.y > height) p.vy = -p.vy
        }
        for (i in ps.indices) for (j in i + 1 until ps.size) {
            val d = Math.hypot((ps[i].x - ps[j].x).toDouble(), (ps[i].y - ps[j].y).toDouble()).toFloat()
            if (d < maxD) {
                line.color = Color.argb(((1 - d / maxD) * 150).toInt(), 255, 26, 26)
                c.drawLine(ps[i].x, ps[i].y, ps[j].x, ps[j].y, line)
            }
        }
        for (p in ps) { c.drawCircle(p.x, p.y, p.r * 2.6f, glow); c.drawCircle(p.x, p.y, p.r, dot) }
        postInvalidateOnAnimation()
    }
}
