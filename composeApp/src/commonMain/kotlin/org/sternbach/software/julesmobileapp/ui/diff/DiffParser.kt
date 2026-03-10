package org.sternbach.software.julesmobileapp.ui.diff

data class DiffFile(
    val oldName: String,
    val newName: String,
    val hunks: List<DiffHunk>
)

data class DiffHunk(
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val header: String,
    val lines: List<DiffLine>
)

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
)

enum class DiffLineType {
    ADDITION, DELETION, CONTEXT, META
}

object DiffParser {
    fun parse(diff: String): List<DiffFile> {
        val files = mutableListOf<DiffFile>()
        val lines = diff.split("\n")
        var i = 0

        var currentOldName = ""
        var currentNewName = ""
        var currentHunks = mutableListOf<DiffHunk>()

        var currentHunkOldStart = 0
        var currentHunkOldCount = 0
        var currentHunkNewStart = 0
        var currentHunkNewCount = 0
        var currentHunkHeader = ""
        var currentHunkLines = mutableListOf<DiffLine>()

        var oldLineNum = 0
        var newLineNum = 0

        fun commitHunk() {
            if (currentHunkLines.isNotEmpty() || currentHunkHeader.isNotEmpty()) {
                currentHunks.add(
                    DiffHunk(
                        currentHunkOldStart,
                        currentHunkOldCount,
                        currentHunkNewStart,
                        currentHunkNewCount,
                        currentHunkHeader,
                        currentHunkLines.toList()
                    )
                )
                currentHunkLines.clear()
                currentHunkHeader = ""
            }
        }

        fun commitFile() {
            commitHunk()
            if (currentOldName.isNotEmpty() || currentNewName.isNotEmpty() || currentHunks.isNotEmpty()) {
                files.add(DiffFile(currentOldName, currentNewName, currentHunks.toList()))
                currentOldName = ""
                currentNewName = ""
                currentHunks.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i]

            when {
                line.startsWith("diff --git") -> {
                    commitFile()
                }
                line.startsWith("--- ") -> {
                    currentOldName = line.removePrefix("--- ").trim().removePrefix("a/")
                }
                line.startsWith("+++ ") -> {
                    currentNewName = line.removePrefix("+++ ").trim().removePrefix("b/")
                }
                line.startsWith("@@ ") -> {
                    commitHunk()
                    currentHunkHeader = line

                    // Parse @@ -start,count +start,count @@
                    val match = Regex("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*").find(line)
                    if (match != null) {
                        currentHunkOldStart = match.groupValues[1].toInt()
                        currentHunkOldCount = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 1
                        currentHunkNewStart = match.groupValues[3].toInt()
                        currentHunkNewCount = match.groupValues[4].takeIf { it.isNotEmpty() }?.toInt() ?: 1

                        oldLineNum = currentHunkOldStart
                        newLineNum = currentHunkNewStart
                    }
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    currentHunkLines.add(DiffLine(DiffLineType.ADDITION, line, null, newLineNum))
                    newLineNum++
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    currentHunkLines.add(DiffLine(DiffLineType.DELETION, line, oldLineNum, null))
                    oldLineNum++
                }
                line.startsWith(" ") -> {
                    currentHunkLines.add(DiffLine(DiffLineType.CONTEXT, line, oldLineNum, newLineNum))
                    oldLineNum++
                    newLineNum++
                }
                line.startsWith("No newline at end of file") -> {
                    currentHunkLines.add(DiffLine(DiffLineType.META, line, null, null))
                }
                else -> {
                    // Could be meta information from git diff (index, new file mode, etc.)
                }
            }
            i++
        }

        commitFile()

        return files
    }
}
