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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.BoldHighlight

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
                    var isExpanded by remember(file, expandAll) { mutableStateOf(expandAll) }
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

            val isDarkTheme = isSystemInDarkTheme()
            val theme = if (isDarkTheme) SyntaxThemes.darcula() else SyntaxThemes.pastel()
            val highlightsBuilder = remember(theme) {
                Highlights.Builder().theme(theme)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(8.dp)
            ) {
                file.hunks.forEach { hunk ->
                    DiffHunkView(hunk, highlightsBuilder)
                }
            }
        }
    }
}

@Composable
fun DiffHunkView(hunk: DiffHunk, highlightsBuilder: Highlights.Builder) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = hunk.header,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f)).fillMaxWidth().padding(2.dp)
        )

        val hunkText = hunk.lines.joinToString("\n") { it.text }
        val highlights = remember(hunkText, highlightsBuilder) {
            highlightsBuilder.code(hunkText).build().getHighlights()
        }

        var currentIndex = 0
        hunk.lines.forEach { line ->
            val lineLength = line.text.length

            // Filter highlights that intersect with this line
            val lineHighlights = highlights.mapNotNull { highlight ->
                when (highlight) {
                    is ColorHighlight -> {
                        val start = maxOf(0, highlight.location.start - currentIndex)
                        val end = minOf(lineLength, highlight.location.end - currentIndex)
                        if (start < end) ColorHighlight(dev.snipme.highlights.model.PhraseLocation(start, end), highlight.rgb) else null
                    }
                    is BoldHighlight -> {
                        val start = maxOf(0, highlight.location.start - currentIndex)
                        val end = minOf(lineLength, highlight.location.end - currentIndex)
                        if (start < end) BoldHighlight(dev.snipme.highlights.model.PhraseLocation(start, end)) else null
                    }
                    else -> null
                }
            }

            DiffLineView(line, lineHighlights)
            currentIndex += lineLength + 1 // +1 for the newline
        }
    }
}

@Composable
fun DiffLineView(line: DiffLine, highlights: List<dev.snipme.highlights.model.CodeHighlight>) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = when (line.type) {
        DiffLineType.ADDITION -> if (isDarkTheme) Color(0xFF1E4A28) else Color(0xFFE6FFED)
        DiffLineType.DELETION -> if (isDarkTheme) Color(0xFF5A1D24) else Color(0xFFFFEEF0)
        DiffLineType.CONTEXT -> Color.Transparent
        DiffLineType.META -> Color.Transparent
    }

    val defaultTextColor = when (line.type) {
        DiffLineType.ADDITION -> if (isDarkTheme) Color(0xFF6BDB80) else Color(0xFF22863A)
        DiffLineType.DELETION -> if (isDarkTheme) Color(0xFFFF7B86) else Color(0xFFCB2431)
        DiffLineType.CONTEXT -> MaterialTheme.colorScheme.onSurface
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

        val codeText = line.text
        val annotatedString = buildAnnotatedString {
            append(codeText)

            addStyle(SpanStyle(color = defaultTextColor), 0, codeText.length)

            highlights.forEach { highlight ->
                when (highlight) {
                    is ColorHighlight -> {
                        addStyle(
                            SpanStyle(color = Color(highlight.rgb)),
                            highlight.location.start,
                            highlight.location.end
                        )
                    }
                    is BoldHighlight -> {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            highlight.location.start,
                            highlight.location.end
                        )
                    }
                }
            }
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
