package com.lectures.features.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Wraps either selection direction, or selects a placeholder at the caret. */
internal fun wrapMarkdownSelection(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val selected = if (start == end) placeholder else value.text.substring(start, end)
    val text = value.text.substring(0, start) + prefix + selected + suffix + value.text.substring(end)
    val selection = if (start == end) {
        TextRange(start + prefix.length, start + prefix.length + selected.length)
    } else {
        TextRange(end + prefix.length + suffix.length)
    }
    return TextFieldValue(text, selection)
}

/** Continues a list or quote only when the user inserts a single newline. */
internal fun continueMarkdownLine(previous: TextFieldValue, next: TextFieldValue): TextFieldValue {
    if (!previous.selection.collapsed || !next.selection.collapsed || next.composition != null) return next
    val insertion = previous.selection.start
    if (next.selection.start != insertion + 1 ||
        next.text != previous.text.substring(0, insertion) + "\n" + previous.text.substring(insertion)
    ) return next

    val lineStart = previous.text.lastIndexOf('\n', insertion - 1) + 1
    val openCodeFence = previous.text.substring(0, lineStart).lineSequence()
        .count { it.trimStart().startsWith("```") } % 2 != 0
    if (openCodeFence) return next
    val line = previous.text.substring(lineStart, insertion)
    val indent = line.takeWhile { it == ' ' || it == '\t' }
    val body = line.removePrefix(indent)
    val checklist = Regex("^- \\[[ xX]\\](?:[ \\t]+(.*))?$").matchEntire(body)
    val bullet = Regex("^([-*•])(?:[ \\t]+(.*))?$").matchEntire(body)
    val numbered = Regex("^(\\d+)\\.(?:[ \\t]+(.*))?$").matchEntire(body)
    val quote = Regex("^>(?:[ \\t]+(.*))?$").matchEntire(body)

    val empty = when {
        checklist != null -> checklist.groupValues[1].isBlank()
        bullet != null -> bullet.groupValues[2].isBlank()
        numbered != null -> numbered.groupValues[2].isBlank()
        quote != null -> quote.groupValues[1].isBlank()
        else -> false
    }
    if (empty) {
        val text = next.text.replaceRange(lineStart, insertion, "")
        return TextFieldValue(text, TextRange(lineStart + 1))
    }

    val prefix = when {
        checklist != null -> "- [ ] "
        bullet != null -> "${bullet.groupValues[1]} "
        numbered != null -> {
            val number = numbered.groupValues[1].toLongOrNull() ?: return next
            if (number == Long.MAX_VALUE) return next
            "${number + 1}. "
        }
        quote != null -> "> "
        else -> return next
    }
    val continuation = indent + prefix
    val cursor = next.selection.start
    return TextFieldValue(
        next.text.substring(0, cursor) + continuation + next.text.substring(cursor),
        TextRange(cursor + continuation.length)
    )
}
