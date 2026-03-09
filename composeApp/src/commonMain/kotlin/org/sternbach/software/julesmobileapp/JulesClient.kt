package org.sternbach.software.julesmobileapp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val title: String? = null,
    val sourceContext: SourceContext? = null,
    val prompt: String? = null,
    val outputs: List<Output>? = null
)

@Serializable
data class ListSessionsResponse(
    val sessions: List<Session>? = null,
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

    suspend fun listSessions(pageSize: Int = 50, pageToken: String? = null): ListSessionsResponse {
        return client.get("https://jules.googleapis.com/v1alpha/sessions") {
            header("x-goog-api-key", apiKey)
            parameter("pageSize", pageSize)
            if (pageToken != null) {
                parameter("pageToken", pageToken)
            }
        }.body()
    }
}