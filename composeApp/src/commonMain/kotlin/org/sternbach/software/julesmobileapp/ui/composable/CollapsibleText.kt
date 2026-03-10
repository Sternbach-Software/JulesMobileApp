package org.sternbach.software.julesmobileapp.ui.composable

import androidx.compose.foundation.clickable
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
fun CollapsibleText(
    string: String,
    style: TextStyle? = null,
    expanded: Boolean? = null,
    clickable: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    var localExpanded by remember(string) { mutableStateOf(false) }
    val isExpanded = expanded ?: localExpanded
    val toggle = onToggle ?: { localExpanded = !localExpanded }
    var isEllipsized by remember(string) { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = if(clickable) Modifier.clickable { toggle() } else Modifier
    ) {
        Text(
            text = string,
            style = style ?: MaterialTheme.typography.bodyMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            onTextLayout = { textLayoutResult ->
                if (!isExpanded) {
                    isEllipsized = textLayoutResult.hasVisualOverflow
                }
            }
        )
        if (isExpanded || isEllipsized) {
            IconButton(onClick = { toggle() }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Show less" else "Show more"
                )
            }
        }
    }
}