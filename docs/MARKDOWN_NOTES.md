# Markdown notes

The note editor highlights Markdown while preserving the source text and cursor
offsets. Formatting shortcuts insert callouts, tables, highlights, formula
delimiters, and note references. Lists and quotes continue on Enter; Enter on an
empty item exits the structure. Pasting text and editing fenced code examples do
not trigger list continuation.

Preview supports headings, checklists, lists, quotes, typed callouts, aligned
tables, formula blocks, and code blocks with copy actions. Formula text is
displayed as source with styling; equations are not typeset by a LaTeX engine.
`[[Note Title]]` is a styled reference, without cross-note navigation.

The editor saves draft fields, text selection, and the Write/Preview tab across
activity recreation. The Notes screen restores the edited note by its key and
waits for local data before opening it. The active app tab also survives
recreation so returning to the editor is possible.

## Verification

`MarkdownNotesTest` exercises parsing, malformed table separators, adjacent
callouts, formula boundaries, checklist interaction, preview text, CRLF/emoji
offset preservation, selection wrapping, and continuation edge cases. The
existing Android CI also compiles both build types, assembles APKs, verifies
release signing, runs all unit tests, and validates the database asset.

Device checks remain necessary for typing with an IME, rotation while editing,
Write/Preview switching, large fonts, long tables/formulas, attachments, and
save/discard behavior. Screen layout and activity restoration have not been
visually verified locally.
