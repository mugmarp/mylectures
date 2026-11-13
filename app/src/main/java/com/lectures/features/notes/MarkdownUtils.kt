package com.lectures.features.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class CalloutType(val defaultTitle: String) {
    NOTE("Note"),
    TIP("Tip"),
    IMPORTANT("Important"),
    WARNING("Warning"),
    DANGER("Danger"),
    SUCCESS("Success"),
    INFO("Info"),
    QUESTION("Question"),
    EXAMPLE("Example"),
    QUOTE("Quote");

    companion object {
        fun fromString(str: String): CalloutType {
            val key = str.trim().uppercase(Locale.ROOT)
            return when {
                key in listOf("NOTE", "NOTES") -> NOTE
                key in listOf("TIP", "HINT") -> TIP
                key in listOf("IMPORTANT", "ATTENTION") -> IMPORTANT
                key in listOf("WARNING", "CAUTION") -> WARNING
                key in listOf("DANGER", "ERROR", "FAILURE", "FAIL") -> DANGER
                key in listOf("SUCCESS", "CHECK", "DONE") -> SUCCESS
                key in listOf("INFO", "INFORMATION") -> INFO
                key in listOf("QUESTION", "FAQ", "HELP") -> QUESTION
                key in listOf("EXAMPLE", "SAMPLE") -> EXAMPLE
                key in listOf("QUOTE", "CITE") -> QUOTE
                else -> NOTE
            }
        }
    }
}

