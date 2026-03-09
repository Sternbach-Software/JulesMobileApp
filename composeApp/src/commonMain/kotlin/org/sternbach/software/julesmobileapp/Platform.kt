package org.sternbach.software.julesmobileapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform