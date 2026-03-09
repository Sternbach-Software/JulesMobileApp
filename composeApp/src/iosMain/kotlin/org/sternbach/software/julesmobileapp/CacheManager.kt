package org.sternbach.software.julesmobileapp

import platform.Foundation.NSUserDefaults

actual object CacheManager {
    private val defaults = NSUserDefaults.standardUserDefaults()
    private const val CACHE_KEY = "jules_sessions_cache"

    actual fun readCache(): String? {
        return defaults.stringForKey(CACHE_KEY)
    }

    actual fun writeCache(json: String) {
        defaults.setObject(json, forKey = CACHE_KEY)
    }

    actual fun readApiKey(): String? {
        return defaults.stringForKey("jules_api_key")
    }

    actual fun writeApiKey(key: String) {
        defaults.setObject(key, forKey = "jules_api_key")
    }
}