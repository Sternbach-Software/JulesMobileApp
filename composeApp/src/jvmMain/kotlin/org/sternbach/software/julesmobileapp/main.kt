package org.sternbach.software.julesmobileapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.sternbach.software.julesmobileapp.ui.helper.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "JulesMobileApp",
    ) {
        App()
    }
}