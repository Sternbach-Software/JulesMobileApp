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
import androidx.compose.foundation.isSystemInDarkTheme
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.highlights.model.SyntaxLanguage
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

private fun getLanguageFromExtension(extension: String): SyntaxLanguage {
    return when (extension.lowercase()) {
        "kt", "kts" -> SyntaxLanguage.KOTLIN
        "java" -> SyntaxLanguage.JAVA
        "swift" -> SyntaxLanguage.SWIFT
        "js" -> SyntaxLanguage.JAVASCRIPT
        "ts" -> SyntaxLanguage.TYPESCRIPT
        "py" -> SyntaxLanguage.PYTHON
        "c" -> SyntaxLanguage.C
        "cpp", "h", "hpp" -> SyntaxLanguage.CPP
        "cs" -> SyntaxLanguage.CSHARP
        "go" -> SyntaxLanguage.GO
        "php" -> SyntaxLanguage.PHP
        "rb" -> SyntaxLanguage.RUBY
        "sh" -> SyntaxLanguage.SHELL
        "rs" -> SyntaxLanguage.RUST
        "dart" -> SyntaxLanguage.DART
        else -> SyntaxLanguage.DEFAULT
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
            val theme = if (isDarkTheme) SyntaxThemes.darcula(true) else SyntaxThemes.pastel(false)
            
            val language = remember(filename) {
                val extension = filename.substringAfterLast('.', "")
                getLanguageFromExtension(extension)
            }

            val highlightsBuilder = remember(theme, language) {
                Highlights.Builder()
                    .theme(theme)
                    .language(language)
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

        val showOld = BooleanArray(hunk.lines.size) { true }
        val showNew = BooleanArray(hunk.lines.size) { true }

        var nextDiffType: DiffLineType? = null
        val nextTypeForContext = Array<DiffLineType?>(hunk.lines.size) { null }
        for (i in hunk.lines.indices.reversed()) {
            val line = hunk.lines[i]
            if (line.type == DiffLineType.ADDITION || line.type == DiffLineType.DELETION) {
                nextDiffType = line.type
            }
            if (line.type == DiffLineType.CONTEXT) {
                nextTypeForContext[i] = nextDiffType
            }
        }

        var prevDiffType: DiffLineType? = null
        for (i in hunk.lines.indices) {
            val line = hunk.lines[i]
            if (line.type == DiffLineType.ADDITION || line.type == DiffLineType.DELETION) {
                prevDiffType = line.type
            }
            if (line.type == DiffLineType.CONTEXT) {
                if (line.oldLineNumber != null && line.oldLineNumber == line.newLineNumber) {
                    val resolvedType = nextTypeForContext[i] ?: prevDiffType ?: DiffLineType.DELETION
                    if (resolvedType == DiffLineType.ADDITION) {
                        showOld[i] = false
                        showNew[i] = true
                    } else if (resolvedType == DiffLineType.DELETION) {
                        showOld[i] = true
                        showNew[i] = false
                    }
                } else {
                    showOld[i] = true
                    showNew[i] = true
                }
            } else if (line.type == DiffLineType.ADDITION) {
                showOld[i] = false
                showNew[i] = true
            } else if (line.type == DiffLineType.DELETION) {
                showOld[i] = true
                showNew[i] = false
            } else if (line.type == DiffLineType.META) {
                showOld[i] = false
                showNew[i] = false
            }
        }

        hunk.lines.forEachIndexed { index, line ->
            // Highlight each line individually to avoid complex indexing
            // and potentially improve accuracy by stripping the prefix
            val codePart = if (line.text.isNotEmpty()) line.text.drop(1) else ""
            
            val highlights = remember(codePart, highlightsBuilder) {
                highlightsBuilder.code(codePart).build().getHighlights()
            }
            
            DiffLineView(line, highlights, showOld[index], showNew[index])
        }
    }
}

@Composable
fun DiffLineView(
    line: DiffLine,
    highlights: List<dev.snipme.highlights.model.CodeHighlight>,
    showOldNumber: Boolean = true,
    showNewNumber: Boolean = true
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = when (line.type) {
        DiffLineType.ADDITION -> if (isDarkTheme) Color(0xFF1E4A28).copy(alpha = 0.4f) else Color(0xFFE6FFED)
        DiffLineType.DELETION -> if (isDarkTheme) Color(0xFF5A1D24).copy(alpha = 0.4f) else Color(0xFFFFEEF0)
        DiffLineType.CONTEXT -> Color.Transparent
        DiffLineType.META -> Color.Transparent
    }

    val defaultTextColor = when (line.type) {
        DiffLineType.ADDITION, DiffLineType.DELETION, DiffLineType.CONTEXT -> 
            if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        DiffLineType.META -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (showOldNumber) (line.oldLineNumber?.toString() ?: "   ") else "   ",
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = if (showNewNumber) (line.newLineNumber?.toString() ?: "   ") else "   ",
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(8.dp))

        val fullText = line.text
        val prefix = if (fullText.isNotEmpty()) fullText.take(1) else ""
        val codePart = if (fullText.isNotEmpty()) fullText.drop(1) else ""

        val annotatedString = buildAnnotatedString {
            // Render prefix with lower opacity to distinguish from code
            withStyle(SpanStyle(color = defaultTextColor.copy(alpha = 0.5f))) {
                append(prefix)
            }
            
            val codeStart = length
            withStyle(SpanStyle(color = defaultTextColor)) {
                append(codePart)
            }

            highlights.forEach { highlight ->
                val start = codeStart + highlight.location.start
                val end = codeStart + highlight.location.end
                
                // Ensure indices are within bounds
                if (start >= 0 && end <= length && start < end) {
                    when (highlight) {
                        is ColorHighlight -> {
                            // Ensure the color is opaque using bitwise OR with 0xFF000000
                            // This handles libraries that return 24-bit colors without alpha.
                            val colorInt = highlight.rgb or 0xFF000000.toInt()
                            addStyle(
                                SpanStyle(color = Color(colorInt)),
                                start,
                                end
                            )
                        }
                        is BoldHighlight -> {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                start,
                                end
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}
