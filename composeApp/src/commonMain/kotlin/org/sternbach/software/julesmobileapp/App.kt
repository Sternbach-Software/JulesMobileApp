package org.sternbach.software.julesmobileapp

import androidx.compose.foundation.background
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

@Composable
fun App() {
    MaterialTheme {
        var apiKey by remember { mutableStateOf("") }
        var searchQuery by remember { mutableStateOf("") }
        var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
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

        // Read cache on startup
        LaunchedEffect(Unit) {
            try {
                val cachedKey = CacheManager.readApiKey()
                if (!cachedKey.isNullOrBlank()) {
                    apiKey = cachedKey
                }

                val cached = CacheManager.readCache()
                if (cached != null) {
                    val decoded = jsonFormat.decodeFromString<List<Session>>(cached)
                    sessions = decoded
                }

                if (!cachedKey.isNullOrBlank()) {
                    fetchSessions(cachedKey)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = "Failed to initialize: ${e.message}"
            }
        }

        val filteredSessions = sessions.filter { session ->
            if (searchQuery.isBlank()) return@filter true
            val query = searchQuery.lowercase()
            session.name.lowercase().contains(query) ||
                    (session.title?.lowercase()?.contains(query) == true) ||
                    (session.prompt?.lowercase()?.contains(query) == true)
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    CacheManager.writeApiKey(it)
                },
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = session.title ?: "No Title", style = MaterialTheme.typography.titleMedium)
                            Text(text = "ID: ${session.id}", style = MaterialTheme.typography.bodySmall)
                            if (session.prompt != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = session.prompt, style = MaterialTheme.typography.bodyMedium)
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
}