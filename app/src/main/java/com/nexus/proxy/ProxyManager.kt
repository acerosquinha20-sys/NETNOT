package com.nexus.proxy

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** Proxy HTTP global do Android. O status só fica ATIVO se o proxy realmente estiver configurado no sistema. */
object ProxyManager {
    const val PERM = "android.permission.WRITE_SECURE_SETTINGS"
    fun hasPerm(c: Context) = c.checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED
    fun current(c: Context): String? = Settings.Global.getString(c.contentResolver, "http_proxy")
    fun isActive(c: Context): Boolean { val v = current(c); return !v.isNullOrEmpty() && v != ":0" }
    fun set(c: Context, host: String, port: String): Boolean =
        try { Settings.Global.putString(c.contentResolver, "http_proxy", "$host:$port") } catch (e: Exception) { false }
    fun clear(c: Context): Boolean =
        try { Settings.Global.putString(c.contentResolver, "http_proxy", ":0") } catch (e: Exception) { false }
}
