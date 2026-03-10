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

    // Generic methods implementation
    actual fun readPreference(key: String): String? {
        return prefs?.getString(key, null)
    }

    actual fun writePreference(key: String, value: String?) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun readBooleanPreference(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: defaultValue
    }

    actual fun writeBooleanPreference(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }
}