package org.sternbach.software.julesmobileapp.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewer(files: List<DiffFile>) {
    var isTabbedMode by remember { mutableStateOf(false) }
    var expandAll by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stacked", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = isTabbedMode,
                    onCheckedChange = { isTabbedMode = it },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text("Tabbed", style = MaterialTheme.typography.bodyMedium)
            }
            if (!isTabbedMode) {
                TextButton(onClick = { expandAll = !expandAll }) {
                    Text(if (expandAll) "Collapse All" else "Expand All")
                }
            }
        }

        if (files.isEmpty()) {
            Text("No changes.", modifier = Modifier.padding(16.dp))
            return
        }

        if (isTabbedMode) {
            var selectedFileIndex by remember { mutableStateOf(0) }
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedFileIndex,
                edgePadding = 8.dp
            ) {
                files.forEachIndexed { index, file ->
                    val filename = if (file.newName == "/dev/null") file.oldName else file.newName
                    Tab(
                        selected = selectedFileIndex == index,
                        onClick = { selectedFileIndex = index },
                        text = { Text(filename.substringAfterLast('/')) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val file = files.getOrNull(selectedFileIndex)
                if (file != null) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        DiffFileView(file = file, isExpanded = true, onToggle = {})
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(files) { file ->
                    var isExpanded by rememberSaveable(file, expandAll) { mutableStateOf(expandAll) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        DiffFileView(
                            file = file,
                            isExpanded = isExpanded,
                            onToggle = { isExpanded = !isExpanded }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiffFileView(file: DiffFile, isExpanded: Boolean, onToggle: () -> Unit) {
    Column {
        val filename = if (file.newName == "/dev/null") file.oldName else file.newName
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(filename, style = MaterialTheme.typography.titleMedium)
        }

        if (isExpanded) {
            val horizontalScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(8.dp)
            ) {
                file.hunks.forEach { hunk ->
                    DiffHunkView(hunk)
                }
            }
        }
    }
}

@Composable
fun DiffHunkView(hunk: DiffHunk) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = hunk.header,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f)).fillMaxWidth().padding(2.dp)
        )
        hunk.lines.forEach { line ->
            DiffLineView(line)
        }
    }
}

@Composable
fun DiffLineView(line: DiffLine) {
    val backgroundColor = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFFE6FFED) // Light green
        DiffLineType.DELETION -> Color(0xFFFFEEF0) // Light red
        DiffLineType.CONTEXT -> Color.Transparent
        DiffLineType.META -> Color.Transparent
    }

    val textColor = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFF22863A) // Dark green
        DiffLineType.DELETION -> Color(0xFFCB2431) // Dark red
        DiffLineType.CONTEXT -> Color.Unspecified
        DiffLineType.META -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = line.oldLineNumber?.toString() ?: "   ",
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = line.newLineNumber?.toString() ?: "   ",
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
