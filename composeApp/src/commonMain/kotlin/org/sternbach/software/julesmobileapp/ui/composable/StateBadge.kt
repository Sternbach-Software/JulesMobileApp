package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StateBadge(state: String) {
    val (color, icon, contentDescription) = when (state) {
        "COMPLETED" -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Completed") // Green
        "FAILED" -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Error, "Failed") // Red
        "IN_PROGRESS" -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.PlayCircleFilled, "In Progress") // Blue
        "PLANNING" -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.Pending, "Planning") // Blue
        "AWAITING_PLAN_APPROVAL", "AWAITING_USER_FEEDBACK" -> Triple(Color(0xFFFF9800), Icons.Default.QuestionMark, "Needs Attention") // Orange
        "PAUSED" -> Triple(MaterialTheme.colorScheme.secondary, Icons.Default.PauseCircle, "Paused")
        "QUEUED" -> Triple(MaterialTheme.colorScheme.secondary, Icons.Default.HourglassEmpty, "Queued")
        else -> Triple(MaterialTheme.colorScheme.secondary, Icons.Default.QuestionMark, "Unknown State")
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = state,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
