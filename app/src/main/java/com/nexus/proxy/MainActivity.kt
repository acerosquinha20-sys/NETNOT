package com.nexus.proxy

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val KEY = "Netflix67"
    private lateinit var prefs: SharedPreferences
    private lateinit var holder: FrameLayout
    private var perms: TextView? = null
    private var toggle: Button? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("nexusproxy", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val root = FrameLayout(this)
        root.addView(ParticleView(this), FrameLayout.LayoutParams(-1, -1))
        holder = FrameLayout(this)
        root.addView(holder, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        if (prefs.getBoolean("ok", false)) showMain() else showKey()
    }

    private fun page() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(this@MainActivity, 24), Ui.dp(this@MainActivity, 40), Ui.dp(this@MainActivity, 24), Ui.dp(this@MainActivity, 24))
    }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = Ui.neon(this@MainActivity, 24)
        val p = Ui.dp(this@MainActivity, 18); setPadding(p, p, p, p)
    }
    private fun LinearLayout.addGap(v: android.view.View, h: Int = 12) = addView(v, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, Ui.dp(this@MainActivity, h)) })

    private fun show(col: LinearLayout) {
        holder.removeAllViews()
        holder.addView(ScrollView(this).apply { addView(col) })
    }

    private fun showKey() {
        val col = page()
        col.addView(ImageView(this).apply { setImageResource(R.drawable.logo_nexus) }, LinearLayout.LayoutParams(Ui.dp(this, 170), Ui.dp(this, 170)))
        col.addView(Ui.text(this, "NEXUS", 28f, Color.WHITE, true).apply { letterSpacing = 0.3f; gravity = Gravity.CENTER })
        col.addView(Ui.text(this, "Digite sua key para entrar", 14f, Color.LTGRAY).apply { gravity = Gravity.CENTER; setPadding(0, 6, 0, 24) })
        val c = card()
        val key = EditText(this).apply {
            hint = "KEY"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_TEXT
            gravity = Gravity.CENTER; background = Ui.neon(this@MainActivity, 14, Color.parseColor("#1A0000"), Color.BLACK)
            setPadding(Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 12))
        }
        c.addGap(key)
        val enter = Ui.button(this, "ENTRAR").apply {
            setOnClickListener {
                if (key.text.toString().trim() == KEY) { prefs.edit().putBoolean("ok", true).apply(); showMain() }
                else Toast.makeText(context, "Key inválida", Toast.LENGTH_SHORT).show()
            }
        }
        c.addView(enter, LinearLayout.LayoutParams(-1, -2))
        col.addView(c, LinearLayout.LayoutParams(-1, -2))
        show(col)
    }

    private fun showMain() {
        val col = page()
        col.addView(ImageView(this).apply { setImageResource(R.drawable.logo_nexus) }, LinearLayout.LayoutParams(Ui.dp(this, 130), Ui.dp(this, 130)))
        col.addView(Ui.text(this, "NEXUS PROXY", 22f, Color.WHITE, true).apply { letterSpacing = 0.2f; gravity = Gravity.CENTER; setPadding(0, 6, 0, 18) })
        val c = card()
        perms = Ui.text(this, "", 13f, Color.WHITE).apply { setPadding(0, 0, 0, 14) }
        c.addView(perms)
        c.addGap(Ui.button(this, "1. Permitir sobreposição").apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } })
        c.addGap(Ui.button(this, "2. Permitir acesso de uso").apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } })
        c.addGap(Ui.button(this, "3. Permissão do sistema (root)").apply { setOnClickListener { grantRoot() } })
        c.addGap(Ui.button(this, "Copiar comando ADB (Termux)").apply {
            setOnClickListener {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText("adb", "adb shell pm grant $packageName ${ProxyManager.PERM}"))
                Toast.makeText(context, "Comando copiado", Toast.LENGTH_SHORT).show()
            } })
        toggle = Ui.button(this, "ATIVAR FLUTUANTE").apply {
            setOnClickListener {
                if (!Settings.canDrawOverlays(context)) Toast.makeText(context, "Permita a sobreposição primeiro", Toast.LENGTH_SHORT).show()
                else if (OverlayService.running) stopService(Intent(context, OverlayService::class.java))
                else startForegroundService(Intent(context, OverlayService::class.java))
                postDelayed({ refresh() }, 400)
            }
        }
        c.addView(toggle, LinearLayout.LayoutParams(-1, -2))
        col.addView(c, LinearLayout.LayoutParams(-1, -2))
        show(col); refresh()
    }

    private fun grantRoot() {
        Thread {
            val ok = try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "pm grant $packageName ${ProxyManager.PERM}")).waitFor() == 0
            } catch (e: Exception) { false }
            runOnUiThread {
                Toast.makeText(this, if (ok) "Permissão concedida" else "Root não disponível. Use o comando ADB.", Toast.LENGTH_LONG).show()
                refresh()
            }
        }.start()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        fun ok(v: Boolean) = if (v) "✔" else "✘"
        perms?.text = "Sobreposição: ${ok(Settings.canDrawOverlays(this))}\nAcesso de uso: ${ok(hasUsageAccess(this))}\nPermissão do sistema: ${ok(ProxyManager.hasPerm(this))}"
        toggle?.text = if (OverlayService.running) "DESATIVAR FLUTUANTE" else "ATIVAR FLUTUANTE"
    }

    companion object {
        fun hasUsageAccess(c: Context): Boolean {
            val ops = c.getSystemService(APP_OPS_SERVICE) as AppOpsManager
            return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) == AppOpsManager.MODE_ALLOWED
        }
    }
}
