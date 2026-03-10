package org.sternbach.software.julesmobileapp.ui.helper

import org.sternbach.software.julesmobileapp.Session

sealed class Screen {
    object SessionList : Screen()
    data class SessionDetail(val session: Session) : Screen()
}
