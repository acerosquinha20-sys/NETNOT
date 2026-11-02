package com.nexus.app

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.max

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var view: LinearLayout

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val pad = 22
        view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.rgb(18,18,18))
        }

        fun tv(text: String, size: Float): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.WHITE)
                setPadding(0, 5, 0, 5)
            }

        view.addView(tv("PLAYERS: 42", 20f))
        view.addView(tv("● CONECTADO", 12f))
        view.addView(tv("PLAYER 01     18.4 m", 13f))
        view.addView(tv("PLAYER 02     31.7 m", 13f))
        view.addView(tv("PLAYER 03     46.2 m", 13f))
        view.addView(tv("ESP: ON    DISTÂNCIA: ON", 12f))

        val close = Button(this).apply { text = "FECHAR" }
        view.addView(close)
        close.setOnClickListener { stopSelf() }

        val type = if (android.os.Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            330, WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 100
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startX = 0
            var startY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.rawX; downY = e.rawY
                        startX = params.x; startY = params.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX - (e.rawX - downX).toInt()
                        params.y = startY + (e.rawY - downY).toInt()
                        wm.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(view, params)
    }

    override fun onDestroy() {
        if (::view.isInitialized) wm.removeView(view)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
