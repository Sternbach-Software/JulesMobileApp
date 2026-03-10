package org.sternbach.software.julesmobileapp

import platform.Foundation.NSUserDefaults

actual object CacheManager {
    private val defaults = NSUserDefaults.standardUserDefaults()

    // Generic methods implementation
    actual fun readPreference(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun writePreference(key: String, value: String?) {
        defaults.setObject(value, forKey = key)
    }

    actual fun readBooleanPreference(key: String, defaultValue: Boolean): Boolean {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.boolForKey(key)
    }

    actual fun writeBooleanPreference(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}