package org.sternbach.software.julesmobileapp

import android.content.Context
import android.content.SharedPreferences

actual object CacheManager {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val prefs: SharedPreferences?
        get() = appContext?.getSharedPreferences("jules_cache", Context.MODE_PRIVATE)

    actual fun readCache(): String? {
        return prefs?.getString("sessions_cache", null)
    }

    actual fun writeCache(json: String) {
        prefs?.edit()?.putString("sessions_cache", json)?.apply()
    }

    actual fun readApiKey(): String? {
        return prefs?.getString("api_key", null)
    }

    actual fun writeApiKey(key: String) {
        prefs?.edit()?.putString("api_key", key)?.apply()
    }
}