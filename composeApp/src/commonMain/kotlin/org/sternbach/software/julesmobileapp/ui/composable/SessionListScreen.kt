package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.Session
import org.sternbach.software.julesmobileapp.Source
import org.sternbach.software.julesmobileapp.ui.helper.AppState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var repoFilter by remember { mutableStateOf<String?>(null) }
    var groupBy by remember { mutableStateOf<String>("None") }
    var sortBy by remember { mutableStateOf<String>("Newest") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val collapsedGroups = remember { mutableStateListOf<String>() }

    val allRepos = remember(state.sessions) {
        state.sessions.mapNotNull { it.sourceContext?.source }.distinct().sorted()
    }

    var filteredSessions = state.sessions.filter { session ->
        val matchesSearch = if (searchQuery.isBlank()) true else {
            val query = searchQuery.lowercase()
            session.name.lowercase().contains(query) ||
                    (session.title?.lowercase()?.contains(query) == true) ||
                    (session.prompt.lowercase().contains(query))
        }
        val matchesStatus = statusFilter == null || session.state.equals(statusFilter, ignoreCase = true)
        val matchesRepo = repoFilter == null || session.sourceContext?.source.equals(repoFilter, ignoreCase = true)

        matchesSearch && matchesStatus && matchesRepo
    }

    filteredSessions = when (sortBy) {
        "Oldest" -> filteredSessions.sortedBy { it.createTime ?: "" }
        "Name" -> filteredSessions.sortedBy { it.title ?: it.name }
        else -> filteredSessions.sortedByDescending { it.createTime ?: "" }
    }

    val groupedSessions = when (groupBy) {
        "Status" -> filteredSessions.groupBy { it.state ?: "Unknown" }
        "Repo" -> filteredSessions.groupBy { it.sourceContext?.source ?: "Unknown Repo" }
        else -> mapOf("All" to filteredSessions)
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter Sessions")
                    }
                    if (state.isLoadingSessions) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }

            Button(
                onClick = onFetchSessions,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingSessions
            ) {
                Text(if (state.isLoadingSessions) "Fetching..." else "Fetch Sessions")
            }

            if (state.sessionsError != null) {
                Text(text = state.sessionsError, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (groupBy == "None") {
                    items(filteredSessions) { session ->
                        SessionCard(onSessionSelected, session)
                    }
                } else {
                    groupedSessions.forEach { (groupName, sessions) ->
                        stickyHeader {
                            val isCollapsed = collapsedGroups.contains(groupName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (isCollapsed) collapsedGroups.remove(groupName)
                                        else collapsedGroups.add(groupName)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$groupName (${sessions.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (isCollapsed) "Expand" else "Collapse"
                                )
                            }
                        }
                        if (!collapsedGroups.contains(groupName)) {
                            items(sessions) { session ->
                                SessionCard(onSessionSelected, session)
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

    if (showFilterDialog) {
        SessionFilterDialog(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            statusFilter = statusFilter,
            onStatusFilterChange = { statusFilter = it },
            repoFilter = repoFilter,
            onRepoFilterChange = { repoFilter = it },
            availableRepos = allRepos,
            groupBy = groupBy,
            onGroupByChange = { groupBy = it },
            sortBy = sortBy,
            onSortByChange = { sortBy = it },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionFilterDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: String?,
    onStatusFilterChange: (String?) -> Unit,
    repoFilter: String?,
    onRepoFilterChange: (String?) -> Unit,
    availableRepos: List<String>,
    groupBy: String,
    onGroupByChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val statuses = listOf("COMPLETED", "FAILED", "IN_PROGRESS", "PLANNING", "AWAITING_PLAN_APPROVAL", "AWAITING_USER_FEEDBACK", "PAUSED", "QUEUED")
    val groupOptions = listOf("None", "Status", "Repo")
    val sortOptions = listOf("Newest", "Oldest", "Name")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter & Sort Sessions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                var statusExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    TextField(
                        value = statusFilter ?: "All",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Filter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                onStatusFilterChange(null)
                                statusExpanded = false
                            }
                        )
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    onStatusFilterChange(status)
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                var repoExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = repoExpanded,
                    onExpandedChange = { repoExpanded = !repoExpanded }
                ) {
                    TextField(
                        value = repoFilter ?: "All",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repo Filter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repoExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = repoExpanded,
                        onDismissRequest = { repoExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                onRepoFilterChange(null)
                                repoExpanded = false
                            }
                        )
                        availableRepos.forEach { repo ->
                            DropdownMenuItem(
                                text = { Text(repo) },
                                onClick = {
                                    onRepoFilterChange(repo)
                                    repoExpanded = false
                                }
                            )
                        }
                    }
                }

                var groupExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = !groupExpanded }
                ) {
                    TextField(
                        value = groupBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Group By") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        groupOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onGroupByChange(option)
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                var sortExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded }
                ) {
                    TextField(
                        value = sortBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sort By") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSortByChange(option)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

