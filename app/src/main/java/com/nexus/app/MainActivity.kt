package com.nexus.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.graphics.Color
import android.view.ViewGroup
import java.util.Locale

class MainActivity : android.app.Activity() {
    private lateinit var ip: EditText
    private lateinit var port: EditText
    private lateinit var status: TextView
    private lateinit var players: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(9,9,9))
        }

        fun label(text: String, size: Float = 15f): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 8)
            }

        root.addView(label("NEXUS", 30f))
        root.addView(label("Proxy + Overlay"))

        ip = EditText(this).apply {
            hint = "IP do servidor/proxy"
            setText("192.168.0.100")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine()
        }
        root.addView(ip, LinearLayout.LayoutParams(-1, 58))

        port = EditText(this).apply {
            hint = "Porta"
            setText("8080")
            inputType = 2
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine()
        }
        root.addView(port, LinearLayout.LayoutParams(-1, 58))

        val debug = Switch(this).apply {
            text = "Modo depuração Wi‑Fi"
            setTextColor(Color.WHITE)
            isChecked = true
        }
        root.addView(debug)

        val test = Button(this).apply { text = "TESTAR CONEXÃO" }
        root.addView(test)

        status = label("● DESCONectado")
        root.addView(status)

        players = label("PLAYERS: 0", 26f)
        root.addView(players)

        val overlay = Button(this).apply { text = "ATIVAR FLUTUANTE" }
        root.addView(overlay)

        val mock = Button(this).apply { text = "SIMULAR 42 PLAYERS" }
        root.addView(mock)

        val note = label(
            "O Nexus usa uma API própria/autorizada para receber dados. " +
            "O app não intercepta nem lê a memória do jogo.",
            13f
        )
        note.setTextColor(Color.LTGRAY)
        root.addView(note)

        setContentView(root)

        test.setOnClickListener {
            status.text = "● CONFIGURADO: ${ip.text}:${port.text}"
            status.setTextColor(Color.rgb(255, 80, 80))
        }

        mock.setOnClickListener {
            players.text = "PLAYERS: 42"
            getSharedPreferences("nexus", MODE_PRIVATE)
                .edit().putInt("players", 42).apply()
        }

        overlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            } else {
                startService(Intent(this, OverlayService::class.java))
            }
        }
    }
}
