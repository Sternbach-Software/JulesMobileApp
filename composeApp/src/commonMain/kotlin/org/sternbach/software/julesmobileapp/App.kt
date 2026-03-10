package org.sternbach.software.julesmobileapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items


import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.SessionList) }
        var apiKey by remember { mutableStateOf("") }

        // Read API key cache on startup
        LaunchedEffect(Unit) {
            try {
                val cachedKey = CacheManager.readApiKey()
                if (!cachedKey.isNullOrBlank()) {
                    apiKey = cachedKey
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (val screen = currentScreen) {
                is Screen.SessionList -> {
                    SessionListScreen(
                        apiKey = apiKey,
                        onApiKeyChange = {
                            apiKey = it
                            CacheManager.writeApiKey(it)
                        },
                        onSessionSelected = { session ->
                            currentScreen = Screen.SessionDetail(session)
                        }
                    )
                }
                is Screen.SessionDetail -> {
                    SessionDetailScreen(
                        session = screen.session,
                        apiKey = apiKey,
                        onBack = { currentScreen = Screen.SessionList }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionListScreen(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSessionSelected: (Session) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun fetchSessions(key: String) {
        if (key.isBlank()) return
        isLoading = true
        error = null
        try {
            val client = JulesClient(key)
            var nextPageToken: String? = null
            val allSessions = mutableListOf<Session>()

            do {
                val response = client.listSessions(pageSize = 50, pageToken = nextPageToken)
                if (response.sessions != null) {
                    allSessions.addAll(response.sessions)
                }
                nextPageToken = response.nextPageToken
            } while (nextPageToken != null)

            sessions = allSessions

            try {
                val jsonString = jsonFormat.encodeToString(allSessions)
                CacheManager.writeCache(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error = "Failed to fetch: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            val cached = CacheManager.readCache()
            if (cached != null) {
                val decoded = jsonFormat.decodeFromString<List<Session>>(cached)
                sessions = decoded
            }
            if (apiKey.isNotBlank()) {
                fetchSessions(apiKey)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val filteredSessions = sessions.filter { session ->
        if (searchQuery.isBlank()) return@filter true
        val query = searchQuery.lowercase()
        session.name.lowercase().contains(query) ||
                (session.title?.lowercase()?.contains(query) == true) ||
                (session.prompt.lowercase().contains(query))
    }

    Scaffold(
        floatingActionButton = {
            if (apiKey.isNotBlank()) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Text("+" )
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
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Jules API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    scope.launch {
                        if (apiKey.isBlank()) {
                            error = "API Key is required"
                            return@launch
                        }
                        fetchSessions(apiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Fetching..." else "Fetch Sessions")
            }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Sessions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
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
            apiKey = apiKey,
            onDismiss = { showCreateDialog = false },
            onSessionCreated = { session ->
                sessions = listOf(session) + sessions
                showCreateDialog = false
                onSessionSelected(session)
            }
        )
    }
}

@Composable
fun CreateSessionDialog(
    apiKey: String,
    onDismiss: () -> Unit,
    onSessionCreated: (Session) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var requirePlanApproval by remember { mutableStateOf(true) }
    var sources by remember { mutableStateOf<List<Source>>(emptyList()) }
    var selectedSource by remember { mutableStateOf<Source?>(null) }
    var startingBranch by remember { mutableStateOf("main") }
    var isLoadingSources by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val client = JulesClient(apiKey)
            val response = client.listSources()
            sources = response.sources ?: emptyList()
            if (sources.isNotEmpty()) {
                selectedSource = sources.first()
            }
        } catch (e: Exception) {
            error = "Failed to load sources: ${e.message}"
        } finally {
            isLoadingSources = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
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

                if (isLoadingSources) {
                    Text("Loading sources...")
                } else if (sources.isEmpty()) {
                    Text("No sources available.")
                } else {
                    // Simple dropdown replacement - just a text field for now since DropdownMenu can be tricky in commonMain sometimes
                    Text("Source: ${selectedSource?.name ?: "None"}")
                    // Alternatively, we can let user type the branch
                    TextField(
                        value = startingBranch,
                        onValueChange = { startingBranch = it },
                        label = { Text("Starting Branch") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                    if (prompt.isBlank()) {
                        error = "Prompt is required"
                        return@Button
                    }
                    scope.launch {
                        isCreating = true
                        error = null
                        try {
                            val client = JulesClient(apiKey)
                            val sourceContext = selectedSource?.let { src ->
                                SourceContext(
                                    source = src.name,
                                    githubRepoContext = GithubRepoContext(startingBranch = startingBranch)
                                )
                            }
                            val request = CreateSessionRequest(
                                prompt = prompt,
                                title = title.takeIf { it.isNotBlank() },
                                sourceContext = sourceContext,
                                requirePlanApproval = requirePlanApproval
                            )
                            val session = client.createSession(request)
                            onSessionCreated(session)
                        } catch (e: Exception) {
                            error = "Failed to create: ${e.message}"
                        } finally {
                            isCreating = false
                        }
                    }
                },
                enabled = !isCreating && !isLoadingSources && prompt.isNotBlank()
            ) {
                Text(if (isCreating) "Creating..." else "Create")
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
    apiKey: String,
    onBack: () -> Unit
) {
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var needsPlanApproval by remember { mutableStateOf(false) }
    var isApproving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session.id) {
        try {
            val client = JulesClient(apiKey)
            // Fetch activities
            val response = client.listActivities(sessionId = session.id)
            activities = response.activities ?: emptyList()

            // Update session status to check if plan approval is needed
            val updatedSession = client.getSession(session.id)
            needsPlanApproval = updatedSession.state == "AWAITING_PLAN_APPROVAL"
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.title ?: "Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back" )
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

            if (needsPlanApproval) {
                Button(
                    onClick = {
                        scope.launch {
                            isApproving = true
                            try {
                                val client = JulesClient(apiKey)
                                client.approvePlan(session.id)
                                needsPlanApproval = false
                            } catch (e: Exception) {
                                error = "Approval failed: ${e.message}"
                            } finally {
                                isApproving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApproving
                ) {
                    Text(if (isApproving) "Approving..." else "Approve Plan")
                }
            }

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activities) { activity ->
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
                        scope.launch {
                            isSending = true
                            try {
                                val client = JulesClient(apiKey)
                                client.sendMessage(session.id, SendMessageRequest(prompt = messageText))
                                messageText = ""
                                // Refresh activities
                                val response = client.listActivities(sessionId = session.id)
                                activities = response.activities ?: emptyList()
                            } catch (e: Exception) {
                                error = "Failed to send: ${e.message}"
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    enabled = !isSending && messageText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}
