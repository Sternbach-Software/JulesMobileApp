package org.sternbach.software.julesmobileapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val jsonFormat = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

sealed class Screen {
    object SessionList : Screen()
    data class SessionDetail(val session: Session) : Screen()
}

@Composable
fun App() {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { JulesViewModel() }
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
                        onLoadSources = { viewModel.loadSources() },
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
                        onBack = { viewModel.navigateToSessionList() }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionListScreen(
    state: AppState,
    onApiKeyChange: (String) -> Unit,
    onFetchSessions: () -> Unit,
    onSessionSelected: (Session) -> Unit,
    onLoadSources: () -> Unit,
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
            onCreateSession = { prompt, title, source, startingBranch, requireApproval ->
                onCreateSession(prompt, title, source, startingBranch, requireApproval) { session ->
                    showCreateDialog = false
                    onSessionSelected(session)
                }
            }
        )
    }
}

@Composable
fun CreateSessionDialog(
    state: AppState,
    onDismiss: () -> Unit,
    onCreateSession: (String, String, Source?, String, Boolean) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var requirePlanApproval by remember { mutableStateOf(true) }
    var selectedSource by remember { mutableStateOf<Source?>(null) }
    var startingBranch by remember { mutableStateOf("main") }
    var sourceSearchQuery by remember { mutableStateOf("") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    @OptIn(ExperimentalMaterial3Api::class)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.sourcesError != null) {
                    Text(text = state.sourcesError, color = MaterialTheme.colorScheme.error)
                }
                if (state.createSessionError != null) {
                    Text(text = state.createSessionError, color = MaterialTheme.colorScheme.error)
                }

                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt (Required)") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (state.isLoadingSources) {
                    Text("Loading sources...")
                } else if (state.sources.isEmpty()) {
                    Text("No sources available.")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = sourceDropdownExpanded,
                        onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = sourceSearchQuery,
                            onValueChange = {
                                sourceSearchQuery = it
                                sourceDropdownExpanded = true
                                if (selectedSource != null && it != selectedSource?.name) {
                                    selectedSource = null
                                }
                            },
                            label = { Text("Source") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        val filteredSources = state.sources.filter { it.name.contains(sourceSearchQuery, ignoreCase = true) }
                        if (filteredSources.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = sourceDropdownExpanded,
                                onDismissRequest = { sourceDropdownExpanded = false }
                            ) {
                                filteredSources.forEach { source ->
                                    DropdownMenuItem(
                                        text = { Text(source.name) },
                                        onClick = {
                                            selectedSource = source
                                            sourceSearchQuery = source.name
                                            sourceDropdownExpanded = false
                                            startingBranch = source.githubRepo?.defaultBranch?.displayName ?: "main"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedSource != null) {
                        var branchDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = branchDropdownExpanded,
                            onExpandedChange = { branchDropdownExpanded = !branchDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = startingBranch,
                                onValueChange = {
                                    startingBranch = it
                                    branchDropdownExpanded = true
                                },
                                label = { Text("Starting Branch") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors()
                            )
                            val branches = selectedSource?.githubRepo?.branches ?: emptyList()
                            val filteredBranches = branches.filter { it.displayName.contains(startingBranch, ignoreCase = true) }
                            if (filteredBranches.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = branchDropdownExpanded,
                                    onDismissRequest = { branchDropdownExpanded = false }
                                ) {
                                    filteredBranches.forEach { branch ->
                                        DropdownMenuItem(
                                            text = { Text(branch.displayName) },
                                            onClick = {
                                                startingBranch = branch.displayName
                                                branchDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        TextField(
                            value = startingBranch,
                            onValueChange = { startingBranch = it },
                            label = { Text("Starting Branch") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = false
                        )
                    }
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = requirePlanApproval,
                        onCheckedChange = { requirePlanApproval = it }
                    )
                    Text("Require Plan Approval")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        onCreateSession(prompt, title, selectedSource, startingBranch, requirePlanApproval)
                    }
                },
                enabled = !state.isCreatingSession && !state.isLoadingSources && prompt.isNotBlank()
            ) {
                Text(if (state.isCreatingSession) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: Session,
    state: AppState,
    onApprovePlan: () -> Unit,
    onSendMessage: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit
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
            Text(text = "ID: ${session.id}", style = MaterialTheme.typography.bodySmall)
            Text(text = session.prompt, style = MaterialTheme.typography.bodyLarge)

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

            if (state.isLoadingActivities) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.activities) { activity ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = activity.originator ?: "Unknown",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Render content based on type
                                if (activity.description != null) {
                                    Text(activity.description)
                                }
                                activity.userMessaged?.userMessage?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                                activity.agentMessaged?.agentMessage?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                                activity.planGenerated?.plan?.steps?.let { steps ->
                                    Text("Plan Generated:", style = MaterialTheme.typography.titleSmall)
                                    steps.forEach { step ->
                                        Text("${step.index}: ${step.title}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                activity.progressUpdated?.let { progress ->
                                    Text("${progress.title}: ${progress.description}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (activity.sessionFailed != null) {
                                    activity.sessionFailed.reason?.let { reason ->
                                        Text("Session Failed: $reason", color = MaterialTheme.colorScheme.error)
                                    } ?: run {
                                        Text("Session Failed", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
