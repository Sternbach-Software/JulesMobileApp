package org.sternbach.software.julesmobileapp

expect object CacheManager {
    // Generic methods
    fun readPreference(key: String): String?
    fun writePreference(key: String, value: String?)
    fun readBooleanPreference(key: String, defaultValue: Boolean): Boolean
    fun writeBooleanPreference(key: String, value: Boolean)
}