enum class TableAlignment {
    LEFT, CENTER, RIGHT
}

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class ChecklistItem(val isChecked: Boolean, val text: String, val lineIndex: Int) : MarkdownBlock()
    data class BulletItem(val text: String, val indentLevel: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class Callout(val type: CalloutType, val title: String, val content: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val alignments: List<TableAlignment>, val rows: List<List<String>>) : MarkdownBlock()
    data class MathBlock(val formula: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    object EmptyLine : MarkdownBlock()
}

/**
 * Provides comprehensive Markdown parsing, inline rich text rendering,
 * and Live Preview visual transformation for Obsidian-compatible notes.
 */
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
                // Strip headers 1..6
                if (l.startsWith("###### ")) l = l.removePrefix("###### ")
                else if (l.startsWith("##### ")) l = l.removePrefix("##### ")
                else if (l.startsWith("#### ")) l = l.removePrefix("#### ")
                else if (l.startsWith("### ")) l = l.removePrefix("### ")
                else if (l.startsWith("## ")) l = l.removePrefix("## ")
                else if (l.startsWith("# ")) l = l.removePrefix("# ")

                // Strip Obsidian Callouts e.g. > [!NOTE] Title
                if (l.startsWith("> [!")) {
                    l = l.replace(Regex("^>\\s*\\[!\\w+\\]\\s*"), "")
                } else if (l.startsWith("> ")) {
                    l = l.removePrefix("> ")
                }

                // Strip checklists
                if (l.startsWith("- [ ] ")) l = l.removePrefix("- [ ] ")
                else if (l.startsWith("- [x] ") || l.startsWith("- [X] ")) l = l.removePrefix(l.take(6))

                // Strip bullet points
                if (l.startsWith("- ") || l.startsWith("* ") || l.startsWith("• ")) l = l.substring(2)

                // Strip numbered list
                l = l.replace(Regex("^\\d+\\.\\s+"), "")

                // Strip highlight ==text== -> text
                l = l.replace(Regex("==(.*?)=="), "$1")

                // Strip WikiLinks [[Note Name]] -> Note Name
                l = l.replace(Regex("\\[\\[(.*?)\\]\\]"), "$1")

                // Strip math $formula$ -> formula
                l = l.replace(Regex("\\$(.*?)\\$"), "$1")

                // Strip bold/italics
                l = l.replace(Regex("\\*\\*\\*(.*?)\\*\\*\\*"), "$1")
                l = l.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                l = l.replace(Regex("\\*(.*?)\\*"), "$1")
                l = l.replace(Regex("~~(.*?)~~"), "$1")
                l = l.replace(Regex("`(.*?)`"), "$1")

                // Strip markdown links [text](url) -> text
                l = l.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")

                // Clean table delimiters
                if (l.startsWith("|") && l.endsWith("|")) {
                    l = l.split("|").filter { it.isNotBlank() && !it.contains("---") }.joinToString(" · ") { it.trim() }
                }

                l.trim()
            }
            .filter { it.isNotBlank() && it != "---" && it != "***" && !it.startsWith("|---") }
            .joinToString(" ")
    }

    /**
     * Counts completed and total checklist items in a Markdown document.
     * Returns Pair(completed, total) or null if no checklist items exist.
     */
    fun countChecklistProgress(markdown: String): Pair<Int, Int>? {
        val items = parseBlocks(markdown).filterIsInstance<MarkdownBlock.ChecklistItem>()
        return if (items.isEmpty()) null else items.count { it.isChecked } to items.size
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
     * Parses markdown text into a structured list of blocks for rendering,
     * fully supporting Obsidian Callouts, Markdown Tables, Math Blocks, and H1..H6.
     */
    fun parseBlocks(markdown: String): List<MarkdownBlock> {
        val lines = markdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0
        val count = lines.size

        while (i < count) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Empty Line
            if (trimmed.isEmpty()) {
                blocks.add(MarkdownBlock.EmptyLine)
                i++
                continue
            }

            // 2. Horizontal Divider
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // 3. Code Block (```lang ... ```)
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim().ifBlank { null }
                val codeLines = mutableListOf<String>()
                i++
                while (i < count && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < count && lines[i].trim().startsWith("```")) {
                    i++ // Consume closing ```
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), lang))
                continue
            }

            // 4. Math Block ($$ ... $$)
            if (trimmed.startsWith("$$")) {
                val mathLines = mutableListOf<String>()
                val firstContent = trimmed.removePrefix("$$").trim()
                if (firstContent.endsWith("$$") && firstContent.length >= 2) {
                    blocks.add(MarkdownBlock.MathBlock(firstContent.removeSuffix("$$").trim()))
                    i++
                    continue
                }
                if (firstContent.isNotEmpty()) mathLines.add(firstContent)
                i++
                while (i < count && !lines[i].trim().endsWith("$$")) {
                    mathLines.add(lines[i])
                    i++
                }
                if (i < count) {
                    val lastContent = lines[i].trim().removeSuffix("$$").trim()
                    if (lastContent.isNotEmpty()) mathLines.add(lastContent)
                    i++
                }
                blocks.add(MarkdownBlock.MathBlock(mathLines.joinToString("\n")))
                continue
            }

            // 5. Obsidian Callout Block (> [!TYPE] Title)
            if (trimmed.startsWith("> [!")) {
                val headerLine = trimmed.removePrefix(">").trim()
                val match = Regex("^\\[!(\\w+)\\](.*)").find(headerLine)
                if (match != null) {
                    val typeStr = match.groupValues[1]
                    val customTitle = match.groupValues[2].trim()
                    val calloutType = CalloutType.fromString(typeStr)
                    val calloutTitle = customTitle.ifBlank { calloutType.defaultTitle }

                    val calloutContent = mutableListOf<String>()
                    i++
                    while (i < count && lines[i].trim().startsWith(">") && !lines[i].trim().startsWith("> [!")) {
                        val cLine = lines[i].trim().removePrefix(">").trimStart()
                        calloutContent.add(cLine)
                        i++
                    }
                    blocks.add(
                        MarkdownBlock.Callout(
                            type = calloutType,
                            title = calloutTitle,
                            content = calloutContent.joinToString("\n").trim()
                        )
                    )
                    continue
                }
            }

            // 6. Generic BlockQuote (> Quote)
            if (trimmed.startsWith("> ") || trimmed == ">") {
                val quoteLines = mutableListOf<String>()
                while (i < count && (lines[i].trim().startsWith(">") || lines[i].trim().isEmpty() && i + 1 < count && lines[i + 1].trim().startsWith(">"))) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MarkdownBlock.BlockQuote(quoteLines.filter { it.isNotBlank() }.joinToString(" ")))
                continue
            }

            // 7. Markdown Table (| Col 1 | Col 2 | \n | --- | --- |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < count) {
                val nextLine = lines[i + 1].trim()
                val headerRow = trimmed.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                val alignRow = nextLine.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                if (headerRow.size == alignRow.size && alignRow.all { Regex(":?-{3,}:?").matches(it) }) {
                    val alignments = alignRow.map { alignStr ->
                        when {
                            alignStr.startsWith(":") && alignStr.endsWith(":") -> TableAlignment.CENTER
                            alignStr.endsWith(":") -> TableAlignment.RIGHT
                            else -> TableAlignment.LEFT
                        }
                    }

                    val dataRows = mutableListOf<List<String>>()
                    i += 2 // Skip header and delimiter
                    while (i < count && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                        val dRow = lines[i].trim().split("|").map { it.trim() }.filterIndexed { idx, _ -> idx != 0 && idx != lines[i].trim().split("|").lastIndex }
                        dataRows.add(dRow)
                        i++
                    }
                    blocks.add(MarkdownBlock.Table(headerRow, alignments, dataRows))
                    continue
                }
            }

            // 8. Headers (H1 to H6)
            when {
                trimmed.startsWith("###### ") -> {
                    blocks.add(MarkdownBlock.Header(6, trimmed.removePrefix("###### ").trim()))
                    i++
                    continue
                }
                trimmed.startsWith("##### ") -> {
                    blocks.add(MarkdownBlock.Header(5, trimmed.removePrefix("##### ").trim()))
                    i++
                    continue
                }
                trimmed.startsWith("#### ") -> {
                    blocks.add(MarkdownBlock.Header(4, trimmed.removePrefix("#### ").trim()))
                    i++
                    continue
                }
                trimmed.startsWith("### ") -> {
                    blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
                    i++
                    continue
                }
                trimmed.startsWith("## ") -> {
                    blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
                    i++
                    continue
                }
                trimmed.startsWith("# ") -> {
                    blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
                    i++
                    continue
                }
            }

            // 9. Checklist item
            if (trimmed.startsWith("- [ ] ")) {
                blocks.add(MarkdownBlock.ChecklistItem(isChecked = false, text = trimmed.removePrefix("- [ ] ").trim(), lineIndex = i))
                i++
                continue
            }
            if (trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")) {
                val prefix = if (trimmed.startsWith("- [x] ")) "- [x] " else "- [X] "
                blocks.add(MarkdownBlock.ChecklistItem(isChecked = true, text = trimmed.removePrefix(prefix).trim(), lineIndex = i))
                i++
                continue
            }

            // 10. Bullet item
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                val text = when {
                    trimmed.startsWith("- ") -> trimmed.removePrefix("- ")
                    trimmed.startsWith("* ") -> trimmed.removePrefix("* ")
                    else -> trimmed.removePrefix("• ")
                }.trim()
                val indent = line.takeWhile { it == ' ' || it == '\t' }.length / 2
                blocks.add(MarkdownBlock.BulletItem(text, indent))
                i++
                continue
            }

            // 11. Numbered item
            if (Regex("^\\d+\\.\\s+").containsMatchIn(trimmed)) {
                val match = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
                if (match != null) {
                    blocks.add(MarkdownBlock.NumberedItem(match.groupValues[1], match.groupValues[2]))
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
                i++
                continue
            }

            // 12. Standard Paragraph
            blocks.add(MarkdownBlock.Paragraph(line))
            i++
        }

        return blocks
    }

    /**
     * Renders inline markdown elements:
     * - **bold**, *italic*, ***bold italic***
     * - ==highlight== (Obsidian syntax)
     * - [[WikiLink]] (Obsidian cross-note link)
     * - $math formula$
     * - ~~strikethrough~~
     * - `inline code`
     * - [link title](url)
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

            val highlightBg = if (isDark) Color(0xFF713F12) else Color(0xFFFEF08A)
            val highlightText = if (isDark) Color(0xFFFEF08A) else Color(0xFF713F12)

            while (i < len) {
                // 1. Obsidian Highlight: ==text==
                if (text.startsWith("==", i)) {
                    val end = text.indexOf("==", i + 2)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                background = highlightBg,
                                color = highlightText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(" " + text.substring(i + 2, end) + " ")
                        pop()
                        i = end + 2
                        continue
                    }
                }

                // 2. Obsidian WikiLink: [[Note Title]]
                if (text.startsWith("[[", i)) {
                    val end = text.indexOf("]]", i + 2)
                    if (end != -1) {
                        val linkText = text.substring(i + 2, end)
                        pushStyle(
                            SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                        append(linkText)
                        pop()
                        i = end + 2
                        continue
                    }
                }

                // 3. Inline Math Formula: $formula$
                if (text[i] == '$' && (i + 1 < len && text[i + 1] != '$')) {
                    val end = text.indexOf('$', i + 1)
                    if (end != -1) {
                        val formula = text.substring(i + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily.Serif,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                background = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        append(" $formula ")
                        pop()
                        i = end + 1
                        continue
                    }
                }

                // 4. Inline Code: `code`
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

                // 5. Bold & Italic: ***text***
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

                // 6. Bold: **text**
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

                // 7. Strikethrough: ~~text~~
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

                // 8. Italic: *text*
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

                // 9. Markdown Link: [title](url)
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

    /**
     * Highlights Markdown syntax in real time inside the editor (Obsidian Live Preview effect).
     * Maintains a strict 1-to-1 character offset mapping so cursor positioning never drifts.
     */
    fun highlightMarkdownSyntax(
        rawText: String,
        isDark: Boolean,
        primaryColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            append(rawText)

            var currentOffset = 0

            val headingColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
            val calloutColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            val tagColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            val dimSyntaxColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
            val highlightBg = if (isDark) Color(0xFF713F12) else Color(0xFFFEF08A)

            while (currentOffset <= rawText.length) {
                val nextNewline = rawText.indexOf('\n', currentOffset)
                val lineRawEnd = if (nextNewline == -1) rawText.length else nextNewline
                val lineEnd = if (lineRawEnd > currentOffset && rawText[lineRawEnd - 1] == '\r') {
                    lineRawEnd - 1
                } else {
                    lineRawEnd
                }
                val line = rawText.substring(currentOffset, lineEnd)
                val lineLen = line.length
                val trimmed = line.trimStart()
                val lineStart = currentOffset + (lineLen - trimmed.length)

                // 1. Headings (# ... ######)
                if (trimmed.startsWith("#")) {
                    val hashCount = trimmed.takeWhile { it == '#' }.length
                    if (hashCount in 1..6 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                        val fontSize = when (hashCount) {
                            1 -> 18.sp
                            2 -> 16.sp
                            else -> 15.sp
                        }
                        addStyle(
                            SpanStyle(
                                color = headingColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSize
                            ),
                            lineStart,
                            lineEnd
                        )
                        // Dim the leading hash symbols
                        addStyle(
                            SpanStyle(color = dimSyntaxColor, fontWeight = FontWeight.Normal),
                            lineStart,
                            lineStart + hashCount + 1
                        )
                    }
                }

                // 2. Obsidian Callouts (> [!NOTE])
                else if (trimmed.startsWith("> [!")) {
                    addStyle(
                        SpanStyle(
                            color = calloutColor,
                            fontWeight = FontWeight.Bold
                        ),
                        lineStart,
                        lineEnd
                    )
                }

                // 3. Quotes (> )
                else if (trimmed.startsWith("> ")) {
                    addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic, color = dimSyntaxColor),
                        lineStart,
                        lineEnd
                    )
                }

                // 4. Checklist (- [ ] or - [x])
                else if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")) {
                    val isChecked = !trimmed.startsWith("- [ ] ")
                    val checkColor = if (isChecked) (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)) else primaryColor
                    addStyle(
                        SpanStyle(color = checkColor, fontWeight = FontWeight.Bold),
                        lineStart,
                        lineStart + 6
                    )
                    if (isChecked) {
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough, color = dimSyntaxColor),
                            lineStart + 6,
                            lineEnd
                        )
                    }
                }

                // 5. Bullets & Numbers
                else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    addStyle(
                        SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                        lineStart,
                        lineStart + 2
                    )
                }

                if (nextNewline == -1) break
                currentOffset = nextNewline + 1
            }

            // Inline syntax highlighters:
            // ==highlight==
            Regex("==([^=]+)==").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(background = highlightBg, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
            }

            // **bold**
            Regex("\\*\\*([^*]+)\\*\\*").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }

            // *italic*
            Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }

            // `code`
            Regex("`([^`]+)`").findAll(rawText).forEach { match ->
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                        background = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // [[WikiLink]]
            Regex("\\[\\[([^\\]]+)\\]\\]").findAll(rawText).forEach { match ->
                addStyle(
                    SpanStyle(
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // $math$
            Regex("\\$([^\\$]+)\\$").findAll(rawText).forEach { match ->
                addStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // #tag
            Regex("#[a-zA-Z0-9_]+").findAll(rawText).forEach { match ->
                addStyle(
                    SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}

/**
 * VisualTransformation that provides live syntax styling for the Markdown editor
 * without altering cursor offsets or raw characters.
 */
class MarkdownLiveVisualTransformation(
    private val isDark: Boolean,
    private val primaryColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = MarkdownUtils.highlightMarkdownSyntax(text.text, isDark, primaryColor)
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
