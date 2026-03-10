package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun CollapsibleText(string: String, style: TextStyle? = null) {
    var expanded by remember(string) { mutableStateOf(false) }
    var isEllipsized by remember(string) { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = string,
            style = style ?: MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            onTextLayout = { textLayoutResult ->
                if (!expanded) {
                    isEllipsized = textLayoutResult.hasVisualOverflow
                }
            }
        )
        if (expanded || isEllipsized) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Show less" else "Show more"
                )
            }
        }
    }
}