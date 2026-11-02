package com.nexus.proxy

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.view.*
import android.widget.*
import java.net.InetSocketAddress
import java.net.Socket

class OverlayService : Service() {
    companion object { @Volatile var running = false }

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var floatView: ImageView? = null
    private var panel: LinearLayout? = null
    private var content: LinearLayout? = null
    private var tabViews: List<TextView> = emptyList()
    private var bar: TextView? = null
    private var tab = 1
    private var homeProxy: TextView? = null
    private var homeGame: TextView? = null
    private var homePing: TextView? = null
    private var homeBtn: Button? = null
    private var currentPkg = ""
    private var lastQuery = 0L
    @Volatile private var pingMs = -1L
    private val games = setOf("com.dts.freefireth", "com.dts.freefiremax")

    private val poll = object : Runnable { override fun run() { checkForeground(); handler.postDelayed(this, 1500) } }
    private val pinger = object : Runnable {
        override fun run() {
            Thread { pingMs = measure(); handler.post { updateStatus() } }.start()
            handler.postDelayed(this, 2500)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("nexusproxy", MODE_PRIVATE)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("nx", "Nexus Proxy", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "nx").setContentTitle("Nexus Proxy ativo")
            .setSmallIcon(android.R.drawable.ic_menu_manage).build())
        buildFloat(); buildBar()
        handler.post(poll); handler.post(pinger)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        floatView?.let { runCatching { wm.removeView(it) } }
        panel?.let { runCatching { wm.removeView(it) } }
        bar?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    private fun lp(w: Int, h: Int, focusable: Boolean, touchable: Boolean = true) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) or
            (if (touchable) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT)

