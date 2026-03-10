package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.CacheManager
import org.sternbach.software.julesmobileapp.Constants
import org.sternbach.software.julesmobileapp.Source
import org.sternbach.software.julesmobileapp.ui.helper.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionDialog(
    state: AppState,
    onDismiss: () -> Unit,
    onSetSourceSearchQuery: (String) -> Unit,
    onToggleSourceSearchMode: (Boolean) -> Unit,
    onLoadMoreSources: () -> Unit,
    onCreateSession: (String, String, Source?, String, Boolean) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var requirePlanApproval by remember { mutableStateOf(CacheManager.readBooleanPreference(Constants.KEY_PLAN_APPROVAL, true)) }
    var selectedSource by remember { mutableStateOf<Source?>(null) }
    var startingBranch by remember { mutableStateOf(CacheManager.readPreference(Constants.KEY_LAST_BRANCH) ?: "main") }
    var showSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.sources) {
        if (selectedSource == null) {
            val lastSourceId = CacheManager.readPreference(Constants.KEY_LAST_SOURCE_ID)
            if (lastSourceId != null) {
                val lastSource = state.sources.find { it.id == lastSourceId }
                if (lastSource != null) {
                    selectedSource = lastSource
                }
            }
        }
    }

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

                OutlinedCard(
                    onClick = { showSourceDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedSource == null) "Select Source" else "Source: ${selectedSource?.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedSource == null) {
                            Text(text = "Tap to choose a repository", style = MaterialTheme.typography.bodySmall)
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
                                CacheManager.writePreference(Constants.KEY_LAST_BRANCH, it)
                                branchDropdownExpanded = true
                            },
                            label = { Text("Starting Branch") },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
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
                                            CacheManager.writePreference(Constants.KEY_LAST_BRANCH, branch.displayName)
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
                        onValueChange = {
                            startingBranch = it
                            CacheManager.writePreference(Constants.KEY_LAST_BRANCH, it)
                        },
                        label = { Text("Starting Branch") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = requirePlanApproval,
                        onCheckedChange = {
                            requirePlanApproval = it
                            CacheManager.writeBooleanPreference(Constants.KEY_PLAN_APPROVAL, it)
                        }
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
                enabled = !state.isCreatingSession && prompt.isNotBlank()
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

    if (showSourceDialog) {
        SourceSelectionDialog(
            state = state,
            onDismiss = { showSourceDialog = false },
            onSetSourceSearchQuery = onSetSourceSearchQuery,
            onToggleSourceSearchMode = onToggleSourceSearchMode,
            onLoadMoreSources = onLoadMoreSources,
            onSourceSelected = { source ->
                selectedSource = source
                CacheManager.writePreference(Constants.KEY_LAST_SOURCE_ID, source.id)
                startingBranch = source.githubRepo?.defaultBranch?.displayName ?: "main"
                CacheManager.writePreference(Constants.KEY_LAST_BRANCH, startingBranch)
                showSourceDialog = false
            }
        )
    }
}
