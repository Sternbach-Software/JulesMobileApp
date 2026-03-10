package org.sternbach.software.julesmobileapp.ui.helper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.ui.composable.AppState
import org.sternbach.software.julesmobileapp.Source

@Composable
fun SourceSelectionDialog(
    state: AppState,
    onDismiss: () -> Unit,
    onSetSourceSearchQuery: (String) -> Unit,
    onToggleSourceSearchMode: (Boolean) -> Unit,
    onLoadMoreSources: () -> Unit,
    onSourceSelected: (Source) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 500.dp)) {
                TextField(
                    value = state.sourceSearchQuery,
                    onValueChange = onSetSourceSearchQuery,
                    label = { Text("Search Sources") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = state.isInMemorySourceSearch,
                        onCheckedChange = onToggleSourceSearchMode
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("In-memory Search (fetches all pages)")
                }

                if (state.sourcesError != null) {
                    Text(text = state.sourcesError, color = MaterialTheme.colorScheme.error)
                }

                val filteredSources = if (state.isInMemorySourceSearch && state.sourceSearchQuery.isNotBlank()) {
                    state.sources.filter { it.name.contains(state.sourceSearchQuery, ignoreCase = true) }
                } else {
                    state.sources
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredSources) { source ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSourceSelected(source) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = source.name,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (state.nextSourcePageToken != null && !state.isInMemorySourceSearch) {
                        item {
                            Button(
                                onClick = onLoadMoreSources,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isLoadingSources
                            ) {
                                Text(if (state.isLoadingSources) "Loading..." else "Load More")
                            }
                        }
                    }

                    if (state.isLoadingSources && state.sources.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
