package org.sternbach.software.julesmobileapp

import kotlinx.serialization.json.Json

val jsonFormat = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}
