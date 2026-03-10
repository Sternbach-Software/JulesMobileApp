package org.sternbach.software.julesmobileapp.ui.helper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.ui.composable.AppState
import org.sternbach.software.julesmobileapp.Session
import org.sternbach.software.julesmobileapp.Source

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
    onCreateSession: (String, String, Source?, String, Boolean, (Session) -> Unit) -> Unit
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
                    Card(modifier = Modifier.fillMaxWidth().clickable { onSessionSelected(session) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = session.title ?: "No Title", style = MaterialTheme.typography.titleMedium)
                            Text(text = "ID: ${session.id}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = session.prompt, style = MaterialTheme.typography.bodyMedium)
                            if (session.state != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "State: ${session.state}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (session.sourceContext != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Source: ${session.sourceContext.source}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
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
