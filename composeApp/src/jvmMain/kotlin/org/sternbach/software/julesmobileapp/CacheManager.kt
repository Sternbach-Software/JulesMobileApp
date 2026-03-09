package org.sternbach.software.julesmobileapp

import java.io.File

actual object CacheManager {
    private val cacheFile = File(System.getProperty("user.home"), ".jules_cache.json")

    actual fun readCache(): String? {
        return if (cacheFile.exists()) {
            try {
                cacheFile.readText()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    actual fun writeCache(json: String) {
        try {
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val apiFile = File(System.getProperty("user.home"), ".jules_api_key")

    actual fun readApiKey(): String? {
        return if (apiFile.exists()) {
            try {
                apiFile.readText()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    actual fun writeApiKey(key: String) {
        try {
            apiFile.writeText(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}