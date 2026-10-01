package com.mustime.features.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class ChecklistItem(val isChecked: Boolean, val text: String, val lineIndex: Int) : MarkdownBlock()
    data class BulletItem(val text: String, val indentLevel: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    object EmptyLine : MarkdownBlock()
}

object MarkdownUtils {

    /**
     * Strips Markdown syntax for clean card previews and subtitles.
     */
    fun stripMarkdown(markdown: String): String {
        if (markdown.isBlank()) return ""
        return markdown
            .lines()
            .map { line ->
                var l = line.trim()
                // Strip headers
                if (l.startsWith("### ")) l = l.removePrefix("### ")
                else if (l.startsWith("## ")) l = l.removePrefix("## ")
                else if (l.startsWith("# ")) l = l.removePrefix("# ")
                // Strip quotes
                if (l.startsWith("> ")) l = l.removePrefix("> ")
                // Strip checklists
                if (l.startsWith("- [ ] ")) l = l.removePrefix("- [ ] ")
                else if (l.startsWith("- [x] ") || l.startsWith("- [X] ")) l = l.removePrefix(l.take(6))
                // Strip bullet points
                if (l.startsWith("- ") || l.startsWith("* ") || l.startsWith("• ")) l = l.substring(2)
                // Strip numbered list
                l = l.replace(Regex("^\\d+\\.\\s+"), "")
                // Strip bold/italics
                l = l.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                l = l.replace(Regex("\\*(.*?)\\*"), "$1")
                l = l.replace(Regex("~~(.*?)~~"), "$1")
                l = l.replace(Regex("`(.*?)`"), "$1")
                // Strip markdown links [text](url) -> text
                l = l.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
                l.trim()
            }
            .filter { it.isNotBlank() && it != "---" && it != "***" }
            .joinToString(" ")
    }

    /**
     * Counts completed and total checklist items in a Markdown document.
     * Returns Pair(completed, total) or null if no checklist items exist.
     */
    fun countChecklistProgress(markdown: String): Pair<Int, Int>? {
        val lines = markdown.lines()
        var total = 0
        var completed = 0
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("- [ ] ")) {
                total++
            } else if (trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")) {
                total++
                completed++
            }
        }
        return if (total > 0) Pair(completed, total) else null
    }

    /**
     * Toggles a checklist item at the given line index between checked and unchecked.
     */
    fun toggleChecklistAt(markdown: String, targetLineIndex: Int): String {
        val lines = markdown.lines().toMutableList()
        if (targetLineIndex in lines.indices) {
            val line = lines[targetLineIndex]
            val indent = line.takeWhile { it.isWhitespace() }
            val trimmed = line.trim()
            val toggled = when {
                trimmed.startsWith("- [ ] ") -> indent + "- [x] " + trimmed.removePrefix("- [ ] ")
                trimmed.startsWith("- [x] ") -> indent + "- [ ] " + trimmed.removePrefix("- [x] ")
                trimmed.startsWith("- [X] ") -> indent + "- [ ] " + trimmed.removePrefix("- [X] ")
                else -> line
            }
            lines[targetLineIndex] = toggled
        }
        return lines.joinToString("\n")
    }

    /**
     * Parses markdown text into a structured list of blocks for rendering.
     */
    fun parseBlocks(markdown: String): List<MarkdownBlock> {
        val lines = markdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var inCodeBlock = false
        val codeBuffer = StringBuilder()
        var codeLang: String? = null

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd(), codeLang))
                    codeBuffer.clear()
                    codeLang = null
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLang = trimmed.removePrefix("```").trim().ifBlank { null }
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                return@forEachIndexed
            }

            when {
                trimmed.isEmpty() -> {
                    blocks.add(MarkdownBlock.EmptyLine)
                }
                trimmed == "---" || trimmed == "***" -> {
                    blocks.add(MarkdownBlock.Divider)
                }
                trimmed.startsWith("# ") -> {
                    blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
                }
                trimmed.startsWith("## ") -> {
                    blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
                }
                trimmed.startsWith("### ") -> {
                    blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
                }
                trimmed.startsWith("- [ ] ") -> {
                    blocks.add(MarkdownBlock.ChecklistItem(isChecked = false, text = trimmed.removePrefix("- [ ] ").trim(), lineIndex = index))
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") -> {
                    val prefix = if (trimmed.startsWith("- [x] ")) "- [x] " else "- [X] "
                    blocks.add(MarkdownBlock.ChecklistItem(isChecked = true, text = trimmed.removePrefix(prefix).trim(), lineIndex = index))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val text = when {
                        trimmed.startsWith("- ") -> trimmed.removePrefix("- ")
                        trimmed.startsWith("* ") -> trimmed.removePrefix("* ")
                        else -> trimmed.removePrefix("• ")
                    }.trim()
                    val indent = line.takeWhile { it == ' ' || it == '\t' }.length / 2
                    blocks.add(MarkdownBlock.BulletItem(text, indent))
                }
                Regex("^\\d+\\.\\s+").containsMatchIn(trimmed) -> {
                    val match = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
                    if (match != null) {
                        blocks.add(MarkdownBlock.NumberedItem(match.groupValues[1], match.groupValues[2]))
                    } else {
                        blocks.add(MarkdownBlock.Paragraph(line))
                    }
                }
                trimmed.startsWith("> ") -> {
                    blocks.add(MarkdownBlock.BlockQuote(trimmed.removePrefix("> ").trim()))
                }
                else -> {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            blocks.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd(), codeLang))
        }

        return blocks
    }

    /**
     * Renders inline markdown elements:
     * **bold**, *italic*, ~~strikethrough~~, `inline code`, [link title](url)
     */
    fun renderInlineMarkdown(
        text: String,
        isDark: Boolean,
        primaryColor: Color = Color(0xFF2563EB),
        onSurfaceColor: Color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
        codeBgColor: Color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    ): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                // Inline Code: `code`
                if (text[i] == '`') {
                    val nextBacktick = text.indexOf('`', i + 1)
                    if (nextBacktick != -1) {
                        val code = text.substring(i + 1, nextBacktick)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                fontSize = 13.sp
                            )
                        )
                        append(" $code ")
                        pop()
                        i = nextBacktick + 1
                        continue
                    }
                }

                // Bold & Italic: ***text***
                if (text.startsWith("***", i)) {
                    val end = text.indexOf("***", i + 3)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                        append(text.substring(i + 3, end))
                        pop()
                        i = end + 3
                        continue
                    }
                }

                // Bold: **text**
                if (text.startsWith("**", i)) {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = onSurfaceColor))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                        continue
                    }
                }

                // Strikethrough: ~~text~~
                if (text.startsWith("~~", i)) {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                        continue
                    }
                }

                // Italic: *text* (ensure not part of ** which was checked above)
                if (text[i] == '*' && (i + 1 < len && text[i + 1] != '*')) {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                        continue
                    }
                }

                // Markdown Link: [title](url)
                if (text[i] == '[') {
                    val closeBracket = text.indexOf(']', i + 1)
                    if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val linkTitle = text.substring(i + 1, closeBracket)
                            pushStyle(
                                SpanStyle(
                                    color = primaryColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            append(linkTitle)
                            pop()
                            i = closeParen + 1
                            continue
                        }
                    }
                }

                append(text[i])
                i++
            }
        }
    }
}
