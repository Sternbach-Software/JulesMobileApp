package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sternbach.software.julesmobileapp.Session


@Composable
fun SessionCard(
    onSessionSelected: (Session) -> Unit,
    session: Session
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).clickable { onSessionSelected(session) }) {
            CollapsibleText(session.title ?: "No Title", style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${session.id}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            CollapsibleText(session.prompt)
            if (session.state != null) {
                Spacer(modifier = Modifier.height(4.dp))
                StateBadge(state = session.state)
            }
            if (session.sourceContext != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source: ${session.sourceContext.source}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
