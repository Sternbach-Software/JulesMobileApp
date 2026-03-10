package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.Activity
import org.sternbach.software.julesmobileapp.Artifact
import org.sternbach.software.julesmobileapp.Session

@Composable
fun ActivityCard(
    activity: Activity,
    onFetchActivity: (String, String) -> Unit,
    session: Session
) {
    var activityFetched by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(activity.id) { mutableStateOf(false) }

    val content = remember(activity) {
        StringBuilder().apply {
            activity.description?.let { appendLine(it) }
            activity.userMessaged?.userMessage?.let { appendLine(it) }
            activity.agentMessaged?.agentMessage?.let { appendLine(it) }
            if (activity.planApproved != null) {
                appendLine("Plan Approved.")
            }
            activity.planGenerated?.plan?.steps?.let { steps ->
                appendLine("Plan Generated:")
                steps.forEach { step ->
                    appendLine("${(step.index ?: 0) + 1}: ${step.title}")
                    step.description?.let { appendLine(it) }
                }
            }
            activity.progressUpdated?.let { progress ->
                if (!progress.title.isNullOrEmpty() && !progress.description.isNullOrEmpty()) {
                    appendLine("${progress.title}: ${progress.description}")
                }
            }
            activity.sessionFailed?.let { failed ->
                appendLine("Session Failed${failed.reason?.let { ": $it" } ?: ""}")
            }
            if (activity.artifacts != null && activity.artifacts.isNotEmpty()) {
                if (activity.artifacts.any { it.changeSet != null }) {
                    appendLine(extractStrings(activity.artifacts, ::extractChangeset))
                }
                if (activity.artifacts.any { it.bashOutput != null }) {
                    appendLine(extractStrings(activity.artifacts, ::extractBashOutput))
                }
                if (activity.artifacts.any { it.media != null }) {
                    appendLine(extractStrings(activity.artifacts, ::extractMedia))
                }
            }
        }.trim().toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = MutableInteractionSource()
            ) { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = activity.originator ?: "Unknown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (content.isNotEmpty()) {
                CollapsibleText(
                    string = content,
                    expanded = expanded,
                    clickable = true,
                    onToggle = { expanded = !expanded }
                )
            }

            if (content.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = !activityFetched,
                    onClick = {
                        activityFetched = true
                        onFetchActivity(session.id, activity.id)
                    }
                ) {
                    Text("Fetch Activity")
                }
            }
        }
    }
}

private fun extractMedia(artifact: Artifact): List<String> = listOfNotNull(
    artifact.media?.data?.let { "Data: $it" },
    artifact.media?.mimeType?.let { "Type: $it" },
)

private fun extractStrings(artifacts: List<Artifact>, extractString: (Artifact) -> List<String>): String = artifacts
    .map {
        extractString(it)
    }
    .joinToString("\n") {
        it.joinToString("\n")
    }

private fun extractChangeset(artifact: Artifact): List<String> = listOfNotNull(
    artifact.changeSet?.gitPatch?.baseCommitId?.let { "Commit ID: $it" },
    artifact.changeSet?.gitPatch?.suggestedCommitMessage?.let { "Commit message: $it" },
    artifact.changeSet?.gitPatch?.unidiffPatch?.let { "Diff: $it" }
)

private fun extractBashOutput(artifact: Artifact): List<String> = listOfNotNull(
    artifact.bashOutput?.let { "Command: $it" },
    artifact.bashOutput?.output?.let { "Output: $it" },
    artifact.bashOutput?.exitCode?.let { "Exit code: $it" }
)
