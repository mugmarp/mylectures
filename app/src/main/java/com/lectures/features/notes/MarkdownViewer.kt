package com.lectures.features.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.LocalAppTheme

@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier,
    onToggleChecklist: ((lineIndex: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalAppTheme.current.isDark
    val blocks = remember(markdown) { MarkdownUtils.parseBlocks(markdown) }

    if (markdown.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "This note is empty. Switch to Write mode or choose a template to add content.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                // 1. Headers (H1 to H6)
                is MarkdownBlock.Header -> {
                    val (fontSize, fontWeight, color, topPad) = when (block.level) {
                        1 -> Quadruple(22.sp, FontWeight.ExtraBold, MaterialTheme.colorScheme.primary, 12.dp)
                        2 -> Quadruple(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface, 10.dp)
                        3 -> Quadruple(16.sp, FontWeight.SemiBold, MaterialTheme.colorScheme.onSurface, 8.dp)
                        4 -> Quadruple(15.sp, FontWeight.SemiBold, MaterialTheme.colorScheme.onSurfaceVariant, 6.dp)
                        5 -> Quadruple(14.sp, FontWeight.Medium, MaterialTheme.colorScheme.onSurfaceVariant, 4.dp)
                        else -> Quadruple(13.sp, FontWeight.Medium, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 4.dp)
                    }

                    Spacer(modifier = Modifier.height(topPad))
                    Text(
                        text = block.text,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        lineHeight = (fontSize.value * 1.3).sp
                    )
                    if (block.level == 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                // 2. Obsidian Callouts (> [!NOTE], > [!WARNING], etc.)
                is MarkdownBlock.Callout -> {
                    val (calloutColor, calloutBg, calloutIcon) = when (block.type) {
                        CalloutType.NOTE -> Triple(Color(0xFF2563EB), if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF), Icons.Outlined.Info)
                        CalloutType.TIP -> Triple(Color(0xFF0D9488), if (isDark) Color(0xFF134E4A).copy(alpha = 0.4f) else Color(0xFFF0FDFA), Icons.Outlined.Lightbulb)
                        CalloutType.IMPORTANT -> Triple(Color(0xFF7C3AED), if (isDark) Color(0xFF4C1D95).copy(alpha = 0.4f) else Color(0xFFFAF5FF), Icons.Outlined.PriorityHigh)
                        CalloutType.WARNING -> Triple(Color(0xFFD97706), if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFFFBEB), Icons.Outlined.WarningAmber)
                        CalloutType.DANGER -> Triple(Color(0xFFDC2626), if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEF2F2), Icons.Outlined.ErrorOutline)
                        CalloutType.SUCCESS -> Triple(Color(0xFF16A34A), if (isDark) Color(0xFF14532D).copy(alpha = 0.4f) else Color(0xFFF0FDF4), Icons.Outlined.CheckCircle)
                        CalloutType.INFO -> Triple(Color(0xFF0284C7), if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.4f) else Color(0xFFF0F9FF), Icons.Outlined.Info)
                        CalloutType.QUESTION -> Triple(Color(0xFFEAB308), if (isDark) Color(0xFF713F12).copy(alpha = 0.4f) else Color(0xFFFEFCE8), Icons.Outlined.HelpOutline)
                        CalloutType.EXAMPLE -> Triple(Color(0xFF4F46E5), if (isDark) Color(0xFF312E81).copy(alpha = 0.4f) else Color(0xFFEEF2FF), Icons.Outlined.Science)
                        CalloutType.QUOTE -> Triple(Color(0xFF64748B), if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC), Icons.Default.FormatQuote)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = calloutBg),
                        border = BorderStroke(1.5.dp, calloutColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = calloutIcon,
                                    contentDescription = null,
                                    tint = calloutColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = block.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = calloutColor
                                )
                            }
                            if (block.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = MarkdownUtils.renderInlineMarkdown(block.content, isDark, calloutColor),
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }
                }

                // 3. Markdown Table (| Col 1 | Col 2 |)
                is MarkdownBlock.Table -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                block.headers.forEachIndexed { colIndex, header ->
                                    val alignment = block.alignments.getOrElse(colIndex) { TableAlignment.LEFT }
                                    Box(
                                        modifier = Modifier
                                            .width(180.dp)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = when (alignment) {
                                            TableAlignment.CENTER -> Alignment.Center
                                            TableAlignment.RIGHT -> Alignment.CenterEnd
                                            else -> Alignment.CenterStart
                                        }
                                    ) {
                                        Text(
                                            text = MarkdownUtils.renderInlineMarkdown(header, isDark, MaterialTheme.colorScheme.primary),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = when (alignment) {
                                                TableAlignment.CENTER -> TextAlign.Center
                                                TableAlignment.RIGHT -> TextAlign.End
                                                else -> TextAlign.Start
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), thickness = 1.dp)

                            // Data Rows
                            block.rows.forEachIndexed { rowIndex, row ->
                                val rowBg = if (rowIndex % 2 == 1) {
                                    if (isDark) Color(0xFF0F172A).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                                } else Color.Transparent

                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    row.forEachIndexed { colIndex, cell ->
                                        val alignment = block.alignments.getOrElse(colIndex) { TableAlignment.LEFT }
                                        Box(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = when (alignment) {
                                                TableAlignment.CENTER -> Alignment.Center
                                                TableAlignment.RIGHT -> Alignment.CenterEnd
                                                else -> Alignment.CenterStart
                                            }
                                        ) {
                                            Text(
                                                text = MarkdownUtils.renderInlineMarkdown(cell, isDark, MaterialTheme.colorScheme.primary),
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp,
                                                textAlign = when (alignment) {
                                                    TableAlignment.CENTER -> TextAlign.Center
                                                    TableAlignment.RIGHT -> TextAlign.End
                                                    else -> TextAlign.Start
                                                }
                                            )
                                        }
                                    }
                                }
                                if (rowIndex < block.rows.lastIndex) {
                                    HorizontalDivider(color = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                // 4. Math Block ($$ formula $$)
                is MarkdownBlock.MathBlock -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FORMULA / EQUATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                    letterSpacing = 0.5.sp
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Formula", block.formula))
                                        Toast.makeText(context, "Formula copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy formula",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = block.formula,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.sp,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF),
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                // 5. Checklist Item
                is MarkdownBlock.ChecklistItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = block.isChecked,
                            onCheckedChange = {
                                onToggleChecklist?.invoke(block.lineIndex)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = MarkdownUtils.renderInlineMarkdown(block.text, isDark, MaterialTheme.colorScheme.primary),
                            fontSize = 14.sp,
                            color = if (block.isChecked) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (block.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 6. Bullet Item
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indentLevel * 16).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = MarkdownUtils.renderInlineMarkdown(block.text, isDark, MaterialTheme.colorScheme.primary),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7. Numbered Item
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = block.number,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = MarkdownUtils.renderInlineMarkdown(block.text, isDark, MaterialTheme.colorScheme.primary),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 8. BlockQuote
                is MarkdownBlock.BlockQuote -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = MarkdownUtils.renderInlineMarkdown(block.text, isDark, MaterialTheme.colorScheme.primary),
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // 9. Code Block with 1-Tap Copy & Language Badge
                is MarkdownBlock.CodeBlock -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFF1E293B)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (block.language ?: "CODE").uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", block.code))
                                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = block.code,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFF1F5F9),
                                lineHeight = 18.sp,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                // 10. Divider
                MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // 11. Paragraph
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = MarkdownUtils.renderInlineMarkdown(block.text, isDark, MaterialTheme.colorScheme.primary),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                // 12. Empty Line
                MarkdownBlock.EmptyLine -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
