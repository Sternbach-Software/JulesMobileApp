package org.sternbach.software.julesmobileapp

import java.io.File

actual object CacheManager {
    actual fun readPreference(key: String): String? {
        val file = File(System.getProperty("user.home"), ".jules_$key")
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    actual fun writePreference(key: String, value: String?) {
        val file = File(System.getProperty("user.home"), ".jules_$key")
        if (value == null) {
            file.delete()
        } else {
            try {
                file.writeText(value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun readBooleanPreference(key: String, defaultValue: Boolean): Boolean {
        val str = readPreference(key)
        return str?.toBoolean() ?: defaultValue
    }

    actual fun writeBooleanPreference(key: String, value: Boolean) {
        writePreference(key, value.toString())
    }
}