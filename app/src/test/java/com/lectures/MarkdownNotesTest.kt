package com.lectures

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lectures.features.notes.CalloutType
import com.lectures.features.notes.MarkdownBlock
import com.lectures.features.notes.MarkdownLiveVisualTransformation
import com.lectures.features.notes.MarkdownUtils
import com.lectures.features.notes.NoteTemplatesRepository
import com.lectures.features.notes.TableAlignment
import com.lectures.features.notes.continueMarkdownLine
import com.lectures.features.notes.wrapMarkdownSelection
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class MarkdownNotesTest {
    @Test
    fun formattingHandlesReversedSelectionsAndSelectsInsertedPlaceholders() {
        val selected = TextFieldValue("Read cells today", TextRange(10, 5))
        val wrapped = wrapMarkdownSelection(selected, "==", "==", "term")
        assertEquals("Read ==cells== today", wrapped.text)
        assertEquals(TextRange(14), wrapped.selection)
        val inserted = wrapMarkdownSelection(TextFieldValue("", TextRange(0)), "[[", "]]", "Lecture")
        assertEquals("[[Lecture]]", inserted.text)
        assertEquals(TextRange(2, 9), inserted.selection)
    }

    @Test
    fun parsesCalloutsWithBodiesAndKeepsAdjacentCalloutsSeparate() {
        val blocks = MarkdownUtils.parseBlocks(
            "> [!HINT] Revise\n> ==Focus here==\n> [!WARNING]\n> Check units"
        ).filterIsInstance<MarkdownBlock.Callout>()
        assertEquals(2, blocks.size)
        assertEquals(MarkdownBlock.Callout(CalloutType.TIP, "Revise", "==Focus here=="), blocks[0])
        assertEquals(MarkdownBlock.Callout(CalloutType.WARNING, "Warning", "Check units"), blocks[1])
    }

    @Test
    fun calloutAliasesDoNotDependOnDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(CalloutType.INFO, CalloutType.fromString("info"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun tablesPreserveAlignmentAndRejectInvalidSeparatorRows() {
        val blocks = MarkdownUtils.parseBlocks("| Topic | Mark |\n| :--- | ---: |\n| Cells | 10 |")
        val table = blocks.single() as MarkdownBlock.Table
        assertEquals(listOf("Topic", "Mark"), table.headers)
        assertEquals(listOf(TableAlignment.LEFT, TableAlignment.RIGHT), table.alignments)
        assertEquals(listOf(listOf("Cells", "10")), table.rows)
        assertTrue(MarkdownUtils.parseBlocks("| A | B |\n| --- | prose |")
            .none { it is MarkdownBlock.Table })
    }

    @Test
    fun formulaBlocksDoNotConsumeFollowingParagraphs() {
        val blocks = MarkdownUtils.parseBlocks("\$\$\$\$\nAfter\n\$\$\nx + y\n\$\$\nEnd")
        assertEquals(MarkdownBlock.MathBlock(""), blocks[0])
        assertEquals(MarkdownBlock.Paragraph("After"), blocks[1])
        assertEquals(MarkdownBlock.MathBlock("x + y"), blocks[2])
        assertEquals(MarkdownBlock.Paragraph("End"), blocks[3])
        assertEquals(MarkdownBlock.MathBlock("E = mc^2"),
            MarkdownUtils.parseBlocks("\$\$E = mc^2\$\$").single())
    }

    @Test
    fun codeExamplesDoNotBecomeInteractiveTasksOrChangeChecklistProgress() {
        val note = "```md\n- [x] Example\n> [!NOTE] Literal\n```\n\n- [ ] Real task"
        val blocks = MarkdownUtils.parseBlocks(note)
        assertTrue(blocks.first() is MarkdownBlock.CodeBlock)
        assertEquals(0 to 1, MarkdownUtils.countChecklistProgress(note))
        val task = blocks.filterIsInstance<MarkdownBlock.ChecklistItem>().single()
        val toggled = MarkdownUtils.toggleChecklistAt(note, task.lineIndex)
        assertTrue(toggled.startsWith("```md\n- [x] Example"))
        assertTrue(toggled.endsWith("- [x] Real task"))
        assertEquals(1 to 1, MarkdownUtils.countChecklistProgress(toggled))
    }

    @Test
    fun syntaxHighlightingKeepsCursorOffsetsForEmojiAndCrlf() {
        val raw = "# 🧪 Cells\r\n- [x] Finished\r\n> [!NOTE] Remember\r\n==Exam=="
        val transformed = MarkdownLiveVisualTransformation(false, Color.Blue)
            .filter(AnnotatedString(raw))
        assertEquals(raw, transformed.text.text)
        for (offset in 0..raw.length) {
            assertEquals(offset, transformed.offsetMapping.originalToTransformed(offset))
            assertEquals(offset, transformed.offsetMapping.transformedToOriginal(offset))
        }
        assertTrue(transformed.text.spanStyles.all { it.start >= 0 && it.end <= raw.length })
        assertTrue(transformed.text.spanStyles.any { raw.substring(it.start, it.end) == "Finished" })
    }

    @Test
    fun notePreviewsRemoveNewMarkupWithoutDroppingContent() {
        val preview = MarkdownUtils.stripMarkdown(
            "> [!NOTE] Revision\n> ==Term== and [[Lecture One]] with \$x + y\$"
        )
        assertEquals("Revision Term and Lecture One with x + y", preview)
    }

    @Test
    fun listsContinueWithIndentationAndUncheckedTasks() {
        assertEquals("  - [x] Read\n  - [ ] ", enter("  - [x] Read").text)
        assertEquals("  * Cells\n  * ", enter("  * Cells").text)
        assertEquals("9. Topic\n10. ", enter("9. Topic").text)
        assertEquals("> [!NOTE] Reminder\n> ", enter("> [!NOTE] Reminder").text)
    }

    @Test
    fun enterOnEmptyItemsExitsTheListOrQuote() {
        for (prefix in listOf("- [ ] ", "  - ", "3. ", "> ")) {
            val result = enter("Intro\n$prefix")
            assertEquals("Intro\n\n", result.text)
            assertEquals(result.text.length, result.selection.start)
        }
    }

    @Test
    fun enterInMiddleOfLinePreservesTrailingTextAndCursor() {
        val result = enter("- first second", 7)
        assertEquals("- first\n-  second", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun pasteSelectionsCompositionAndCodeExamplesStayUnmodified() {
        val old = TextFieldValue("- item", TextRange(6))
        val pasted = TextFieldValue("- item\nPasted", TextRange(13))
        assertEquals(pasted, continueMarkdownLine(old, pasted))
        val selected = old.copy(selection = TextRange(2, 6))
        val replacement = TextFieldValue("- \n", TextRange(3))
        assertEquals(replacement, continueMarkdownLine(selected, replacement))
        val composing = TextFieldValue("- item\n", TextRange(7), TextRange(0, 7))
        assertEquals(composing, continueMarkdownLine(old, composing))
        assertEquals("```md\n- item\n", enter("```md\n- item").text)
        assertEquals("9223372036854775807. item\n", enter("9223372036854775807. item").text)
    }

    @Test
    fun lectureTemplateHasRenderableCalloutBodiesAndTable() {
        val blocks = NoteTemplatesRepository.templates.flatMap { MarkdownUtils.parseBlocks(it.content) }
        assertTrue(blocks.any { it is MarkdownBlock.Table })
        assertTrue(blocks.filterIsInstance<MarkdownBlock.Callout>().all { it.content.isNotBlank() })
    }

    private fun enter(text: String, at: Int = text.length): TextFieldValue {
        val previous = TextFieldValue(text, TextRange(at))
        val next = TextFieldValue(text.substring(0, at) + "\n" + text.substring(at), TextRange(at + 1))
        return continueMarkdownLine(previous, next)
    }
}
