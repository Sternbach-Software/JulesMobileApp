package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.Session
import org.sternbach.software.julesmobileapp.Source
import org.sternbach.software.julesmobileapp.ui.helper.AppState

@Composable
fun SessionListScreen(
    state: AppState,
    onApiKeyChange: (String) -> Unit,
    onFetchSessions: () -> Unit,
    onSessionSelected: (Session) -> Unit,
    onLoadSources: () -> Unit,
    onSetSourceSearchQuery: (String) -> Unit,
    onToggleSourceSearchMode: (Boolean) -> Unit,
    onLoadMoreSources: () -> Unit,
    onCreateSession: (String, String, Source?, String, Boolean, (Session) -> Unit) -> Unit,
    onTogglePeriodicSessionUpdate: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredSessions = state.sessions.filter { session ->
        if (searchQuery.isBlank()) return@filter true
        val query = searchQuery.lowercase()
        session.name.lowercase().contains(query) ||
                (session.title?.lowercase()?.contains(query) == true) ||
                (session.prompt.lowercase().contains(query))
    }

    Scaffold(
        floatingActionButton = {
            if (state.apiKey.isNotBlank()) {
                FloatingActionButton(onClick = {
                    onLoadSources()
                    showCreateDialog = true
                }) {
                    Text("+")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = state.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Jules API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto Update", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = state.isPeriodicSessionUpdateEnabled,
                        onCheckedChange = onTogglePeriodicSessionUpdate
                    )
                }

                if (state.isLoadingSessions) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }

            Button(
                onClick = onFetchSessions,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingSessions
            ) {
                Text(if (state.isLoadingSessions) "Fetching..." else "Fetch Sessions")
            }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Sessions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.sessionsError != null) {
                Text(text = state.sessionsError, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSessions) { session ->
                    SessionCard(onSessionSelected, session)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSessionDialog(
            state = state,
            onDismiss = { showCreateDialog = false },
            onSetSourceSearchQuery = onSetSourceSearchQuery,
            onToggleSourceSearchMode = onToggleSourceSearchMode,
            onLoadMoreSources = onLoadMoreSources,
            onCreateSession = { prompt, title, source, startingBranch, requireApproval ->
                onCreateSession(prompt, title, source, startingBranch, requireApproval) { session ->
                    showCreateDialog = false
                    onSessionSelected(session)
                }
            }
        )
    }
}

