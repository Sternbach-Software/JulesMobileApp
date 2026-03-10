package org.sternbach.software.julesmobileapp

import java.io.File

actual object CacheManager {
    private fun getFile(key: String): File {
        return File(System.getProperty("user.home"), ".jules_pref_$key")
    }

    actual fun readPreference(key: String): String? {
        val file = getFile(key)
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
        val file = getFile(key)
        try {
            if (value == null) {
                if (file.exists()) {
                    file.delete()
                }
            } else {
                file.writeText(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun readBooleanPreference(key: String, defaultValue: Boolean): Boolean {
        val str = readPreference(key)
        return str?.toBooleanStrictOrNull() ?: defaultValue
    }

    actual fun writeBooleanPreference(key: String, value: Boolean) {
        writePreference(key, value.toString())
    }
}