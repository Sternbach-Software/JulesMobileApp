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
import org.sternbach.software.julesmobileapp.ui.diff.DiffFile
import org.sternbach.software.julesmobileapp.ui.diff.DiffParser
import org.sternbach.software.julesmobileapp.ui.diff.DiffViewer
import org.sternbach.software.julesmobileapp.ui.helper.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: Session,
    state: AppState,
    onApprovePlan: () -> Unit,
    onSendMessage: (String, String, () -> Unit) -> Unit,
    onFetchActivity: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var isReviewMode by remember { mutableStateOf(false) }

    val diffFiles = remember(state.activities) {
        val latestDiffs = mutableMapOf<String, DiffFile>()
        state.activities.forEach { activity ->
            activity.artifacts?.forEach { artifact ->
                artifact.changeSet?.gitPatch?.unidiffPatch?.let { diff ->
                    val parsedFiles = DiffParser.parse(diff)
                    parsedFiles.forEach { file ->
                        val key = if (file.newName == "/dev/null") file.oldName else file.newName
                        latestDiffs[key] = file
                    }
                }
            }
        }
        latestDiffs.values.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.title ?: "Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { isReviewMode = !isReviewMode }) {
                        Text(if (isReviewMode) "Chat" else "Code")
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

            if (isReviewMode) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DiffViewer(diffFiles)
                }
            } else {
                if (state.isLoadingActivities) {
                    CollapsibleText(session.prompt, style = MaterialTheme.typography.bodyLarge, clickable = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            CollapsibleText(session.prompt, style = MaterialTheme.typography.bodyLarge, clickable = true)
                        }
                        items(state.activities) { activity ->
                            ActivityCard(activity, onFetchActivity, session)
                        }
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
