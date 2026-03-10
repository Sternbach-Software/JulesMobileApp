package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.Session
import org.sternbach.software.julesmobileapp.ui.helper.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: Session,
    state: AppState,
    onApprovePlan: () -> Unit,
    onSendMessage: (String, String, () -> Unit) -> Unit,
    onFetchActivity: (String, String) -> Unit,
    onBack: () -> Unit,
    onTogglePeriodicActivityUpdate: (Boolean, String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.title ?: "Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto Update", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = state.isPeriodicActivityUpdateEnabled,
                        onCheckedChange = { onTogglePeriodicActivityUpdate(it, session.id) }
                    )
                }

                if (state.isLoadingActivities) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }

            Text(text = "ID: ${session.id}", style = MaterialTheme.typography.bodySmall)

            if (state.needsPlanApproval) {
                Button(
                    onClick = onApprovePlan,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isApprovingPlan
                ) {
                    Text(if (state.isApprovingPlan) "Approving..." else "Approve Plan")
                }
            }

            if (state.activitiesError != null) {
                Text(text = state.activitiesError, color = MaterialTheme.colorScheme.error)
            }
            if (state.approvePlanError != null) {
                Text(text = state.approvePlanError, color = MaterialTheme.colorScheme.error)
            }
            if (state.sendMessageError != null) {
                Text(text = state.sendMessageError, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CollapsibleText(session.prompt, style = MaterialTheme.typography.bodyLarge, clickable = true)
                }
                if (state.activities.isEmpty() && state.isLoadingActivities) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    items(state.activities) { activity ->
                        ActivityCard(activity, onFetchActivity, session)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Send a message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isBlank()) return@IconButton
                        onSendMessage(session.id, messageText) {
                            messageText = ""
                        }
                    },
                    enabled = !state.isSendingMessage && messageText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, ("Send"))
                }
            }
        }
    }
}