    // ---------- Flutuante organizado com o símbolo N ----------
    private fun buildFloat() {
        val size = Ui.dp(this, 50)
        val p = lp(size, size, false).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 300 }
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.logo_nexus); scaleType = ImageView.ScaleType.FIT_CENTER
            background = Ui.neon(this@OverlayService, 12, Color.BLACK, Color.BLACK)
            val pd = Ui.dp(this@OverlayService, 4); setPadding(pd, pd, pd, pd)
        }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        iv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = p.x; sy = p.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    if (moved) { p.x = sx + dx; p.y = sy + dy; wm.updateViewLayout(iv, p) }
                }
                MotionEvent.ACTION_UP -> if (!moved) togglePanel()
            }
            true
        }
        floatView = iv; wm.addView(iv, p)
    }

    // ---------- Barra no topo quando o Free Fire está aberto ----------
    private fun buildBar() {
        val tv = Ui.text(this, "", 11f, Color.WHITE, true).apply {
            background = Ui.neon(this@OverlayService, 8, Color.parseColor("#4A0000"), Color.BLACK)
            setShadowLayer(8f, 0f, 3f, Ui.RED)
            val h = Ui.dp(this@OverlayService, 12); val v = Ui.dp(this@OverlayService, 5); setPadding(h, v, h, v)
            visibility = View.GONE
        }
        val p = lp(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, false, false)
            .apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = Ui.dp(this@OverlayService, 24) }
        bar = tv; wm.addView(tv, p)
    }

    // ---------- Painel com 3 abas ----------
    private fun togglePanel() {
        if (panel != null) { runCatching { wm.removeView(panel) }; panel = null; return }
        val c = this
        val box = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL; background = Ui.neon(c, 24)
            val pd = Ui.dp(c, 14); setPadding(pd, pd, pd, pd)
        }
        val head = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(Ui.text(c, "NEXUS", 18f, Ui.RED, true).apply { letterSpacing = 0.25f; setShadowLayer(12f, 0f, 0f, Ui.RED) }, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(Ui.text(c, "✕", 18f, Color.WHITE, true).apply { setPadding(16, 0, 4, 0); setOnClickListener { togglePanel() } })
        box.addView(head)
        val tabs = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 10) }
        val names = listOf("⚙  Config", "⌂  Home", "ⓘ  Info")
        tabViews = names.mapIndexed { i, n ->
            Ui.text(c, n, 12f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER; setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
                setOnClickListener { showTab(i) }
            }.also { tabs.addView(it, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(3, 0, 3, 0) }) }
        }
        box.addView(tabs)
        content = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        box.addView(content, LinearLayout.LayoutParams(-1, -2))
        panel = box
        showTab(tab)
        val p = lp(Ui.dp(c, 320), WindowManager.LayoutParams.WRAP_CONTENT, true).apply {
            gravity = Gravity.CENTER; softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN }
        wm.addView(box, p)
    }

    private fun field(hint: String, value: String, number: Boolean): EditText = EditText(this).apply {
        this.hint = hint; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setText(value)
        if (number) inputType = InputType.TYPE_CLASS_NUMBER
        background = Ui.neon(this@OverlayService, 12, Color.parseColor("#1A0000"), Color.BLACK)
        val pd = Ui.dp(this@OverlayService, 10); setPadding(pd, pd, pd, pd)
    }

    private fun showTab(i: Int) {
        tab = i
        tabViews.forEachIndexed { k, t ->
            t.background = if (k == i) Ui.neon(this, 10, Color.parseColor("#B00000"), Color.parseColor("#2A0000")) else null
            t.setTextColor(if (k == i) Color.WHITE else Color.parseColor("#AA5555"))
        }
        val c = content ?: return
        c.removeAllViews(); homeProxy = null; homeGame = null; homePing = null; homeBtn = null
        val gap = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, Ui.dp(this@OverlayService, 10)) }
        when (i) {
            0 -> {
                c.addView(Ui.text(this, "Proxy (host e porta)", 13f, Color.LTGRAY), gap)
                val host = field("Host (ex: 192.168.0.10)", prefs.getString("host", "") ?: "", false)
                val port = field("Porta (ex: 8080)", prefs.getString("port", "") ?: "", true)
                c.addView(host, gap); c.addView(port, gap)
                c.addView(Ui.button(this, "Salvar").apply {
                    setOnClickListener {
                        prefs.edit().putString("host", host.text.toString().trim()).putString("port", port.text.toString().trim()).apply()
                        Toast.makeText(context, "Salvo", Toast.LENGTH_SHORT).show()
                    } }, gap)
                c.addView(Ui.text(this, "Permissão do sistema: " + if (ProxyManager.hasPerm(this)) "✔" else "✘ (veja no app)", 12f, Color.LTGRAY))
            }
            1 -> {
                homeProxy = Ui.text(this, "", 16f, Color.WHITE, true)
                homeGame = Ui.text(this, "", 14f, Color.LTGRAY)
                homePing = Ui.text(this, "", 14f, Color.LTGRAY)
                c.addView(homeProxy, gap); c.addView(homeGame, gap); c.addView(homePing, gap)
                homeBtn = Ui.button(this, "LIGAR PROXY").apply { setOnClickListener { toggleProxy() } }
                c.addView(homeBtn, gap)
                refreshHome()
            }
            else -> {
                c.addView(Ui.text(this, "Nexus Proxy 1.0", 16f, Color.WHITE, true), gap)
                c.addView(Ui.text(this, "Configura um proxy HTTP global no Android (host e porta da aba Config) e mostra uma barra no topo quando o Free Fire está aberto, com o ping medido em tempo real.\n\nO status só fica ATIVO quando o proxy está realmente configurado no sistema.", 13f, Color.LTGRAY))
            }
        }
    }

    private fun toggleProxy() {
        if (!ProxyManager.hasPerm(this)) { Toast.makeText(this, "Falta a permissão do sistema (root ou ADB)", Toast.LENGTH_LONG).show(); return }
        if (ProxyManager.isActive(this)) ProxyManager.clear(this)
        else {
            val h = prefs.getString("host", "") ?: ""; val p = prefs.getString("port", "") ?: ""
            if (h.isEmpty() || p.isEmpty()) { Toast.makeText(this, "Configure host e porta na aba Config", Toast.LENGTH_LONG).show(); return }
            if (!ProxyManager.set(this, h, p)) Toast.makeText(this, "Não foi possível aplicar o proxy", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }

    // ---------- Ping (conexão TCP) ----------
    private fun measure(): Long {
        var host = "1.1.1.1"; var port = 443
        if (ProxyManager.isActive(this)) {
            val s = ProxyManager.current(this) ?: ""
            val i = s.lastIndexOf(':')
            if (i > 0) { host = s.substring(0, i); port = s.substring(i + 1).toIntOrNull() ?: 443 }
        }
        return try {
            val t0 = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(host, port), 1500) }
            (System.nanoTime() - t0) / 1_000_000
        } catch (e: Exception) { -1L }
    }
    private fun pingText() = if (pingMs < 0) "-- ms" else "$pingMs ms"

    // ---------- Detecta Free Fire em primeiro plano ----------
    private fun checkForeground() {
        if (MainActivity.hasUsageAccess(this)) {
            val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val ev = usm.queryEvents(if (lastQuery == 0L) now - 3_600_000 else lastQuery, now)
            val e = UsageEvents.Event()
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e)
                @Suppress("DEPRECATION")
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) currentPkg = e.packageName
            }
            lastQuery = now
        }
        updateStatus()
    }

    private fun refreshHome() {
        val active = ProxyManager.isActive(this)
        homeProxy?.text = "Proxy: " + if (active) "ATIVO" else "INATIVO"
        homeProxy?.setTextColor(if (active) Ui.GREEN else Ui.RED)
        homeGame?.text = "Free Fire: " + if (currentPkg in games) "detectado" else "não detectado"
        homePing?.text = "Ping: " + pingText()
        homeBtn?.text = if (active) "DESLIGAR PROXY" else "LIGAR PROXY"
    }

    private fun updateStatus() {
        val inGame = currentPkg in games
        val active = ProxyManager.isActive(this)
        bar?.apply {
            text = "PROXY NETFLIX NEXUS " + (if (active) "ATIVO" else "INATIVO") + "  •  " + pingText()
            setTextColor(if (active) Color.WHITE else Color.parseColor("#FF8888"))
            visibility = if (inGame) View.VISIBLE else View.GONE
        }
        refreshHome()
    }
}
