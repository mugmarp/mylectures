package com.lectures.features.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MarkdownCheatItem(
    val title: String,
    val visualResult: String,
    val syntaxToType: String,
    val toolbarButton: String,
    val snippetToInsert: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownHelpBottomSheet(
    onDismiss: () -> Unit,
    onInsertSnippet: (String) -> Unit
) {
    val items = listOf(
        MarkdownCheatItem(
            title = "Checklist / To-Dos",
            visualResult = "☑ Study Chapter 4",
            syntaxToType = "- [ ] Study Chapter 4",
            toolbarButton = "Tap ☑ in toolbar",
            snippetToInsert = "- [ ] "
        ),
        MarkdownCheatItem(
            title = "Bold Emphasis",
            visualResult = "Important Term",
            syntaxToType = "**Important Term**",
            toolbarButton = "Select text and tap B",
            snippetToInsert = "**Important Term**"
        ),
        MarkdownCheatItem(
            title = "Italicized Notes",
            visualResult = "Theoretical definition",
            syntaxToType = "*Theoretical definition*",
            toolbarButton = "Select text and tap I",
            snippetToInsert = "*Theoretical definition*"
        ),
        MarkdownCheatItem(
            title = "Main Heading",
            visualResult = "Lecture Title (Large)",
            syntaxToType = "# Topic Title",
            toolbarButton = "Tap H1 in toolbar",
            snippetToInsert = "# Topic Title\n"
        ),
        MarkdownCheatItem(
            title = "Subheading",
            visualResult = "Section 1: Details (Medium)",
            syntaxToType = "## Section Title",
            toolbarButton = "Tap H2 in toolbar",
            snippetToInsert = "## Section Title\n"
        ),
        MarkdownCheatItem(
            title = "Key Quote / Callout",
            visualResult = "Lecturer tip: Tested on midterm",
            syntaxToType = "> Lecturer tip: Tested on midterm",
            toolbarButton = "Tap ” in toolbar",
            snippetToInsert = "> Important takeaway or lecturer quote\n"
        ),
        MarkdownCheatItem(
            title = "Bullet Points",
            visualResult = "• Core principle A\n• Core principle B",
            syntaxToType = "- Core principle A\n- Core principle B",
            toolbarButton = "Tap • in toolbar",
            snippetToInsert = "- First point\n- Second point\n"
        ),
        MarkdownCheatItem(
            title = "Numbered Step",
            visualResult = "1. Setup configuration\n2. Run calculation",
            syntaxToType = "1. Setup configuration\n2. Run calculation",
            toolbarButton = "Tap 1. in toolbar",
            snippetToInsert = "1. First step\n2. Second step\n"
        ),
        MarkdownCheatItem(
            title = "Callout",
            visualResult = "📘 Note: Key Takeaway",
            syntaxToType = "> [!NOTE] Title\n> Description text",
            toolbarButton = "Tap [!Note] in toolbar",
            snippetToInsert = "> [!NOTE] Key Takeaway\n> Core concept explained here\n"
        ),
        MarkdownCheatItem(
            title = "Markdown Table",
            visualResult = "Tabular data grid with columns",
            syntaxToType = "| Col 1 | Col 2 |\n| --- | --- |\n| Val 1 | Val 2 |",
            toolbarButton = "Tap Table in toolbar",
            snippetToInsert = "\n| Topic | Key Concept | Status |\n| :--- | :--- | :---: |\n| Point 1 | Definition | Ready |\n| Point 2 | Example | Review |\n"
        ),
        MarkdownCheatItem(
            title = "Highlight Text",
            visualResult = "==Highlighted exam term==",
            syntaxToType = "==Exam question==",
            toolbarButton = "Tap == in toolbar",
            snippetToInsert = "==Important exam topic=="
        ),
        MarkdownCheatItem(
            title = "Formula Block",
            visualResult = "Formula source in a styled block",
            syntaxToType = "\$\$formula\$\$ or \$inline\$",
            toolbarButton = "Tap \$\$ Math in toolbar",
            snippetToInsert = "\$\$E = mc^2\$\$\n"
        ),
        MarkdownCheatItem(
            title = "Note Reference",
            visualResult = "Styled note reference (display only)",
            syntaxToType = "[[Course Note Name]]",
            toolbarButton = "Tap [[Link]] in toolbar",
            snippetToInsert = "[[Course Note Title]]"
        ),
        MarkdownCheatItem(
            title = "Code / Formula Snippet",
            visualResult = "speed = distance / time",
            syntaxToType = "`speed = distance / time`",
            toolbarButton = "Tap ` in toolbar",
            snippetToInsert = "`formula_or_code`"
        ),
        MarkdownCheatItem(
            title = "Divider Line",
            visualResult = "───────────────",
            syntaxToType = "---",
            toolbarButton = "Tap — in toolbar",
            snippetToInsert = "\n---\n"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notes Formatting Guide",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Use the visual toolbar buttons or type quick markdown syntax",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedButton(
                                    onClick = {
                                        onInsertSnippet(item.snippetToInsert)
                                        onDismiss()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Insert", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "How to type it:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = item.syntaxToType,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Toolbar shortcut:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = item.toolbarButton,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
