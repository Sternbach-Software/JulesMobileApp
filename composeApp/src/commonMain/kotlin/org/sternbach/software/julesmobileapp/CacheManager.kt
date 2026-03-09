package org.sternbach.software.julesmobileapp

expect object CacheManager {
    fun readCache(): String?
    fun writeCache(json: String)
    fun readApiKey(): String?
    fun writeApiKey(key: String)
}