package org.sternbach.software.julesmobileapp.ui.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.sternbach.software.julesmobileapp.Activity
import org.sternbach.software.julesmobileapp.CacheManager
import org.sternbach.software.julesmobileapp.CreateSessionRequest
import org.sternbach.software.julesmobileapp.GithubRepoContext
import org.sternbach.software.julesmobileapp.JulesClient
import org.sternbach.software.julesmobileapp.SendMessageRequest
import org.sternbach.software.julesmobileapp.Session
import org.sternbach.software.julesmobileapp.Source
import org.sternbach.software.julesmobileapp.SourceContext
import org.sternbach.software.julesmobileapp.jsonFormat

data class AppState(
    val currentScreen: Screen = Screen.SessionList,
    val apiKey: String = "",
    val sessions: List<Session> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val sessionsError: String? = null,

    // Create Session
    val sources: List<Source> = emptyList(),
    val isLoadingSources: Boolean = false,
    val sourcesError: String? = null,
    val nextSourcePageToken: String? = null,
    val sourceSearchQuery: String = "",
    val isInMemorySourceSearch: Boolean = false,
    val isCreatingSession: Boolean = false,
    val createSessionError: String? = null,

    // Session Details
    val activities: List<Activity> = emptyList(),
    val isLoadingActivities: Boolean = true,
    val activitiesError: String? = null,
    val needsPlanApproval: Boolean = false,
    val isApprovingPlan: Boolean = false,
    val approvePlanError: String? = null,
    val isSendingMessage: Boolean = false,
    val sendMessageError: String? = null
)

class JulesViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cachedKey = CacheManager.readApiKey()
                if (!cachedKey.isNullOrBlank()) {
                    _state.update { it.copy(apiKey = cachedKey) }
                }
                val cached = CacheManager.readCache()
                if (cached != null) {
                    val decoded = jsonFormat.decodeFromString<List<Session>>(cached)
                    _state.update { it.copy(sessions = decoded) }
                }
                if (!cachedKey.isNullOrBlank()) {
                    fetchSessions(cachedKey)
                }
            } catch (e: Exception) {
                println("DEBUG: Initialization error: ${e.message}")
            }
        }
    }

    fun fetchSessions(key: String) {
        println("viewmodel: fun fetchSessions(key: String) {")
        if (key.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoadingSessions = true, sessionsError = null) }
            try {
                println("DEBUG: Fetching sessions with API key: ${key.take(4)}...")
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

                println("DEBUG: Fetched ${allSessions.size} sessions")
                println("DEBUG: Response sessions: $allSessions")

                _state.update { it.copy(sessions = allSessions, isLoadingSessions = false) }

                try {
                    val jsonString = jsonFormat.encodeToString(allSessions)
                    CacheManager.writeCache(jsonString)
                } catch (e: Exception) {
                    println("DEBUG: Failed to cache sessions: ${e.message}")
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to fetch sessions: ${e.message}")
                _state.update { it.copy(sessionsError = "Failed to fetch: ${e.message}", isLoadingSessions = false) }
            }
        }
    }

    fun setApiKey(key: String) {
        println("viewmodel: fun setApiKey(key: String) {")
        _state.update { it.copy(apiKey = key) }
        viewModelScope.launch(Dispatchers.IO) {
            CacheManager.writeApiKey(key)
        }
    }

    fun navigateToSessionList() {
        println("viewmodel: fun navigateToSessionList() {")
        _state.update { it.copy(currentScreen = Screen.SessionList) }
    }

    fun navigateToSessionDetail(session: Session) {
        println("viewmodel: fun navigateToSessionDetail(session: Session) {")
        _state.update { it.copy(currentScreen = Screen.SessionDetail(session)) }
        loadSessionDetails(session.id)
    }

    private fun loadSessionDetails(sessionId: String) {
        println("viewmodel: private fun loadSessionDetails(sessionId: String) {")
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoadingActivities = true, activitiesError = null) }
            try {
                val apiKey = _state.value.apiKey
                println("DEBUG: Loading details for session: $sessionId with API key: ${apiKey.take(4)}...")
                val client = JulesClient(apiKey)

                // Fetch activities
                val response = client.listActivities(sessionId = sessionId)
                println("DEBUG: Fetched ${response.activities?.size ?: 0} activities for session $sessionId")
                println("DEBUG: Response activities: ${response.activities}")
                _state.update { it.copy(activities = response.activities ?: emptyList()) }

                // Update session status to check if plan approval is needed
                val updatedSession = client.getSession(sessionId)
                println("DEBUG: Updated session status for $sessionId: ${updatedSession.state}")
                _state.update { it.copy(needsPlanApproval = updatedSession.state == "AWAITING_PLAN_APPROVAL") }
            } catch (e: Exception) {
                println("DEBUG: Failed to load session details: ${e.message}")
                _state.update { it.copy(activitiesError = e.message) }
            } finally {
                _state.update { it.copy(isLoadingActivities = false) }
            }
        }
    }

    fun setSourceSearchQuery(query: String) {
        println("viewmodel: fun setSourceSearchQuery(query: String) {")
        val oldQuery = _state.value.sourceSearchQuery
        _state.update { it.copy(sourceSearchQuery = query) }
        if (!_state.value.isInMemorySourceSearch && oldQuery != query) {
            loadSources(reset = true)
        }
    }

    fun toggleSourceSearchMode(isInMemory: Boolean) {
        println("viewmodel: fun toggleSourceSearchMode(isInMemory: Boolean) {")
        _state.update { it.copy(isInMemorySourceSearch = isInMemory) }
        loadSources(reset = true)
    }

    fun loadSources(reset: Boolean = false) {
        println("viewmodel: fun loadSources(reset: Boolean = false) {")
        if (_state.value.isLoadingSources && !reset) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoadingSources = true, sourcesError = null) }
            try {
                val currentState = _state.value
                val client = JulesClient(currentState.apiKey)
                
                if (currentState.isInMemorySourceSearch) {
                    val allSources = mutableListOf<Source>()
                    var token: String? = null
                    do {
                        val response = client.listSources(pageSize = 100, pageToken = token)
                        allSources.addAll(response.sources ?: emptyList())
                        token = response.nextPageToken
                    } while (token != null)
                    _state.update { it.copy(sources = allSources, nextSourcePageToken = null) }
                } else {
                    val currentToken = if (reset) null else currentState.nextSourcePageToken
                    val response = client.listSources(
                        pageSize = 50,
                        pageToken = currentToken,
                        filter = currentState.sourceSearchQuery.takeIf { it.isNotBlank() }
                    )
                    _state.update { 
                        it.copy(
                            sources = if (reset) (response.sources ?: emptyList()) else (it.sources + (response.sources ?: emptyList())),
                            nextSourcePageToken = response.nextPageToken
                        )
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to load sources: ${e.message}")
                _state.update { it.copy(sourcesError = "Failed to load sources: ${e.message}") }
            } finally {
                _state.update { it.copy(isLoadingSources = false) }
            }
        }
    }

    fun createSession(prompt: String, title: String, source: Source?, startingBranch: String, requirePlanApproval: Boolean, onSuccess: (Session) -> Unit) {
        println("viewmodel: fun createSession(prompt: String, title: String, source: Source?, startingBranch: String, requirePlanApproval: Boolean, onSuccess: (Session) -> Unit) {")
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isCreatingSession = true, createSessionError = null) }
            try {
                val apiKey = _state.value.apiKey
                println("DEBUG: Creating session with API key: ${apiKey.take(4)}...")
                val client = JulesClient(apiKey)
                val sourceContext = source?.let { src ->
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
                println("DEBUG: Created session: $session")

                // Refresh sessions and notify success
                val updatedSessions = listOf(session) + _state.value.sessions
                _state.update { it.copy(sessions = updatedSessions) }

                withContext(Dispatchers.Main) {
                    onSuccess(session)
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to create session: ${e.message}")
                _state.update { it.copy(createSessionError = "Failed to create: ${e.message}") }
            } finally {
                _state.update { it.copy(isCreatingSession = false) }
            }
        }
    }

    fun approvePlan(sessionId: String) {
        println("viewmodel: fun approvePlan(sessionId: String) {")
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isApprovingPlan = true, approvePlanError = null) }
            try {
                val apiKey = _state.value.apiKey
                println("DEBUG: Approving plan for session: $sessionId with API key: ${apiKey.take(4)}...")
                val client = JulesClient(apiKey)
                client.approvePlan(sessionId)
                println("DEBUG: Approved plan for session: $sessionId")
                _state.update { it.copy(needsPlanApproval = false) }
            } catch (e: Exception) {
                println("DEBUG: Failed to approve plan: ${e.message}")
                _state.update { it.copy(approvePlanError = "Approval failed: ${e.message}") }
            } finally {
                _state.update { it.copy(isApprovingPlan = false) }
            }
        }
    }

    fun fetchActivity(sessionId: String, activityId: String) {
        println("viewmodel: fun fetchActivity(sessionId: String, activityId: String) {")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = _state.value.apiKey
                println("DEBUG: Fetching activity $activityId for session: $sessionId with API key: ${apiKey.take(4)}...")
                val client = JulesClient(apiKey)

                val fetchedActivity = client.getActivity(sessionId, activityId)
                println("DEBUG: Fetched activity: $fetchedActivity")

                // Update the activity in the list
                val updatedActivities = _state.value.activities.map {
                    if (it.id == activityId) fetchedActivity else it
                }
                _state.update { it.copy(activities = updatedActivities) }
            } catch (e: Exception) {
                println("DEBUG: Failed to fetch activity: ${e.message}")
                _state.update { it.copy(activitiesError = "Failed to fetch activity: ${e.message}") }
            }
        }
    }

    fun sendMessage(sessionId: String, messageText: String, onMessageSent: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isSendingMessage = true, sendMessageError = null) }
            try {
                val apiKey = _state.value.apiKey
                println("DEBUG: Sending message to session: $sessionId with API key: ${apiKey.take(4)}...")
                val client = JulesClient(apiKey)
                client.sendMessage(sessionId, SendMessageRequest(prompt = messageText))
                println("DEBUG: Sent message to session: $sessionId")

                // Refresh activities
                val response = client.listActivities(sessionId = sessionId)
                println("DEBUG: Fetched ${response.activities?.size ?: 0} activities after sending message")
                _state.update { it.copy(activities = response.activities ?: emptyList()) }

                withContext(Dispatchers.Main) {
                    onMessageSent()
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to send message: ${e.message}")
                _state.update { it.copy(sendMessageError = "Failed to send: ${e.message}") }
            } finally {
                _state.update { it.copy(isSendingMessage = false) }
            }
        }
    }
}
