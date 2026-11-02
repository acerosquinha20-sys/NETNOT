package com.nexus.proxy

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.TextView

object Ui {
    val RED = Color.parseColor("#FF1A1A")
    val GREEN = Color.parseColor("#00E676")
    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    /** Caixa 3D neon: brilho externo + degradê + borda vermelha, pontas arredondadas */
    fun neon(c: Context, radiusDp: Int, top: Int = Color.parseColor("#3A0000"), bottom: Int = Color.BLACK): LayerDrawable {
        val r = dp(c, radiusDp).toFloat()
        val glow = GradientDrawable().apply {
            cornerRadius = r + dp(c, 3); setColor(Color.TRANSPARENT)
            setStroke(dp(c, 5), Color.parseColor("#55FF1A1A"))
        }
        val body = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)).apply {
            cornerRadius = r; setStroke(dp(c, 2), RED)
        }
        return LayerDrawable(arrayOf(glow, body)).apply { val i = dp(c, 3); setLayerInset(1, i, i, i, i) }
    }

    /** Letras estilo iPhone (sans-serif medium, espaçamento leve) */
    fun text(c: Context, t: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(c).apply {
        text = t; textSize = size; setTextColor(color)
        typeface = Typeface.create("sans-serif-medium", if (bold) Typeface.BOLD else Typeface.NORMAL)
        letterSpacing = 0.02f
    }

    fun button(c: Context, t: String): Button = Button(c).apply {
        text = t; setTextColor(Color.WHITE); isAllCaps = true; gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        background = neon(c, 14, Color.parseColor("#B00000"), Color.parseColor("#2A0000"))
        setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6))
    }
}
