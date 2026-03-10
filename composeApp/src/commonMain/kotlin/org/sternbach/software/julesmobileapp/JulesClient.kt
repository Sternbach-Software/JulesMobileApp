package org.sternbach.software.julesmobileapp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GithubBranch(
    val displayName: String
)

@Serializable
data class GithubRepo(
    val owner: String,
    val repo: String,
    val isPrivate: Boolean? = null,
    val defaultBranch: GithubBranch? = null,
    val branches: List<GithubBranch>? = null
)

@Serializable
data class Source(
    val name: String,
    val id: String,
    val githubRepo: GithubRepo? = null
)

@Serializable
data class ListSourcesResponse(
    val sources: List<Source>? = null,
    val nextPageToken: String? = null
)

@Serializable
data class SourceContext(
    val source: String,
    val githubRepoContext: GithubRepoContext? = null
)

@Serializable
data class GithubRepoContext(
    val startingBranch: String? = null
)

@Serializable
data class PullRequest(
    val url: String,
    val title: String,
    val description: String
)

@Serializable
data class Output(
    val pullRequest: PullRequest? = null
)

@Serializable
data class Session(
    val name: String,
    val id: String,
    val prompt: String,
    val title: String? = null,
    val state: String? = null,
    val url: String? = null,
    val sourceContext: SourceContext? = null,
    val requirePlanApproval: Boolean? = null,
    val automationMode: String? = null,
    val outputs: List<Output>? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class ListSessionsResponse(
    val sessions: List<Session>? = null,
    val nextPageToken: String? = null
)

@Serializable
data class CreateSessionRequest(
    val prompt: String,
    val sourceContext: SourceContext? = null,
    val automationMode: String? = null,
    val title: String? = null,
    val requirePlanApproval: Boolean? = null
)

@Serializable
data class SendMessageRequest(
    val prompt: String
)

@Serializable
data class PlanStep(
    val id: String? = null,
    val index: Int? = null,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class Plan(
    val id: String? = null,
    val steps: List<PlanStep>? = null,
    val createTime: String? = null
)

@Serializable
data class PlanGenerated(
    val plan: Plan? = null
)

@Serializable
data class PlanApproved(
    val planId: String? = null
)

@Serializable
data class UserMessaged(
    val userMessage: String? = null
)

@Serializable
data class AgentMessaged(
    val agentMessage: String? = null
)

@Serializable
data class ProgressUpdated(
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class SessionCompleted(
    val dummy: String? = null // Kotlinx serialization needs something or it can be empty class, but empty classes are tricky. Let's make it object if empty, or just use JsonObject
)

@Serializable
data class SessionFailed(
    val reason: String? = null
)

@Serializable
data class Activity(
    val name: String,
    val id: String,
    val originator: String? = null,
    val description: String? = null,
    val createTime: String? = null,
    val planGenerated: PlanGenerated? = null,
    val planApproved: PlanApproved? = null,
    val userMessaged: UserMessaged? = null,
    val agentMessaged: AgentMessaged? = null,
    val progressUpdated: ProgressUpdated? = null,
    // sessionCompleted and sessionFailed could be empty objects
    val sessionFailed: SessionFailed? = null
)

@Serializable
data class ListActivitiesResponse(
    val activities: List<Activity>? = null,
    val nextPageToken: String? = null
)

class JulesClient(private val apiKey: String) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun listSources(pageSize: Int = 50, pageToken: String? = null, filter: String? = null): ListSourcesResponse {
        val response = client.get("https://jules.googleapis.com/v1alpha/sources") {
            header("x-goog-api-key", apiKey)
            parameter("pageSize", pageSize)
            if (pageToken != null) {
                parameter("pageToken", pageToken)
            }
            if (filter != null) {
                parameter("filter", filter)
            }
        }.body<ListSourcesResponse>()
        println("DEBUG: listSources response: $response")
        return response
    }

    suspend fun createSession(request: CreateSessionRequest): Session {
        val response = client.post("https://jules.googleapis.com/v1alpha/sessions") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<Session>()
        println("DEBUG: createSession response: $response")
        return response
    }

    suspend fun getSession(sessionId: String): Session {
        val response = client.get("https://jules.googleapis.com/v1alpha/sessions/$sessionId") {
            header("x-goog-api-key", apiKey)
        }.body<Session>()
        println("DEBUG: getSession response: $response")
        return response
    }

    suspend fun listSessions(pageSize: Int = 50, pageToken: String? = null): ListSessionsResponse {
        val response = client.get("https://jules.googleapis.com/v1alpha/sessions") {
            header("x-goog-api-key", apiKey)
            parameter("pageSize", pageSize)
            if (pageToken != null) {
                parameter("pageToken", pageToken)
            }
        }.body<ListSessionsResponse>()
        println("DEBUG: listSessions response: $response")
        return response
    }

    suspend fun approvePlan(sessionId: String) {
        val response = client.post("https://jules.googleapis.com/v1alpha/sessions/$sessionId:approvePlan") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
        }
        println("DEBUG: approvePlan response status: ${response.status}")
    }

    suspend fun listActivities(sessionId: String, pageSize: Int = 50, pageToken: String? = null): ListActivitiesResponse {
        val response = client.get("https://jules.googleapis.com/v1alpha/sessions/$sessionId/activities") {
            header("x-goog-api-key", apiKey)
            parameter("pageSize", pageSize)
            if (pageToken != null) {
                parameter("pageToken", pageToken)
            }
        }.body<ListActivitiesResponse>()
        println("DEBUG: listActivities response: $response")
        return response
    }

    suspend fun sendMessage(sessionId: String, request: SendMessageRequest) {
        val response = client.post("https://jules.googleapis.com/v1alpha/sessions/$sessionId:sendMessage") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("DEBUG: sendMessage response status: ${response.status}")
    }
}
