package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sternbach.software.julesmobileapp.ui.helper.JulesViewModel
import org.sternbach.software.julesmobileapp.ui.helper.Screen

@Composable
fun App() {
    val viewModel = viewModel { JulesViewModel() }
    val state by viewModel.state.collectAsState()

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (val screen = state.currentScreen) {
                is Screen.SessionList -> {
                    SessionListScreen(
                        state = state,
                        onApiKeyChange = { viewModel.setApiKey(it) },
                        onFetchSessions = { viewModel.fetchSessions(state.apiKey) },
                        onSessionSelected = { viewModel.navigateToSessionDetail(it) },
                        onLoadSources = { viewModel.loadSources(reset = true) },
                        onSetSourceSearchQuery = { viewModel.setSourceSearchQuery(it) },
                        onToggleSourceSearchMode = { viewModel.toggleSourceSearchMode(it) },
                        onLoadMoreSources = { viewModel.loadSources(reset = false) },
                        onCreateSession = { prompt, title, source, startingBranch, requireApproval, onSuccess ->
                            viewModel.createSession(prompt, title, source, startingBranch, requireApproval, onSuccess)
                        }
                    )
                }
                is Screen.SessionDetail -> {
                    SessionDetailScreen(
                        session = screen.session,
                        state = state,
                        onApprovePlan = { viewModel.approvePlan(screen.session.id) },
                        onSendMessage = { sessionId, msg, onSent -> viewModel.sendMessage(sessionId, msg, onSent) },
                        onFetchActivity = { sessionId, activityId -> viewModel.fetchActivity(sessionId, activityId) },
                        onBack = { viewModel.navigateToSessionList() }
                    )
                }
            }
        }
    }
}
